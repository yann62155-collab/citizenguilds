package fr.citizen.guilds;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Sends the saved global fracture state to players as they enter the server. */
@EventBusSubscriber(modid = CitizenGuilds.MOD_ID)
public final class RiftSkySyncEvents {
    private RiftSkySyncEvents() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean active = DungeonData.get(player.getServer()).fractureActive();
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new RiftSkyStatePayload(active));
    }
}
