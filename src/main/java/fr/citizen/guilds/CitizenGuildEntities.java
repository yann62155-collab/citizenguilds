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

    public static final DeferredHolder<EntityType<?>, EntityType<RiftMechanismEntity>> RIFT_MECHANISM =
        ENTITY_TYPES.register("rift_mechanism", () -> EntityType.Builder
            .of(RiftMechanismEntity::new, MobCategory.MISC)
            .sized(3.0F, 3.0F)
            .clientTrackingRange(128)
            .updateInterval(1)
            .build("citizenguilds:rift_mechanism"));

    public static final DeferredHolder<EntityType<?>, EntityType<DungeonGateVisualEntity>> DUNGEON_GATE_VISUAL =
        ENTITY_TYPES.register("dungeon_gate_visual", () -> EntityType.Builder
            .of(DungeonGateVisualEntity::new, MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .build("citizenguilds:dungeon_gate_visual"));

    private CitizenGuildEntities() {}
}
