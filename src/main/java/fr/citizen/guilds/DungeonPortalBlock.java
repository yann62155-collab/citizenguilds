package fr.citizen.guilds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        DungeonSessionData session = DungeonSessionData.get(serverPlayer.getServer());
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
        serverPlayer.sendSystemMessage(Component.literal("§6[Donjon] §fInscription validee pour la guilde " + session.guildName() + ". Une seule vie : ta mort t'eliminera de cette session."));
        return InteractionResult.CONSUME;
    }
}
