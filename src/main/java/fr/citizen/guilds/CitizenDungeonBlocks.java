package fr.citizen.guilds;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CitizenDungeonBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, CitizenGuilds.MOD_ID);
    public static final DeferredHolder<Block, Block> DUNGEON_GATE = BLOCKS.register("dungeon_gate",
        () -> new DungeonPortalBlock(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F)
            .noLootTable().lightLevel(state -> 12).noOcclusion()));
    private CitizenDungeonBlocks() {}
}
