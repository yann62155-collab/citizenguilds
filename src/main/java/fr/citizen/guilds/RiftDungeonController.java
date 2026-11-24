package fr.citizen.guilds;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Connects the visual rift charge to the existing CitizenGuilds dungeon portal lifecycle. */
public final class RiftDungeonController {
    private static final long CHARGE_TICKS = 20L * 20L;
    private static BlockPos pendingSite;
    private static long chargeStartedAt;
    private static UUID mechanismId;

    private RiftDungeonController() {}

    public static DungeonService.Result begin(MinecraftServer server, BlockPos ground) {
        if (pendingSite != null) return DungeonService.Result.fail("Une faille est deja en cours de formation.");
        if (DungeonSessionData.get(server).active()) return DungeonService.Result.fail("Une expedition est deja en cours.");
        if (DungeonService.isPortalPresent(server)) return DungeonService.Result.fail("Une porte de donjon est deja ouverte.");
        ServerLevel overworld = server.overworld();
        if (!overworld.hasChunkAt(ground)) return DungeonService.Result.fail("Le site de faille n'est pas charge.");
        RiftMechanismEntity mechanism = CitizenGuildEntities.RIFT_MECHANISM.get().create(overworld);
        if (mechanism == null) return DungeonService.Result.fail("Impossible de creer le mecanisme celeste.");
        mechanism.moveTo(ground.getX() + 0.5D, ground.getY() + 8.0D, ground.getZ() + 0.5D, 0.0F, 0.0F);
        mechanism.setCharging(true, 8.0F);
        if (!overworld.addFreshEntity(mechanism)) return DungeonService.Result.fail("Impossible d'ajouter le mecanisme au monde.");
        pendingSite = ground.immutable();
        chargeStartedAt = overworld.getGameTime();
        mechanismId = mechanism.getUUID();
        server.getPlayerList().broadcastSystemMessage(Component.literal(
            "§6[Faille] §fUn mecanisme celeste s'active au-dessus d'un ancien site. Ouverture dans 20 secondes..."), false);
        return DungeonService.Result.ok("Mecanisme celeste active. La porte du donjon s'ouvrira dans 20 secondes.");
    }

    public static void tick(MinecraftServer server) {
        if (pendingSite == null) return;
        ServerLevel overworld = server.overworld();
        if (overworld.getGameTime() - chargeStartedAt < CHARGE_TICKS) return;
        BlockPos site = pendingSite;
        pendingSite = null;
        mechanismId = null;
        DungeonService.Result result = DungeonService.placePortal(server, site);
        if (!result.ok()) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("§c[Faille] §fLa formation de la porte a echoue : " + result.message()), false);
            return;
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal("§5[Faille] §dLa faille s'ouvre ! La porte du donjon est apparue."), false);
    }

    public static void clear(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (mechanismId != null) {
            var entity = overworld.getEntity(mechanismId);
            if (entity != null) entity.discard();
        }
        pendingSite = null;
        mechanismId = null;
        chargeStartedAt = 0L;
    }

    public static boolean isCharging() { return pendingSite != null; }
}
