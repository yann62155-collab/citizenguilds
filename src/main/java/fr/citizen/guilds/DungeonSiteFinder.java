package fr.citizen.guilds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Searches a natural surface spot for the portal monument. It never modifies the terrain itself.
 *
 * Search order:
 *  1. around online Overworld players located in (or near) the allowed dungeon zone (already loaded chunks);
 *  2. if nobody is there, a few random spots inside the zone whose chunks already exist on disk
 *     (the chunks are loaded, never generated), so the portal can appear even when nobody is nearby.
 */
public final class DungeonSiteFinder {
    /** Width of the random area explored past the minimum coordinates when nobody is in the zone. */
    private static final int REMOTE_SPAN = 1500;
    private static final double PLAYER_DISTANCE = 40.0D;

    private DungeonSiteFinder() {}

    public record Search(BlockPos site, String note) {}

    private record Candidate(BlockPos pos, int score) {}

    /**
     * Random search over the configured map zone. Player positions are never used
     * as search origins; only the minimum X/Y/Z limits and existing world terrain matter.
     */
    public static Search findAuto(ServerLevel level, DungeonData data) {
        RandomSource random = level.getRandom();
        Candidate best = null;
        int existingChunks = 0;
        final int mapAttempts = 24;

        for (int attempt = 0; attempt < mapAttempts; attempt++) {
            int x = data.minX() + 32 + random.nextInt(REMOTE_SPAN);
            int z = data.minZ() + 32 + random.nextInt(REMOTE_SPAN);
            BlockPos probe = new BlockPos(x, level.getMinBuildHeight(), z);
            if (!level.getWorldBorder().isWithinBounds(probe)) continue;

            ChunkPos center = new ChunkPos(probe);
            if (!chunkExistsOnDisk(level, center)) continue;
            existingChunks++;

            // Load only chunks already present on disk; never generate a new region just to place a portal.
            for (int cx = -1; cx <= 1; cx++) {
                for (int cz = -1; cz <= 1; cz++) {
                    ChunkPos around = new ChunkPos(center.x + cx, center.z + cz);
                    if (chunkExistsOnDisk(level, around)) level.getChunk(around.x, around.z);
                }
            }

            Candidate candidate = searchAround(level, data, center.getMiddleBlockX(),
                center.getMiddleBlockZ(), 0, 112, 180, null);
            best = better(best, candidate);
            if (best != null && best.score >= 3) {
                return new Search(best.pos, "Site aleatoire trouve dans la zone configuree, independamment des joueurs.");
            }
        }

        if (best != null) return new Search(best.pos,
            "Site aleatoire trouve dans la zone configuree, independamment des joueurs.");
        if (existingChunks == 0) {
            return new Search(null, "Aucun chunk deja genere trouve dans la zone X>=" + data.minX()
                + ", Z>=" + data.minZ() + ". Explore ou genere cette zone avant le prochain essai.");
        }
        return new Search(null, "Aucun terrain naturel assez plat et eloigne des joueurs trouve dans la zone configuree. Nouvel essai bientot.");
    }

    /** Manual scan around an administrator. */
    public static Search findNear(ServerLevel level, ServerPlayer requester, DungeonData data) {
        Candidate best = searchAround(level, data, requester.getBlockX(), requester.getBlockZ(), 8, 128, 500, requester);
        if (best == null) return new Search(null, "Aucun site naturel adapte trouve autour de toi.");
        return new Search(best.pos, "Site trouve.");
    }

