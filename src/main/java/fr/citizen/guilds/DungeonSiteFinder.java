package fr.citizen.guilds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
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
import net.minecraft.world.level.storage.LevelResource;

/**
 * Searches a natural surface spot for the portal monument. It never modifies the terrain itself.
 *
 * Search order:
 *  1. around online Overworld players located in (or near) the allowed dungeon zone (already loaded chunks);
 *  2. if nobody is there, a few random spots inside the zone whose chunks already exist on disk
 *     (the chunks are loaded, never generated), so the portal can appear even when nobody is nearby.
 */
public final class DungeonSiteFinder {
    private static final double PLAYER_DISTANCE = 40.0D;

    private DungeonSiteFinder() {}

    public record Search(BlockPos site, String note) {}

    private record Candidate(BlockPos pos, int score) {}

    /**
     * Search generated region files first, then sample existing chunks from them.
     * This avoids random misses across the enormous world border and never uses
     * an administrator/player position as the search origin.
     */
    public static Search findAuto(ServerLevel level, DungeonData data) {
        RandomSource random = level.getRandom();
        List<ChunkPos> generated = listGeneratedChunks(level, data);
        if (generated.isEmpty()) {
            return new Search(null, "Aucun chunk deja genere trouve au-dela de X>=" + data.minX()
                + ", Z>=" + data.minZ() + ". Explore la carte dans cette zone avant le prochain essai.");
        }
        Collections.shuffle(generated, new java.util.Random(random.nextLong()));
        Candidate best = null;
        int tested = 0;
        for (ChunkPos center : generated) {
            if (tested++ >= 256) break;
            // Load only the already-generated 3x3 neighbourhood. Missing chunks are skipped.
            boolean footprintReady = true;
            for (int cx = center.x - 1; cx <= center.x + 1 && footprintReady; cx++) {
                for (int cz = center.z - 1; cz <= center.z + 1; cz++) {
                    ChunkPos neighbour = new ChunkPos(cx, cz);
                    if (!chunkExistsOnDisk(level, neighbour)) {
                        footprintReady = false;
                        break;
                    }
                    if (!level.hasChunk(cx, cz)) level.getChunk(cx, cz);
                }
            }
            if (!footprintReady) continue;

            Candidate candidate = searchAround(level, data, center.getMiddleBlockX(),
                center.getMiddleBlockZ(), 0, 72, 160, null);
            best = better(best, candidate);
            if (candidate != null && candidate.score >= 3) {
                return new Search(candidate.pos,
                    "Site choisi aleatoirement parmi les chunks deja generes, sans utiliser la position des joueurs.");
            }
        }
        if (best != null) {
            return new Search(best.pos,
                "Site choisi aleatoirement parmi les chunks deja generes, sans utiliser la position des joueurs.");
        }
        return new Search(null, "Chunks generes trouves, mais aucun terrain naturel assez plat, libre et a plus de "
            + (int)PLAYER_DISTANCE + " blocs des joueurs. La recherche sera relancee.");
    }

    /**
     * Enumerate existing Overworld region files and randomly sample their chunk
     * coordinates. Region files can contain empty slots; chunkExistsOnDisk filters
     * those without causing generation.
     */
    private static List<ChunkPos> listGeneratedChunks(ServerLevel level, DungeonData data) {
        List<ChunkPos> result = new ArrayList<>();
        Path regionDir = level.getServer().getWorldPath(LevelResource.ROOT).resolve("region");
        if (!Files.isDirectory(regionDir)) return result;
        int minRegionX = Math.floorDiv(data.minX(), 512);
        int minRegionZ = Math.floorDiv(data.minZ(), 512);
        int maxRegionX = Math.floorDiv((int)Math.floor(level.getWorldBorder().getMaxX()), 512);
        int maxRegionZ = Math.floorDiv((int)Math.floor(level.getWorldBorder().getMaxZ()), 512);
        try (Stream<Path> files = Files.list(regionDir)) {
            files.filter(path -> path.getFileName().toString().matches("r.-?\\d+.-?\\d+\\.mca"))
                .forEach(path -> {
                    String[] parts = path.getFileName().toString().split("\\.");
                    if (parts.length != 4) return;
                    try {
                        int rx = Integer.parseInt(parts[1]);
                        int rz = Integer.parseInt(parts[2]);
                        if (rx < minRegionX || rz < minRegionZ || rx > maxRegionX || rz > maxRegionZ) return;
                        int baseX = rx * 32;
                        int baseZ = rz * 32;
                        // Read the region header (4 KiB): non-zero location entries are generated chunk slots.
                        // This avoids issuing 1024 asynchronous NBT reads for every region file.
                        List<Integer> slots = new ArrayList<>(128);
                        try (java.io.RandomAccessFile region = new java.io.RandomAccessFile(path.toFile(), "r")) {
                            if (region.length() < 4096L) return;
                            for (int slot = 0; slot < 1024; slot++) {
                                region.seek(slot * 4L);
                                if (region.readInt() != 0) slots.add(slot);
                            }
                        }
                        Collections.shuffle(slots, new java.util.Random(level.getRandom().nextLong()));
                        int foundInRegion = 0;
                        for (int slot : slots) {
                            int cx = baseX + (slot & 31);
                            int cz = baseZ + (slot >>> 5);
                            int blockX = cx * 16 + 8;
                            int blockZ = cz * 16 + 8;
                            if (blockX < data.minX() + 8 || blockZ < data.minZ() + 8) continue;
                            if (blockX < level.getWorldBorder().getMinX() + 32
                                || blockX > level.getWorldBorder().getMaxX() - 32
                                || blockZ < level.getWorldBorder().getMinZ() + 32
                                || blockZ > level.getWorldBorder().getMaxZ() - 32) continue;
                            ChunkPos pos = new ChunkPos(cx, cz);
                            if (chunkExistsOnDisk(level, pos)) {
                                result.add(pos);
                                if (++foundInRegion >= 24) break;
                            }
                        }
                    } catch (NumberFormatException | java.io.IOException ignored) {
                        // Ignore malformed region file names or unreadable region headers.
                    }
                });
        } catch (Exception ignored) {
            return result;
        }
        return result;
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

    /**
     * The portal's central 3-wide footing must be level. Its surrounding 5x5
     * clearance may follow gentle terrain (up to 3 blocks of relief), avoiding
     * the old overly strict 7x7 perfectly-natural plateau requirement.
     */
    private static boolean flatNaturalArea(ServerLevel level, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos column = new BlockPos(x + dx, level.getMinBuildHeight(), z + dz);
                if (!level.hasChunkAt(column)) return false;
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                boolean footing = dz == 0 && Math.abs(dx) <= 1;
                if (footing ? h != y : Math.abs(h - y) > 3) return false;
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
