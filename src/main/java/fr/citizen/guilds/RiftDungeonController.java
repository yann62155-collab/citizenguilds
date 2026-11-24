package fr.citizen.guilds;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

/** Controls the permanent hub gyroscope and the sky fracture announcing dungeon openings. */
public final class RiftDungeonController {
    private static final long CHARGE_TICKS = 20L * 20L;
    private static final long MINUTE_TICKS = 20L * 60L;
    private static final int[] CHANCES = {5, 15, 30, 50, 75, 95};
    private static BlockPos pendingSite;
    private static long chargeStartedAt;
    private static UUID mechanismId;

    private RiftDungeonController() {}

    private static RiftMechanismEntity findMechanism(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(4.0D);
        return level.getEntitiesOfClass(RiftMechanismEntity.class, area,
            entity -> entity.blockPosition().equals(pos)).stream().findFirst().orElse(null);
    }

    public static DungeonService.Result setHubPosition(MinecraftServer server, BlockPos pos) {
        ServerLevel overworld = server.overworld();
        if (!overworld.hasChunkAt(pos)) return DungeonService.Result.fail("Le chunk du Rift doit etre charge.");
        DungeonData data = DungeonData.get(server);
        BlockPos old = data.riftPosition();
        if (old != null) {
            RiftMechanismEntity previous = findMechanism(overworld, old);
            if (previous != null) previous.discard();
        }
        RiftMechanismEntity mechanism = CitizenGuildEntities.RIFT_MECHANISM.get().create(overworld);
        if (mechanism == null) return DungeonService.Result.fail("Impossible de creer le mecanisme celeste.");
        mechanism.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        mechanism.setIdleSpeed(1.0F);
        if (!overworld.addFreshEntity(mechanism)) return DungeonService.Result.fail("Impossible d'ajouter le Rift au hub.");
        data.setRiftPosition(pos);
        if (data.nextSpawnGameTime() <= 0L) data.setNextSpawnGameTime(overworld.getGameTime() + data.spawnIntervalMinutes() * MINUTE_TICKS);
        data.setNextRiftCheckGameTime(0L);
        return DungeonService.Result.ok("Rift installe en X " + pos.getX() + ", Y " + pos.getY() + ", Z " + pos.getZ() + ".");
    }

    public static DungeonService.Result removeHubRift(MinecraftServer server) {
        DungeonData data = DungeonData.get(server);
        BlockPos pos = data.riftPosition();
        if (pos == null) return DungeonService.Result.fail("Aucun Rift de hub n'est configure.");
        RiftMechanismEntity entity = findMechanism(server.overworld(), pos);
        if (entity != null) entity.discard();
        if (mechanismId != null) {
            var charging = server.overworld().getEntity(mechanismId);
            if (charging != null) charging.discard();
        }
        pendingSite = null;
        mechanismId = null;
        data.setRiftPosition(null);
        return DungeonService.Result.ok("Rift du hub retire.");
    }

    public static String status(MinecraftServer server) {
        DungeonData data = DungeonData.get(server);
        BlockPos pos = data.riftPosition();
        if (pos == null) return "Aucun Rift configure. Place-toi au hub et utilise /guild rift set.";
        int level = data.riftLevel();
        int chance = level == 0 ? 0 : CHANCES[level - 1];
        long remaining = Math.max(0L, data.nextRiftCheckGameTime() - server.overworld().getGameTime());
        long minutes = (remaining + MINUTE_TICKS - 1L) / MINUTE_TICKS;
        return "Rift en X " + pos.getX() + ", Y " + pos.getY() + ", Z " + pos.getZ()
            + " | Niveau " + level + "/6 | Chance actuelle " + chance + "% | Prochain controle dans ~" + minutes + " min.";
    }

