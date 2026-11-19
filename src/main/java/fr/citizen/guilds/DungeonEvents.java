package fr.citizen.guilds;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = CitizenGuilds.MOD_ID)
public final class DungeonEvents {
    private DungeonEvents() {}

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.getServer() == null) return;
        DungeonSessionData session = DungeonSessionData.get(player.getServer());
        if (!session.eliminate(player)) return;
        player.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("§c[Donjon] §f" + player.getGameProfile().getName()
                + " est elimine de la session de " + session.guildName() + " (une seule vie)."), false);
    }
}
