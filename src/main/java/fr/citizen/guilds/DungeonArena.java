package fr.citizen.guilds;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;

/** Builds the dungeon in its own dimension, never modifying player terrain in the Overworld. */
public final class DungeonArena {
    public static final int FLOOR_Y = 20;
    private static final BlockState FLOOR = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState WALL = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState CEILING = Blocks.DEEPSLATE_TILES.defaultBlockState();

    private DungeonArena() {}

    public static ServerLevel prepare(MinecraftServer server) {
        ServerLevel level = server.getLevel(CitizenDimensions.DUNGEON);
        if (level == null) return null;

        AABB arenaBounds = new AABB(-16, FLOOR_Y - 5, -85, 16, FLOOR_Y + 10, 15);
        level.getEntitiesOfClass(Mob.class, arenaBounds).forEach(Entity -> Entity.discard());
        level.getEntitiesOfClass(ItemEntity.class, arenaBounds).forEach(Entity -> Entity.discard());

        clear(level, -16, 16, -84, 14, FLOOR_Y, FLOOR_Y + 7);
        room(level, -9, 9, -9, 9);
        corridor(level, -3, 3, -23, -10);
        room(level, -11, 11, -44, -24);
        corridor(level, -3, 3, -57, -45);
        room(level, -13, 13, -82, -58);

        // Open safe passages through the room walls; all carving stays inside this dedicated dimension.
        doorway(level, -9);
        doorway(level, -24);
        doorway(level, -44);
        doorway(level, -58);

        // The entrance chamber and return gate.
        level.setBlockAndUpdate(new BlockPos(0, FLOOR_Y + 1, 7), CitizenDungeonBlocks.DUNGEON_GATE.get().defaultBlockState());
        lantern(level, -7, FLOOR_Y + 4, 0);
        lantern(level, 7, FLOOR_Y + 4, 0);
        lantern(level, -9, FLOOR_Y + 4, -34);
        lantern(level, 9, FLOOR_Y + 4, -34);
        lantern(level, -11, FLOOR_Y + 4, -70);
        lantern(level, 11, FLOOR_Y + 4, -70);

        // Marked floor traps in the middle chamber.
        int[][] traps = {{-5,-30},{5,-30},{-5,-36},{5,-36},{0,-40}};
        for (int[] trap : traps) {
            level.setBlockAndUpdate(new BlockPos(trap[0], FLOOR_Y, trap[1]), Blocks.REDSTONE_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(trap[0], FLOOR_Y + 1, trap[1]), Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.defaultBlockState());
        }

        // Decorative pillars make the boss chamber visually distinct.
        for (int x : new int[]{-10, 10}) {
            for (int z : new int[]{-78, -62}) {
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 4; y++) {
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.CRYING_OBSIDIAN.defaultBlockState());
                }
            }
        }
        return level;
    }

    public static BlockPos entrance() { return new BlockPos(0, FLOOR_Y + 1, 3); }
    public static BlockPos waveCenter(int wave) {
        return switch (wave) {
            case 1 -> new BlockPos(0, FLOOR_Y + 1, -2);
            case 2 -> new BlockPos(0, FLOOR_Y + 1, -34);
            default -> new BlockPos(0, FLOOR_Y + 1, -70);
        };
    }

    public static boolean isInsideBounds(BlockPos pos) {
        return pos.getX() >= -13 && pos.getX() <= 13 && pos.getZ() >= -82 && pos.getZ() <= 9
            && pos.getY() >= FLOOR_Y - 2 && pos.getY() <= FLOOR_Y + 8;
    }

    public static boolean isTrap(BlockPos pos) {
        int[][] traps = {{-5,-30},{5,-30},{-5,-36},{5,-36},{0,-40}};
        for (int[] trap : traps) {
            if (Math.abs(pos.getX() - trap[0]) <= 1 && Math.abs(pos.getZ() - trap[1]) <= 1
                && pos.getY() >= FLOOR_Y && pos.getY() <= FLOOR_Y + 1) return true;
        }
        return false;
    }

    private static void doorway(ServerLevel level, int z) {
        for (int x = -2; x <= 2; x++) for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 3; y++)
            level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
    }

    private static void clear(ServerLevel level, int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            for (int y = minY; y <= maxY; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
    }

    private static void room(ServerLevel level, int minX, int maxX, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            level.setBlockAndUpdate(new BlockPos(x, FLOOR_Y, z), FLOOR);
            level.setBlockAndUpdate(new BlockPos(x, FLOOR_Y + 6, z), CEILING);
            boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
            for (int y = FLOOR_Y + 1; y < FLOOR_Y + 6; y++)
                level.setBlockAndUpdate(new BlockPos(x, y, z), edge ? WALL : Blocks.AIR.defaultBlockState());
        }
    }

    private static void corridor(ServerLevel level, int minX, int maxX, int minZ, int maxZ) {
        for (int x = minX - 1; x <= maxX + 1; x++) for (int z = minZ; z <= maxZ; z++) {
            boolean wall = x == minX - 1 || x == maxX + 1;
            level.setBlockAndUpdate(new BlockPos(x, FLOOR_Y, z), FLOOR);
            level.setBlockAndUpdate(new BlockPos(x, FLOOR_Y + 6, z), CEILING);
            for (int y = FLOOR_Y + 1; y < FLOOR_Y + 6; y++)
                level.setBlockAndUpdate(new BlockPos(x, y, z), wall ? WALL : Blocks.AIR.defaultBlockState());
        }
    }

    private static void lantern(ServerLevel level, int x, int y, int z) {
        level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.SOUL_LANTERN.defaultBlockState());
    }
}