    /** Draws a jagged purple tear above the active dungeon site while its gate remains open. */
    private static void emitSkyFracture(ServerLevel level, BlockPos gate) {
        int skyY = Math.min(level.getMaxBuildHeight() - 12, gate.getY() + 68);
        double centerX = gate.getX() + 0.5D;
        double centerZ = gate.getZ() + 0.5D;
        double[][] crack = {
            {-8.0D, 12.0D}, {-4.0D, 8.0D}, {1.0D, 4.0D},
            {-2.0D, 0.0D}, {3.0D, -4.0D}, {-1.0D, -8.0D}, {4.0D, -12.0D}
        };
        // Send the particles directly to every player in the Overworld, even when far away.
        // The longDistance flag prevents the normal 32-block particle visibility limit.
        for (ServerPlayer player : level.players()) {
            for (int i = 0; i < crack.length; i++) {
                double x = centerX + crack[i][0];
                double y = skyY + crack[i][1];
                level.sendParticles(player, ParticleTypes.PORTAL, true, x, y, centerZ,
                    5, 0.18D, 0.45D, 0.18D, 0.02D);
                if (i % 2 == 0) {
                    level.sendParticles(player, ParticleTypes.ELECTRIC_SPARK, true, x, y, centerZ,
                        2, 0.25D, 0.35D, 0.25D, 0.01D);
                }
            }
        }
    }

