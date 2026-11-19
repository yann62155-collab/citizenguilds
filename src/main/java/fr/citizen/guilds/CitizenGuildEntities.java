package fr.citizen.guilds;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class CitizenGuildEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, CitizenGuilds.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<DeepGuardianEntity>> DEEP_GUARDIAN =
        ENTITY_TYPES.register("deep_guardian", () -> EntityType.Builder
            .of(DeepGuardianEntity::new, MobCategory.MONSTER)
            .sized(1.8F, 2.6F)
            .clientTrackingRange(10)
            .build("citizenguilds:deep_guardian"));

    private CitizenGuildEntities() {}
}
