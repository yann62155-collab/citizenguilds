package fr.citizen.guilds;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Builds and safely removes the overworld dungeon portal monument. */
public final class DungeonPortalStructure {
    private DungeonPortalStructure() {}

    private static Map<BlockPos, Block> layout(BlockPos ground) {
        Map<BlockPos, Block> blocks = new LinkedHashMap<>();
        // Heavy basalt foundation and two monumental side pillars.
        for (int x = -2; x <= 2; x++) blocks.put(ground.offset(x, 0, 0), Blocks.POLISHED_DEEPSLATE);
        for (int y = 1; y <= 4; y++) {
            blocks.put(ground.offset(-2, y, 0), y == 4 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS);
            blocks.put(ground.offset(2, y, 0), y == 4 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
        // Crown and lintel.
        for (int x = -2; x <= 2; x++) {
            blocks.put(ground.offset(x, 5, 0), x == 0 ? Blocks.AMETHYST_BLOCK : Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
        blocks.put(ground.offset(-1, 4, 0), Blocks.CRYING_OBSIDIAN);
        blocks.put(ground.offset(1, 4, 0), Blocks.CRYING_OBSIDIAN);
        // Jade-like luminous accents on the outer shoulders.
        blocks.put(ground.offset(-2, 3, 0), Blocks.SEA_LANTERN);
        blocks.put(ground.offset(2, 3, 0), Blocks.SEA_LANTERN);
        // The interactive, animated portal core remains at the exact tracked site.
        blocks.put(ground.above(2), CitizenDungeonBlocks.DUNGEON_GATE.get());
        return blocks;
    }

    public static boolean canBuild(ServerLevel level, BlockPos ground) {
        for (Map.Entry<BlockPos, Block> entry : layout(ground).entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return false;
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).canBeReplaced()) return false;
        }
        return true;
    }

    public static boolean place(ServerLevel level, BlockPos ground) {
        if (!canBuild(level, ground)) return false;
        for (Map.Entry<BlockPos, Block> entry : layout(ground).entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue().defaultBlockState(), 3);
        }
        return true;
    }

    public static void remove(ServerLevel level, BlockPos portalCenter) {
        BlockPos ground = portalCenter.below(2);
        for (BlockPos pos : layout(ground).keySet()) {
            BlockState state = level.getBlockState(pos);
            Block expected = layout(ground).get(pos);
            if (state.is(expected)) level.removeBlock(pos, false);
        }
    }
}
