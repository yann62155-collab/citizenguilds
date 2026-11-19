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
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.PORTAL,
                pos.getX() + random.nextDouble(),
                pos.getY() + 0.5D + (random.nextDouble() - 0.5D) * 0.5D,
                pos.getZ() + random.nextDouble(),
                (random.nextDouble() - 0.5D) * 0.4D,
                (random.nextDouble() - 0.5D) * 0.4D,
                (random.nextDouble() - 0.5D) * 0.4D);
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
            BlockPos exit = session.site();
            serverPlayer.teleportTo(serverPlayer.getServer().overworld(),
                exit.getX() + 2.5D, exit.getY(), exit.getZ() + 2.5D,
                serverPlayer.getYRot(), serverPlayer.getXRot());
            serverPlayer.sendSystemMessage(Component.literal("§6[Donjon] Tu as quitte l'instance. Tant que tu n'es pas elimine, tu peux y retourner par la porte."));
            return InteractionResult.CONSUME;
        }
        if (!session.active()) {
            serverPlayer.sendSystemMessage(Component.literal("§c[Donjon] Aucun donjon n'est actuellement ouvert."));
            return InteractionResult.CONSUME;
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