    private static void thunderFracture(MinecraftServer server, BlockPos gate) {
        ServerLevel level = server.overworld();
        emitSkyFracture(level, gate);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 8.0F, 0.65F);
            player.playNotifySound(SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 5.0F, 0.8F);
        }
    }

    /** Uses CitizenGuilds' existing next-spawn timer; the gyroscope accelerates as it approaches zero. */
    public static void tickProgression(MinecraftServer server) {
        DungeonData data = DungeonData.get(server);
        BlockPos pos = data.riftPosition();
        if (pos == null) return;
        ServerLevel overworld = server.overworld();
        RiftMechanismEntity mechanism = findMechanism(overworld, pos);
        if (mechanism == null && overworld.hasChunkAt(pos)) {
            mechanism = CitizenGuildEntities.RIFT_MECHANISM.get().create(overworld);
            if (mechanism != null) {
                mechanism.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
                mechanism.setIdleSpeed(1.0F);
                overworld.addFreshEntity(mechanism);
            }
        }
        if (DungeonService.isPortalPresent(server)) {
            if (overworld.getGameTime() % 5L == 0L) emitSkyFracture(overworld, data.lastSite());
            if (mechanism != null) mechanism.setIdleSpeed(1.0F);
            return;
        }
        if (!data.autoSpawnEnabled() || DungeonSessionData.get(server).active() || isCharging()) {
            if (mechanism != null) mechanism.setIdleSpeed(1.0F);
            return;
        }

        long now = overworld.getGameTime();
        long next = data.nextSpawnGameTime();
        if (next <= 0L) {
            next = now + data.spawnIntervalMinutes() * MINUTE_TICKS;
            data.setNextSpawnGameTime(next);
        }
        long total = Math.max(1L, data.spawnIntervalMinutes() * MINUTE_TICKS);
        long remaining = Math.max(0L, next - now);
        float progress = 1.0F - Math.min(1.0F, (float) remaining / (float) total);
        float speed = 1.0F + progress * 7.0F;
        if (mechanism != null) mechanism.setIdleSpeed(speed);
        int level = Math.min(6, Math.max(0, Math.round(progress * 6.0F)));
        if (data.riftLevel() != level) data.setRiftLevel(level);

        if (remaining > 0L || server.getPlayerList().getPlayerCount() == 0) return;
        DungeonSiteFinder.Search search = DungeonSiteFinder.findAuto(overworld, data);
        data.setLastSearchNote(search.note());
        if (search.site() == null) {
            data.setNextSpawnGameTime(now + 20L * 60L);
            if (mechanism != null) mechanism.setIdleSpeed(1.0F);
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                "§c[Rift] §fLe cycle est termine, mais aucun emplacement valide n'a ete trouve. Nouvelle tentative dans 1 minute."), false);
            return;
        }
        DungeonService.Result result = begin(server, search.site());
        if (!result.ok()) {
            data.setNextSpawnGameTime(now + 20L * 60L);
            if (mechanism != null) mechanism.setIdleSpeed(1.0F);
        }
    }

    public static DungeonService.Result begin(MinecraftServer server, BlockPos ground) {
        if (pendingSite != null) return DungeonService.Result.fail("Une faille est deja en cours de formation.");
        if (DungeonSessionData.get(server).active()) return DungeonService.Result.fail("Une expedition est deja en cours.");
        if (DungeonService.isPortalPresent(server)) return DungeonService.Result.fail("Une porte de donjon est deja ouverte.");
        ServerLevel overworld = server.overworld();
        DungeonData data = DungeonData.get(server);
        BlockPos hub = data.riftPosition();
        if (hub == null) return DungeonService.Result.fail("Configure d'abord le Rift au hub avec /guild rift set.");
        if (!overworld.hasChunkAt(ground) || !overworld.hasChunkAt(hub))
            return DungeonService.Result.fail("Le chunk du site ou du Rift n'est pas charge.");
        RiftMechanismEntity mechanism = findMechanism(overworld, hub);
        if (mechanism == null) {
            mechanism = CitizenGuildEntities.RIFT_MECHANISM.get().create(overworld);
            if (mechanism == null) return DungeonService.Result.fail("Impossible de creer le mecanisme celeste.");
            mechanism.moveTo(hub.getX() + 0.5D, hub.getY(), hub.getZ() + 0.5D, 0.0F, 0.0F);
            if (!overworld.addFreshEntity(mechanism)) return DungeonService.Result.fail("Impossible d'ajouter le mecanisme au hub.");
        }
        mechanism.setCharging(true, 8.0F);
        pendingSite = ground.immutable();
        chargeStartedAt = overworld.getGameTime();
        mechanismId = mechanism.getUUID();
        server.getPlayerList().broadcastSystemMessage(Component.literal(
            "§6[Rift] §fLe gyroscope du hub entre en resonance ! Un donjon va s'ouvrir dans 20 secondes..."), false);
        return DungeonService.Result.ok("Le Rift du hub s'active. Ouverture du donjon dans 20 secondes.");
    }

    public static void tick(MinecraftServer server) {
        if (pendingSite == null) return;
        ServerLevel overworld = server.overworld();
        if (overworld.getGameTime() - chargeStartedAt < CHARGE_TICKS) return;
        BlockPos site = pendingSite;
        pendingSite = null;
        DungeonService.Result result = DungeonService.placePortal(server, site);
        var mechanism = mechanismId == null ? null : overworld.getEntity(mechanismId);
        if (mechanism instanceof RiftMechanismEntity rift) rift.setIdleSpeed(1.0F);
        if (!result.ok()) {
            if (mechanism != null) mechanism.discard();
            mechanismId = null;
            server.getPlayerList().broadcastSystemMessage(Component.literal("§c[Rift] §fLa formation du donjon a echoue : " + result.message()), false);
            return;
        }
        DungeonData data = DungeonData.get(server);
        data.setRiftLevel(0);
        data.setNextRiftCheckGameTime(0L);
        thunderFracture(server, site.above(2));
        server.getPlayerList().broadcastSystemMessage(Component.literal("§5[Rift] §dLa fracture dechire le ciel dans un grondement de tonnerre ! Un donjon vient d'apparaitre."), false);
    }

    public static void clear(MinecraftServer server) {
        if (mechanismId != null) {
            var entity = server.overworld().getEntity(mechanismId);
            if (entity instanceof RiftMechanismEntity rift) rift.setIdleSpeed(1.0F);
        }
        pendingSite = null;
        mechanismId = null;
        chargeStartedAt = 0L;
        DungeonData data = DungeonData.get(server);
        data.setRiftLevel(0);
        data.setNextRiftCheckGameTime(server.overworld().getGameTime() + MINUTE_TICKS);
    }

    public static boolean isCharging() { return pendingSite != null; }
}
