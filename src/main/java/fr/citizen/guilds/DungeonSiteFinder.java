package fr.citizen.guilds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/** Conservative surface-site search. It never places or removes blocks. */
public final class DungeonSiteFinder {
    private DungeonSiteFinder() {}

    public static BlockPos find(ServerLevel level, ServerPlayer requester, int minX, int minY, int minZ) {
        int centerX = requester.blockPosition().getX();
        int centerZ = requester.blockPosition().getZ();
        for (int attempt = 0; attempt < 600; attempt++) {
            int x = centerX + level.getRandom().nextInt(257) - 128;
            int z = centerZ + level.getRandom().nextInt(257) - 128;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (isSafeSite(level, feet, minX, minY, minZ)) return feet;
        }
        return null;
    }

    public static boolean isSafeSite(ServerLevel level, BlockPos feet, int minX, int minY, int minZ) {
        int x = feet.getX();
        int z = feet.getZ();
        int y = feet.getY();
        if (x < minX || z < minZ || y < minY || y >= level.getMaxBuildHeight() - 2) return false;
        if (!level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z))) return false;
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) != y) return false;
        if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) return false;
        if (!isNaturalGround(level.getBlockState(feet.below()).getBlock())) return false;
        if (!naturalArea(level, x, z) || !mountainShelter(level, x, z, y)) return false;
        if (nearAnyPlayer(level, feet, 64.0D)) return false;
        return !DungeonProtection.isProtectedArea(level, feet);
    }

    private static boolean naturalArea(ServerLevel level, int x, int z) {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (!level.hasChunkAt(new BlockPos(x + dx, level.getMinBuildHeight(), z + dz))) return false;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return false;
                if (!isNaturalGround(level.getBlockState(new BlockPos(x + dx, y - 1, z + dz)).getBlock())) return false;
            }
        }
        return true;
    }

    /** Prefer a natural hollow/valley with higher terrain around it, suitable for a hidden mountain entrance. */
    private static boolean mountainShelter(ServerLevel level, int x, int z, int centerY) {
        int higherSides = 0;
        int[][] directions = {{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : directions) {
            int sampleX = x + d[0] * 12;
            int sampleZ = z + d[1] * 12;
            if (!level.hasChunkAt(new BlockPos(sampleX, level.getMinBuildHeight(), sampleZ))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + d[0] * 12, z + d[1] * 12);
            if (y >= centerY + 4) higherSides++;
        }
        return higherSides >= 2;
    }

    private static boolean nearAnyPlayer(ServerLevel level, BlockPos pos, double radius) {
        double limit = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < limit) return true;
        }
        return false;
    }

    private static boolean isNaturalGround(Block block) {
        return block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.COARSE_DIRT
            || block == Blocks.PODZOL || block == Blocks.MYCELIUM || block == Blocks.STONE
            || block == Blocks.ANDESITE || block == Blocks.DIORITE || block == Blocks.GRANITE
            || block == Blocks.DEEPSLATE || block == Blocks.TUFF || block == Blocks.CALCITE
            || block == Blocks.SAND || block == Blocks.RED_SAND || block == Blocks.GRAVEL
            || block == Blocks.SNOW_BLOCK || block == Blocks.MOSS_BLOCK;
    }
}
