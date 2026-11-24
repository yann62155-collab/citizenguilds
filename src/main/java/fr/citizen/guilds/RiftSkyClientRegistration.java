package fr.citizen.guilds;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Client-only receiver registration for the server-authoritative rift state. */
@EventBusSubscriber(modid = CitizenGuilds.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RiftSkyClientRegistration {
    private RiftSkyClientRegistration() {}

    @SubscribeEvent
    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(RiftSkyStatePayload.TYPE, RiftSkyStatePayload.CODEC,
            (payload, context) -> context.enqueueWork(() -> RiftSkyClient.setActive(payload.active())));
    }
}
