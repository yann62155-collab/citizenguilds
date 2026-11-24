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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CitizenGuilds.MOD_ID)
public final class DungeonEvents {
    private static final String MOB_TAG = "citizenguilds_dungeon_mob";
    private static int tickCounter;
    private static int trapCounter;
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DungeonEvents.class);
    private DungeonEvents() {}

    @SubscribeEvent
    public static void verifyDungeonDimension(ServerStartedEvent event) {
        if (event.getServer().getLevel(CitizenDimensions.DUNGEON) == null) {
            LOGGER.error("CITIZEN_DUNGEON_DIMENSION_MISSING - data pack dimension was not registered");
        } else {
            LOGGER.info("CITIZEN_DUNGEON_DIMENSION_LOADED - dedicated instance is ready");
        }
    }

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
        RiftDungeonController.tick(server);
        if (!session.active()) {
            DungeonService.tickAutoSpawn(server);
            return;
        }
        ServerLevel level = server.getLevel(CitizenDimensions.DUNGEON);
        boolean atLeastOneInside = false;
        if (level != null && !session.finished()) {
            for (String uuid : session.livingParticipantIds()) {
                ServerPlayer player = DungeonService.online(server, uuid);
                if (player == null || player.serverLevel() != level) continue;
                atLeastOneInside = true;
                if (!DungeonArena.isInsideBounds(player.blockPosition())) {
                    BlockPos entrance = DungeonArena.entrance();
                    player.teleportTo(level, entrance.getX() + 0.5D, entrance.getY(), entrance.getZ() + 0.5D,
                        player.getYRot(), player.getXRot());
                    player.sendSystemMessage(Component.literal("§e[Donjon] Tu as ete replace dans les limites de l'instance."));
                }
            }
        }
        if (DungeonService.tickSession(server, session, atLeastOneInside)) return;
        if (level == null || session.site() == null || session.participantCount() == 0) return;
        if (session.aliveCount() == 0) {
            session.removeActiveMobs(level);
            GuildSavedData.get(server).recordDungeonFailure(session.guildId());
            session.finish(false);
            server.getPlayerList().broadcastSystemMessage(Component.literal("§c[Donjon] Tous les aventuriers ont ete elimines."), false);
            return;
        }
        if (!atLeastOneInside) return;

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
        int difficulty = DungeonData.get(level.getServer()).riftDifficulty();
        if (wave == 1) {
            List<EntityType<? extends Mob>> easy = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.ZOMBIE);
            List<EntityType<? extends Mob>> medium = List.of(EntityType.PILLAGER, EntityType.HUSK, EntityType.SPIDER, EntityType.STRAY);
            List<EntityType<? extends Mob>> hard = List.of(EntityType.VINDICATOR, EntityType.STRAY, EntityType.PILLAGER, EntityType.CAVE_SPIDER);
            List<EntityType<? extends Mob>> extreme = List.of(EntityType.EVOKER, EntityType.VINDICATOR, EntityType.WITCH, EntityType.VINDICATOR);
            List<EntityType<? extends Mob>> selected = difficulty == 0 ? easy : difficulty == 1 ? medium : difficulty == 2 ? hard : extreme;
            for (int i = 0; i < 4; i++) addMob(level, site, selected.get(i), offsets[i], false, ids);
        } else if (wave == 2) {
            List<EntityType<? extends Mob>> selected = switch (difficulty) {
                case 0 -> List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.HUSK, EntityType.SPIDER);
                case 1 -> List.of(EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.STRAY, EntityType.CAVE_SPIDER);
                case 2 -> List.of(EntityType.VINDICATOR, EntityType.EVOKER, EntityType.PILLAGER, EntityType.WITCH);
                default -> List.of(EntityType.EVOKER, EntityType.WITCH, EntityType.VINDICATOR, EntityType.VINDICATOR);
            };
            for (int i = 0; i < selected.size(); i++) addMob(level, site, selected.get(i), offsets[i], false, ids);
            addMiniBoss(level, site, difficulty, offsets[5], ids);
        } else if (wave == 3) {
            addRiftBoss(level, site, difficulty, offsets[0], ids);
            addMob(level, site, difficulty >= 2 ? EntityType.EVOKER : EntityType.VINDICATOR, offsets[2], false, ids);
            addMob(level, site, difficulty >= 1 ? EntityType.VINDICATOR : EntityType.SKELETON, offsets[4], false, ids);
        }
        return ids;
    }

    private static void addMiniBoss(ServerLevel level, BlockPos site, int difficulty, int[] offset, List<String> ids) {
        EntityType<? extends Mob> type = difficulty >= 2 ? EntityType.VINDICATOR : EntityType.PILLAGER;
        BlockPos spawn = site.offset(offset[0], 0, offset[1]);
        Mob mob = type.create(level);
        if (mob == null || !level.hasChunkAt(spawn) || !level.getBlockState(spawn).isAir()) return;
        mob.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, level.random.nextFloat() * 360F, 0F);
        mob.addTag(MOB_TAG);
        mob.setCustomName(Component.literal("§6Champion de la Faille"));
        mob.setCustomNameVisible(true);
        scaleMob(mob, 28.0D + difficulty * 18.0D, 6.0D + difficulty * 3.0D);
        if (level.addFreshEntity(mob)) ids.add(mob.getUUID().toString());
    }

    private static void addRiftBoss(ServerLevel level, BlockPos site, int difficulty, int[] offset, List<String> ids) {
        EntityType<? extends Mob> type = switch (difficulty) {
            case 0 -> EntityType.WITHER_SKELETON;
            case 1 -> EntityType.PIGLIN_BRUTE;
            case 2 -> EntityType.VINDICATOR;
            default -> EntityType.EVOKER;
        };
        String[] names = {"§8Chevalier noir", "§cChevalier rouge", "§5Chevalier violet", "§0Moine noir"};
        BlockPos spawn = site.offset(offset[0], 0, offset[1]);
        if (!level.hasChunkAt(spawn) || !level.getBlockState(spawn).isAir() || !level.getBlockState(spawn.above()).isAir()) return;
        Mob boss = type.create(level);
        if (boss == null) return;
        boss.moveTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, level.random.nextFloat() * 360F, 0F);
        boss.addTag(MOB_TAG);
        boss.setPersistenceRequired();
        boss.setCustomName(Component.literal(names[difficulty]));
        boss.setCustomNameVisible(true);
        switch (difficulty) {
            case 0 -> {
                boss.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                boss.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                boss.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
                boss.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
                boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
            }
            case 1 -> {
                boss.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                boss.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
            }
            case 2 -> {
                boss.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                boss.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                boss.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                boss.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
            }
            default -> boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        }
        double health = new double[]{140.0D, 220.0D, 320.0D, 460.0D}[difficulty];
        double damage = new double[]{9.0D, 13.0D, 18.0D, 23.0D}[difficulty];
        scaleMob(boss, health, damage);
        if (level.addFreshEntity(boss)) {
            ids.add(boss.getUUID().toString());
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("§5[Faille] §fLe boss " + names[difficulty] + " entre dans l'arène !"), false);
        }
    }

    private static void scaleMob(Mob mob, double health, double damage) {
        var maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(health);
        var attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(damage);
        mob.setHealth((float) health);
        mob.setPersistenceRequired();
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
            if (health != null) health.setBaseValue(180.0D);
            var damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) damage.setBaseValue(16.0D);
            mob.setHealth(180.0F);
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
        String[] bossNames = {"Chevalier noir", "Chevalier rouge", "Chevalier violet", "Moine noir"};
        String defeatedBoss = bossNames[DungeonData.get(server).riftDifficulty()];
        server.getPlayerList().broadcastSystemMessage(
            Component.literal("§6[Faille] §aLa guilde " + session.guildName()
                + " a vaincu " + defeatedBoss + " ! Recompense : " + jadeReward + " Jade par survivant ; " + rewarded + " connecte(s)."), false);
    }
}
