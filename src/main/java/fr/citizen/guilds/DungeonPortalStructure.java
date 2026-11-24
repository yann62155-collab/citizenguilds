package fr.citizen.guilds;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Monumental dungeon gate inspired by the Citizen reference:
 * dark gothic arch, a 3x3 animated rift and a suspended amethyst crystal
 * with gold collars. The central gate block remains the interaction anchor.
 */
public final class DungeonPortalStructure {
    private DungeonPortalStructure() {}

    private static Map<BlockPos, Block> layout(BlockPos ground, int difficulty) {
        Map<BlockPos, Block> blocks = new LinkedHashMap<>();

        // Broad dark plinth.
        for (int x = -3; x <= 3; x++) {
            blocks.put(ground.offset(x, 0, 0), x == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.POLISHED_BLACKSTONE_BRICKS);
        }

        // Gothic side pillars, with stepped shoulders and pink rune inlays.
        for (int y = 1; y <= 5; y++) {
            blocks.put(ground.offset(-2, y, 0), y == 5 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS);
            blocks.put(ground.offset(2, y, 0), y == 5 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS);
            if (y >= 2 && y <= 4) {
                blocks.put(ground.offset(-3, y, 0), Blocks.POLISHED_BLACKSTONE);
                blocks.put(ground.offset(3, y, 0), Blocks.POLISHED_BLACKSTONE);
            }
        }
        blocks.put(ground.offset(-1, 5, 0), Blocks.POLISHED_BLACKSTONE_BRICKS);
        blocks.put(ground.offset(1, 5, 0), Blocks.POLISHED_BLACKSTONE_BRICKS);
        blocks.put(ground.offset(-1, 4, 0), Blocks.CRYING_OBSIDIAN);
        blocks.put(ground.offset(1, 4, 0), Blocks.CRYING_OBSIDIAN);
        blocks.put(ground.offset(-3, 3, 0), Blocks.MAGENTA_STAINED_GLASS);
        blocks.put(ground.offset(3, 3, 0), Blocks.MAGENTA_STAINED_GLASS);

        // The opening is a real 3x3 field of gate blocks; only its center is clickable.
        for (int x = -1; x <= 1; x++) {
            for (int y = 2; y <= 4; y++) {
                blocks.put(ground.offset(x, y, 0), CitizenDungeonBlocks.DUNGEON_GATE.get());
            }
        }

        // Suspended faceted crystal: tapered amethyst body with side shards.
        Block amethyst = crystalBlock(difficulty);
        blocks.put(ground.offset(0, 7, 0), amethyst);
        blocks.put(ground.offset(0, 8, 0), Blocks.AMETHYST_BLOCK);
        blocks.put(ground.offset(0, 9, 0), amethyst);
        blocks.put(ground.offset(0, 10, 0), Blocks.AMETHYST_CLUSTER);
        blocks.put(ground.offset(-1, 8, 0), Blocks.LARGE_AMETHYST_BUD);
        blocks.put(ground.offset(1, 8, 0), Blocks.LARGE_AMETHYST_BUD);
        blocks.put(ground.offset(-1, 9, 0), Blocks.AMETHYST_BLOCK);
        blocks.put(ground.offset(1, 9, 0), Blocks.AMETHYST_BLOCK);

        // Two gold collars around the crystal, with lateral gold pins.
        for (int x : new int[] {-1, 1}) {
            blocks.put(ground.offset(x, 8, 0), Blocks.GOLD_BLOCK);
            blocks.put(ground.offset(x, 10, 0), Blocks.GOLD_BLOCK);
        }
        blocks.put(ground.offset(-2, 8, 0), Blocks.GOLD_BLOCK);
        blocks.put(ground.offset(2, 8, 0), Blocks.GOLD_BLOCK);
        blocks.put(ground.offset(-2, 10, 0), Blocks.AMETHYST_CLUSTER);
        blocks.put(ground.offset(2, 10, 0), Blocks.AMETHYST_CLUSTER);

        // Small luminous violet accents around the crown.
        blocks.put(ground.offset(-2, 6, 0), Blocks.PURPLE_STAINED_GLASS);
        blocks.put(ground.offset(2, 6, 0), Blocks.PURPLE_STAINED_GLASS);
        blocks.put(ground.offset(0, 6, 0), Blocks.AMETHYST_BLOCK);
        return blocks;
    }

    private static Block crystalBlock(int difficulty) {
        // The silhouette stays identical for every difficulty; only the central core tint changes.
        return switch (difficulty) {
            case 0 -> Blocks.AMETHYST_BLOCK;
            case 1 -> Blocks.BUDDING_AMETHYST;
            case 2 -> Blocks.PURPLE_STAINED_GLASS;
            default -> Blocks.CRYING_OBSIDIAN;
        };
    }

    public static boolean canBuild(ServerLevel level, BlockPos ground, int difficulty) {
        for (Map.Entry<BlockPos, Block> entry : layout(ground, difficulty).entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) return false;
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).canBeReplaced()) return false;
        }
        return true;
    }

    public static boolean place(ServerLevel level, BlockPos ground, int difficulty) {
        if (!canBuild(level, ground, difficulty)) return false;
        for (Map.Entry<BlockPos, Block> entry : layout(ground, difficulty).entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue().defaultBlockState(), 3);
        }
        // The custom client renderer supplies the actual 3D arch, animated circular vortex and floating crystal.
        DungeonGateVisualEntity visual = CitizenGuildEntities.DUNGEON_GATE_VISUAL.get().create(level);
        if (visual != null) {
            visual.setPos(ground.getX() + 0.5D, ground.getY() + 2.0D, ground.getZ() + 0.5D);
            level.addFreshEntity(visual);
        }
        return true;
    }

    public static void remove(ServerLevel level, BlockPos portalCenter, int difficulty) {
        BlockPos ground = portalCenter.below(2);
        for (Map.Entry<BlockPos, Block> entry : layout(ground, difficulty).entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = level.getBlockState(pos);
            if (state.is(entry.getValue())) level.removeBlock(pos, false);
        }
        for (DungeonGateVisualEntity visual : level.getEntitiesOfClass(DungeonGateVisualEntity.class,
                new AABB(ground).inflate(8.0D))) {
            visual.discard();
        }
    }
}
