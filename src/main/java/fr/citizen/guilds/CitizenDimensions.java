package fr.citizen.guilds;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Dedicated, isolated dimension used for guild dungeon instances. */
public final class CitizenDimensions {
    public static final ResourceKey<Level> DUNGEON = ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath(CitizenGuilds.MOD_ID, "dungeon")
    );
    private CitizenDimensions() {}
}
