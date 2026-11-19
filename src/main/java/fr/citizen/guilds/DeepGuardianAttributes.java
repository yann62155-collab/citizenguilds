package fr.citizen.guilds;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = CitizenGuilds.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DeepGuardianAttributes {
    private DeepGuardianAttributes() {}

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(CitizenGuildEntities.DEEP_GUARDIAN.get(), DeepGuardianEntity.createAttributes().build());
    }
}
