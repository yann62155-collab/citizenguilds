package fr.citizen.guilds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class DungeonPortalBlock extends Block {
    public DungeonPortalBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // A coherent spiral is emitted across the 3x3 gate aperture.
        // The particles are positioned in the portal plane, not scattered around the frame.
        double time = level.getGameTime() * 0.105D;
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.505D;
        for (int i = 0; i < 3; i++) {
            double angle = time + i * (Math.PI * 2.0D / 3.0D)
                + (pos.getX() * 0.31D + pos.getY() * 0.17D);
            double radius = 0.12D + 0.34D * (0.5D + 0.5D * Math.sin(time * 0.7D + i));
            double px = cx + Math.cos(angle) * radius;
            double py = cy + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.PORTAL, px, py, cz,
                -Math.sin(angle) * 0.025D, Math.cos(angle) * 0.025D, -0.012D);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL,
                cx + (random.nextDouble() - 0.5D) * 0.55D,
                cy + (random.nextDouble() - 0.5D) * 0.55D,
                cz, 0.0D, 0.0D, -0.015D);
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        DungeonSessionData session = DungeonSessionData.get(serverPlayer.getServer());
        if (level.dimension().equals(CitizenDimensions.DUNGEON)) {
            if (!session.active() || session.site() == null || !session.isParticipant(serverPlayer.getUUID().toString())) {
                serverPlayer.teleportTo(serverPlayer.getServer().overworld(),
                    serverPlayer.getServer().overworld().getSharedSpawnPos().getX() + 0.5D,
                    serverPlayer.getServer().overworld().getSharedSpawnPos().getY(),
                    serverPlayer.getServer().overworld().getSharedSpawnPos().getZ() + 0.5D,
                    serverPlayer.getYRot(), serverPlayer.getXRot());
                return InteractionResult.CONSUME;
            }
            BlockPos exit = DungeonService.exitPosition(serverPlayer.getServer().overworld(), session.site());
            serverPlayer.teleportTo(serverPlayer.getServer().overworld(),
                exit.getX() + 0.5D, exit.getY(), exit.getZ() + 0.5D,
                serverPlayer.getYRot(), serverPlayer.getXRot());
            serverPlayer.sendSystemMessage(Component.literal("§6[Donjon] Tu as quitte l'instance. Tant que tu n'es pas elimine, tu peux y retourner par la porte."));
            return InteractionResult.CONSUME;
        }
        if (!session.active()) {
            GuildSavedData.Guild guild = GuildSavedData.get(serverPlayer.getServer()).guildOf(serverPlayer);
            if (guild == null) {
                serverPlayer.sendSystemMessage(Component.literal("§c[Donjon] Seuls les membres d'une guilde peuvent ouvrir cette porte. Fonde ou rejoins une guilde avec la touche N."));
                return InteractionResult.CONSUME;
            }
            DungeonData data = DungeonData.get(serverPlayer.getServer());
            if (!pos.equals(data.lastSite())) {
                serverPlayer.sendSystemMessage(Component.literal("§c[Donjon] Cette porte est eteinte."));
                return InteractionResult.CONSUME;
            }
            DungeonService.Result started = DungeonService.startSession(serverPlayer.getServer(), guild);
            if (!started.ok()) {
                serverPlayer.sendSystemMessage(Component.literal("§c[Donjon] " + started.message()));
                return InteractionResult.CONSUME;
            }
        }
        if (session.finished()) {
            serverPlayer.sendSystemMessage(Component.literal(session.victory() ? "§a[Donjon] Cette session est deja terminee par une victoire." : "§c[Donjon] Cette session est terminee apres elimination de l\u0027equipe."));
            return InteractionResult.CONSUME;
        }
        if (!session.site().equals(pos)) {
            serverPlayer.sendSystemMessage(Component.literal("§c[Donjon] Cette porte n'est pas rattachee a la session active."));
            return InteractionResult.CONSUME;
        }
        if (session.isEliminated(serverPlayer.getUUID().toString())) {
            serverPlayer.sendSystemMessage(Component.literal("§c[Donjon] Tu as ete elimine de cette session. Une nouvelle session sera necessaire."));
            return InteractionResult.CONSUME;
        }
        if (!session.join(serverPlayer, GuildSavedData.get(serverPlayer.getServer()))) {
            serverPlayer.sendSystemMessage(Component.literal("§c[Donjon] Seuls les membres de la guilde inscrite peuvent entrer."));
            return InteractionResult.CONSUME;
        }
        ServerLevel dungeon = serverPlayer.getServer().getLevel(CitizenDimensions.DUNGEON);
        if (dungeon == null) {
            serverPlayer.sendSystemMessage(Component.literal("§c[Donjon] Le monde instancie n'est pas charge. Redemarre le serveur apres installation de la mise a jour."));
            return InteractionResult.CONSUME;
        }
        BlockPos entrance = DungeonArena.entrance();
        serverPlayer.teleportTo(dungeon, entrance.getX() + 0.5D, entrance.getY(), entrance.getZ() + 0.5D,
            serverPlayer.getYRot(), serverPlayer.getXRot());
        serverPlayer.sendSystemMessage(Component.literal("§6[Donjon] §fTu entres dans l'instance de " + session.guildName() + ". Une seule vie : ta mort t'eliminera de cette session."));
        return InteractionResult.CONSUME;
    }
}
