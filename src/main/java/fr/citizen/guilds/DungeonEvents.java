package fr.citizen.guilds;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CitizenGuilds.MOD_ID)
public final class DungeonEvents {
    private static final String MOB_TAG = "citizenguilds_dungeon_mob";
    private static int tickCounter;
    private static int trapCounter;
    private static int autoSpawnSeconds;
    private DungeonEvents() {}

    @SubscribeEvent
    public static void protectDungeonBlocks(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(CitizenDimensions.DUNGEON)) return;
        if (!(event.getPlayer() instanceof ServerPlayer player) || !player.hasPermissions(2)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void protectDungeonPlacement(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(CitizenDimensions.DUNGEON)) return;
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.hasPermissions(2)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DungeonSessionData session = DungeonSessionData.get(player.getServer());
        int pendingJade = session.claimPendingReward(player.getUUID().toString());
        if (pendingJade > 0) {
            ItemStack reward = new ItemStack(CitizenJadeItems.RAW_JADE.get(), pendingJade);
            if (!player.getInventory().add(reward)) player.drop(reward, false);
            player.sendSystemMessage(Component.literal("§a[Donjon] Recompense de reconnexion : " + pendingJade + " minerai(s) de Pierre de Jade."));
        }
        if (!player.serverLevel().dimension().equals(CitizenDimensions.DUNGEON)) return;
        if (session.active() && session.isParticipant(player.getUUID().toString()) && !session.isEliminated(player.getUUID().toString())) return;
        var overworld = player.getServer().overworld();
        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("§e[Donjon] La session n'est plus accessible. Tu as ete renvoye au spawn."));
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().getTags().contains(MOB_TAG) && event.getEntity().level() instanceof ServerLevel mobLevel) {
            DungeonSessionData.get(mobLevel.getServer()).mobDefeated(event.getEntity().getUUID());
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) return;
        DungeonSessionData session = DungeonSessionData.get(player.getServer());
        if (!session.eliminate(player)) return;
        GuildSavedData.get(player.getServer()).recordDungeonDeath(session.guildId(), player.getGameProfile().getName());
        player.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("§c[Donjon] §f" + player.getGameProfile().getName()
                + " est elimine de la session de " + session.guildName() + " (une seule vie)."), false);
        if (session.aliveCount() == 0) {
            ServerLevel dungeon = player.getServer().getLevel(CitizenDimensions.DUNGEON);
            if (dungeon != null) session.removeActiveMobs(dungeon);
            GuildSavedData.get(player.getServer()).recordDungeonFailure(session.guildId());
            session.finish(false);
            player.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§c[Donjon] Toute l'equipe a ete eliminee. La session est un echec."), false);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < 20) return;
        tickCounter = 0;
        MinecraftServer server = event.getServer();
        DungeonSessionData session = DungeonSessionData.get(server);
        if (!session.active()) {
            tryAutoSpawnPortal(server);
            return;
        }
        if (session.finished() || session.site() == null || session.participantCount() == 0) return;
        ServerLevel level = server.getLevel(CitizenDimensions.DUNGEON);
        if (level == null) return;
        if (session.aliveCount() == 0) {
            session.removeActiveMobs(level);
            GuildSavedData.get(server).recordDungeonFailure(session.guildId());
            session.finish(false);
            server.getPlayerList().broadcastSystemMessage(Component.literal("§c[Donjon] Tous les aventuriers ont ete elimines."), false);
            return;
        }
        boolean atLeastOneOnline = false;
        for (String uuid : session.livingParticipantIds()) {
            ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuid));
            if (player == null || player.serverLevel() != level) continue;
            atLeastOneOnline = true;
            if (!DungeonArena.isInsideBounds(player.blockPosition())) {
                BlockPos entrance = DungeonArena.entrance();
                player.teleportTo(level, entrance.getX() + 0.5D, entrance.getY(), entrance.getZ() + 0.5D,
                    player.getYRot(), player.getXRot());
                player.sendSystemMessage(Component.literal("§e[Donjon] Tu as ete replace dans les limites de l'instance."));
            }
        }
        if (!atLeastOneOnline) return;

        if (session.wavePending()) {
            if (session.wave() == 0) session.nextWave();
            List<String> spawned = spawnWave(level, DungeonArena.waveCenter(session.wave()), session.wave());
            session.setWaveMobs(spawned);
            server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6[Donjon] §fVague " + session.wave() + "/3 : " + spawned.size() + " adversaire(s) apparaissent."), false);
            if (spawned.isEmpty()) {
                GuildSavedData.get(server).recordDungeonFailure(session.guildId());
                session.finish(false);
                server.getPlayerList().broadcastSystemMessage(Component.literal("§c[Donjon] Impossible de faire apparaitre les creatures. Session arretee."), false);
            }
            return;
        }

        int remaining = session.pruneMobIds(level);
        if (session.wave() == 2) applyTrapPulse(server, level, session);
        else trapCounter = 0;
        if (remaining > 0) return;
        if (session.wave() < 3) {
            session.nextWave();
            server.getPlayerList().broadcastSystemMessage(
                Component.literal("§e[Donjon] Vague terminee. La prochaine approche..."), false);
            return;
        }
        completeVictory(server, session);
    }

    private static void tryAutoSpawnPortal(MinecraftServer server) {
        DungeonData data = DungeonData.get(server);
        if (!data.autoSpawnEnabled()) { autoSpawnSeconds = 0; return; }
        ServerLevel overworld = server.overworld();
        BlockPos previous = data.lastSite();
        if (previous != null) {
            long spawnedAt = data.portalSpawnGameTime();
            boolean expired = spawnedAt > 0L && overworld.getGameTime() - spawnedAt >= 20L * 60L * 60L * 2L;
            if (expired) {
                overworld.getChunkAt(previous);
                if (overworld.getBlockState(previous).is(CitizenDungeonBlocks.DUNGEON_GATE.get()))
                    overworld.removeBlock(previous, false);
                data.clearLastSite();
                server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§7[Donjon] Une porte ancienne s'est refermee apres deux heures d'attente."), false);
                autoSpawnSeconds = 0;
                return;
            }
            if (!overworld.hasChunkAt(previous)) { autoSpawnSeconds = 0; return; }
            if (overworld.getBlockState(previous).is(CitizenDungeonBlocks.DUNGEON_GATE.get())) {
                autoSpawnSeconds = 0;
                return;
            }
            data.clearLastSite();
        }
        int interval = data.spawnIntervalMinutes() * 60;
        if (++autoSpawnSeconds < interval) return;

        ServerPlayer referencePlayer = overworld.players().stream().findFirst().orElse(null);
        if (referencePlayer == null) return;
        BlockPos site = DungeonSiteFinder.find(overworld, referencePlayer, data.minX(), data.minY(), data.minZ());
        if (site == null) {
            autoSpawnSeconds = Math.max(0, interval - 300);
            return;
        }
        if (!overworld.getBlockState(site).isAir() || !overworld.getBlockState(site.above()).isAir()) return;
        data.setLastSite(site);
        data.setPortalSpawnGameTime(overworld.getGameTime());
        overworld.setBlock(site, CitizenDungeonBlocks.DUNGEON_GATE.get().defaultBlockState(), 3);
        autoSpawnSeconds = 0;
        server.getPlayerList().broadcastSystemMessage(
            Component.literal("§6[Donjon] §fUne porte ancienne vient d'apparaitre : X " + site.getX()
                + ", Y " + site.getY() + ", Z " + site.getZ() + " (Overworld). Le portail attend une guilde."), false);
    }

    private static void applyTrapPulse(MinecraftServer server, ServerLevel level, DungeonSessionData session) {
        if (++trapCounter < 15) return;
        trapCounter = 0;
        for (String uuid : session.livingParticipantIds()) {
            ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuid));
            if (player == null || player.serverLevel() != level) continue;
            if (!DungeonArena.isTrap(player.blockPosition())) continue;
            player.hurt(level.damageSources().magic(), 3.0F);
            player.sendSystemMessage(Component.literal("§c[Piege] Le sol runique te brule ! Eloigne-toi des dalles rouges."));
        }
    }

    private static List<String> spawnWave(ServerLevel level, BlockPos site, int wave) {
        List<String> ids = new ArrayList<>();
        int localX = Math.floorMod(site.getX(), 16);
        int localZ = Math.floorMod(site.getZ(), 16);
        int sx = localX < 8 ? 1 : -1;
        int sz = localZ < 8 ? 1 : -1;
        int[][] offsets = {{sx * 2, 0}, {0, sz * 2}, {sx * 3, sz * 2}, {sx * 2, sz * 3}, {sx * 3, sz * 3}, {sx, sz * 4}};
        if (wave == 1) {
            addMob(level, site, EntityType.ZOMBIE, offsets[0], false, ids);
            addMob(level, site, EntityType.ZOMBIE, offsets[1], false, ids);
            addMob(level, site, EntityType.ZOMBIE, offsets[2], false, ids);
            addMob(level, site, EntityType.SPIDER, offsets[3], false, ids);
        } else if (wave == 2) {
            addMob(level, site, EntityType.SKELETON, offsets[0], false, ids);
            addMob(level, site, EntityType.SKELETON, offsets[1], false, ids);
            addMob(level, site, EntityType.HUSK, offsets[2], false, ids);
            addMob(level, site, EntityType.HUSK, offsets[3], false, ids);
            addMob(level, site, EntityType.SPIDER, offsets[4], false, ids);
        } else if (wave == 3) {
            addMob(level, site, EntityType.RAVAGER, offsets[0], true, ids);
            addMob(level, site, EntityType.VINDICATOR, offsets[2], false, ids);
            addMob(level, site, EntityType.VINDICATOR, offsets[4], false, ids);
        }
        return ids;
    }

    private static void addMob(ServerLevel level, BlockPos site, EntityType<? extends Mob> type,
                               int[] offset, boolean boss, List<String> ids) {
        BlockPos spawn = site.offset(offset[0], 0, offset[1]);
        if (!level.hasChunkAt(spawn) || !level.getBlockState(spawn).isAir()
            || !level.getBlockState(spawn.above()).isAir()) return;
        Mob mob = type.create(level);
        if (mob == null) return;
        mob.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        mob.addTag(MOB_TAG);
        mob.setPersistenceRequired();
        if (boss) {
            mob.setCustomName(Component.literal("§4Gardien des profondeurs"));
            mob.setCustomNameVisible(true);
            var health = mob.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) health.setBaseValue(120.0D);
            var damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) damage.setBaseValue(14.0D);
            mob.setHealth(120.0F);
        }
        if (level.addFreshEntity(mob)) ids.add(mob.getUUID().toString());
    }

    private static void completeVictory(MinecraftServer server, DungeonSessionData session) {
        session.finish(true);
        GuildSavedData data = GuildSavedData.get(server);
        data.recordDungeonWin(session.guildId());
        int rewarded = 0;
        int jadeReward = DungeonData.get(server).rewardJade();
        for (String uuid : session.livingParticipantIds()) {
            session.addPendingReward(uuid, jadeReward);
            ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuid));
            if (player == null) continue;
            int jade = session.claimPendingReward(uuid);
            if (jade > 0) {
                ItemStack reward = new ItemStack(CitizenJadeItems.RAW_JADE.get(), jade);
                if (!player.getInventory().add(reward)) player.drop(reward, false);
            }
            player.sendSystemMessage(Component.literal("§a[Donjon] Victoire ! Recompense : " + jade + " minerai(s) de Pierre de Jade."));
            rewarded++;
        }
        server.getPlayerList().broadcastSystemMessage(
            Component.literal("§6[Donjon] §aLa guilde " + session.guildName()
                + " a vaincu le Gardien des profondeurs ! Recompense : " + jadeReward + " Jade par survivant ; " + rewarded + " connecte(s)."), false);
    }
}
