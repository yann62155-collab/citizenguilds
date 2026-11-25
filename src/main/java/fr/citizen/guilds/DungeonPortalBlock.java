package fr.citizen.guilds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Invisible, pass-through portal field. Walking through the rendered gate
 * automatically enters the guild dungeon; right-click remains as a fallback.
 */
public final class DungeonPortalBlock extends Block {
    public DungeonPortalBlock(BlockBehaviour.Properties properties) { super(properties); }

    /** The 3x3 trigger field must never render as purple/black missing-texture cubes. */
    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double time = level.getGameTime() * 0.105D;
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.505D;
        for (int i = 0; i < 2; i++) {
            double angle = time + i * Math.PI + pos.getX() * 0.31D + pos.getY() * 0.17D;
            double radius = 0.10D + 0.22D * (0.5D + 0.5D * Math.sin(time * 0.7D + i));
            level.addParticle(ParticleTypes.PORTAL,
                cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, cz,
                -Math.sin(angle) * 0.02D, Math.cos(angle) * 0.02D, -0.01D);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL,
                cx + (random.nextDouble() - 0.5D) * 0.45D,
                cy + (random.nextDouble() - 0.5D) * 0.45D,
                cz, 0.0D, 0.0D, -0.012D);
        }
    }

    /** Crossing any part of the 3x3 portal plane triggers entry without interaction. */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;
        enterOrExit(player, pos);
    }

    /** Right-click fallback for players who prefer the old interaction. */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                             Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        enterOrExit(serverPlayer, pos);
        return InteractionResult.CONSUME;
    }

    private static void enterOrExit(ServerPlayer player, BlockPos touchedPos) {
        Level currentLevel = player.level();
        if (!(currentLevel instanceof ServerLevel currentServerLevel)) return;
        DungeonSessionData session = DungeonSessionData.get(player.getServer());

        // The return gate inside the dungeon sends living participants back beside the overworld portal.
        if (currentLevel.dimension().equals(CitizenDimensions.DUNGEON)) {
            if (!session.active() || session.site() == null
                    || !session.isParticipant(player.getUUID().toString())) {
                BlockPos spawn = player.getServer().overworld().getSharedSpawnPos();
                player.teleportTo(player.getServer().overworld(),
                    spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                    player.getYRot(), player.getXRot());
                return;
            }
            BlockPos exit = DungeonService.exitPosition(player.getServer().overworld(), session.site());
            player.teleportTo(player.getServer().overworld(),
                exit.getX() + 0.5D, exit.getY(), exit.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
            player.sendSystemMessage(Component.literal(
                "§6[Donjon] Tu as quitte l'instance. Tu peux y retourner en traversant le portail."));
            return;
        }

        if (!currentLevel.dimension().equals(Level.OVERWORLD)) return;

        if (!session.active()) {
            GuildSavedData.Guild guild = GuildSavedData.get(player.getServer()).guildOf(player);
            if (guild == null) {
                if (player.tickCount % 20 == 0) player.sendSystemMessage(Component.literal(
                    "§c[Donjon] Seuls les membres d'une guilde peuvent entrer dans ce portail."));
                return;
            }
            DungeonData data = DungeonData.get(player.getServer());
            BlockPos activeGate = data.lastSite();
            if (!data.hasPlacedPortal() || activeGate == null
                    || Math.abs(touchedPos.getX() - activeGate.getX()) > 1
                    || Math.abs(touchedPos.getY() - activeGate.getY()) > 1
                    || touchedPos.getZ() != activeGate.getZ()) {
                // The saved portal site is authoritative; stale gate blocks cannot start a session.
                if (player.tickCount % 20 == 0) player.sendSystemMessage(Component.literal(
                    "§c[Donjon] Cette porte n'est plus active."));
                return;
            }
            DungeonService.Result started = DungeonService.startSession(player.getServer(), guild);
            if (!started.ok()) {
                if (player.tickCount % 20 == 0) player.sendSystemMessage(Component.literal("§c[Donjon] " + started.message()));
                return;
            }
        }

        if (session.finished()) {
            if (player.tickCount % 20 == 0) player.sendSystemMessage(Component.literal(
                session.victory() ? "§a[Donjon] Cette session est deja terminee par une victoire."
                    : "§c[Donjon] Cette session est terminee apres elimination de l'equipe."));
            return;
        }
        if (session.site() == null || !session.isParticipant(player.getUUID().toString())) {
            // Only members of the guild that owns the current expedition may enter.
            if (!session.join(player, GuildSavedData.get(player.getServer()))) {
                if (player.tickCount % 20 == 0) player.sendSystemMessage(Component.literal(
                    "§c[Donjon] Seuls les membres de la guilde inscrite peuvent entrer."));
                return;
            }
        }

        ServerLevel dungeon = player.getServer().getLevel(CitizenDimensions.DUNGEON);
        if (dungeon == null) {
            if (player.tickCount % 20 == 0) player.sendSystemMessage(Component.literal(
                "§c[Donjon] Le monde instancie n'est pas charge. Redemarre le serveur."));
            return;
        }
        BlockPos entrance = DungeonArena.entrance();
        player.teleportTo(dungeon, entrance.getX() + 0.5D, entrance.getY(), entrance.getZ() + 0.5D,
            player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("§6[Donjon] §fTu entres dans l'instance de "
            + session.guildName() + ". Une seule vie : ta mort t'eliminera de cette session."));
    }
}