    private static Candidate searchAround(ServerLevel level, DungeonData data, int cx, int cz, int minRadius, int maxRadius,
                                          int attempts, ServerPlayer ignored) {
        RandomSource random = level.getRandom();
        Candidate best = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double dist = minRadius + random.nextDouble() * Math.max(1, maxRadius - minRadius);
            int x = cx + (int) Math.round(Math.cos(angle) * dist);
            int z = cz + (int) Math.round(Math.sin(angle) * dist);
            if (!level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!isSafeSite(level, feet, data.minX(), data.minY(), data.minZ(), ignored)) continue;
            if (!DungeonPortalStructure.canBuild(level, feet, 0)) continue;
            Candidate candidate = new Candidate(feet, shelterScore(level, x, z, y));
            best = better(best, candidate);
            if (candidate.score >= 3) return candidate;
        }
        return best;
    }

    private static Candidate better(Candidate a, Candidate b) {
        if (a == null) return b;
        if (b == null) return a;
        return b.score > a.score ? b : a;
    }

    public static boolean isSafeSite(ServerLevel level, BlockPos feet, int minX, int minY, int minZ) {
        return isSafeSite(level, feet, minX, minY, minZ, null);
    }

    public static boolean isSafeSite(ServerLevel level, BlockPos feet, int minX, int minY, int minZ, ServerPlayer ignoredPlayer) {
        int x = feet.getX();
        int z = feet.getZ();
        int y = feet.getY();
        if (x < minX || z < minZ || y < minY || y >= level.getMaxBuildHeight() - 8) return false;
        if (!level.getWorldBorder().isWithinBounds(feet)) return false;
        if (!level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z))) return false;
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) != y) return false;
        if (!isOpen(level.getBlockState(feet)) || !isOpen(level.getBlockState(feet.above()))) return false;
        if (!isNaturalGround(level.getBlockState(feet.below()))) return false;
        if (!flatNaturalArea(level, x, y, z)) return false;
        if (nearAnyPlayer(level, feet, PLAYER_DISTANCE, ignoredPlayer)) return false;
        return !DungeonProtection.isProtectedArea(level, feet);
    }

    /** Air, short grass, flowers, snow layer... but never water or lava. */
    private static boolean isOpen(BlockState state) {
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    /** The 5 blocks under the monument must be level, the 7x7 area around it natural and roughly flat. */
    private static boolean flatNaturalArea(ServerLevel level, int x, int y, int z) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (!level.hasChunkAt(new BlockPos(x + dx, level.getMinBuildHeight(), z + dz))) return false;
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                boolean underMonument = dz == 0 && Math.abs(dx) <= 2;
                if (underMonument ? h != y : Math.abs(h - y) > 2) return false;
                BlockState ground = level.getBlockState(new BlockPos(x + dx, h - 1, z + dz));
                if (!isNaturalGround(ground)) return false;
            }
        }
        return true;
    }

    /** Number of directions where the terrain rises: a hollow between hills scores higher (hidden entrance). */
    private static int shelterScore(ServerLevel level, int x, int z, int centerY) {
        int higherSides = 0;
        int[][] directions = {{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : directions) {
            int sx = x + d[0] * 12;
            int sz = z + d[1] * 12;
            if (!level.hasChunkAt(new BlockPos(sx, level.getMinBuildHeight(), sz))) continue;
            if (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz) >= centerY + 4) higherSides++;
        }
        return higherSides;
    }

    private static boolean nearAnyPlayer(ServerLevel level, BlockPos pos, double radius, ServerPlayer ignoredPlayer) {
        double limit = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player == ignoredPlayer || player.isSpectator()) continue;
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < limit) return true;
        }
        return false;
    }

    /** Reads the region file only: a chunk that was never generated is skipped instead of being created. */
    private static boolean chunkExistsOnDisk(ServerLevel level, ChunkPos pos) {
        if (level.hasChunk(pos.x, pos.z)) return true;
        try {
            Optional<CompoundTag> tag = level.getChunkSource().chunkMap.read(pos).join();
            if (tag.isEmpty()) return false;
            String status = tag.get().getString("Status");
            return status.isEmpty() || status.endsWith("full");
        } catch (Throwable error) {
            return false;
        }
    }

    private static boolean isNaturalGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD)
            || state.is(BlockTags.TERRACOTTA) || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK)
            || state.is(Blocks.CLAY) || state.is(Blocks.CALCITE) || state.is(Blocks.DIRT_PATH)
            || state.is(Blocks.PACKED_ICE) || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE);
    }
}
