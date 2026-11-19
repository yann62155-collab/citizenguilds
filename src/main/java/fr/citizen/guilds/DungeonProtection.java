package fr.citizen.guilds;

import java.lang.reflect.Method;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Optional WorldGuard integration through reflection, so the mod has no hard plugin dependency. */
public final class DungeonProtection {
    private DungeonProtection() {}

    public static boolean isProtectedArea(ServerLevel level, BlockPos center) {
        int[][] samples = {
            {0, 0}, {-5, -5}, {-5, 5}, {5, -5}, {5, 5},
            {0, -5}, {0, 5}, {-5, 0}, {5, 0}
        };
        for (int[] sample : samples) {
            if (isProtected(level, center.getX() + sample[0], center.getY(), center.getZ() + sample[1])) return true;
        }
        return false;
    }

    private static boolean isProtected(ServerLevel level, int x, int y, int z) {
        try {
            Class<?> bukkitWorldType = Class.forName("org.bukkit.World");
            Class<?> bukkitLocationType = Class.forName("org.bukkit.Location");
            Object bukkitWorld = level.getClass().getMethod("getWorld").invoke(level);
            if (bukkitWorld == null || !bukkitWorldType.isInstance(bukkitWorld)) return false;
            Object bukkitLocation = bukkitLocationType
                .getConstructor(bukkitWorldType, double.class, double.class, double.class)
                .newInstance(bukkitWorld, x + 0.5D, y, z + 0.5D);

            Class<?> adapterType = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object worldEditLocation = invokeStaticAdapter(adapterType, bukkitLocationType, bukkitLocation);
            Class<?> worldGuardType = Class.forName("com.sk89q.worldguard.WorldGuard");
            Object worldGuard = worldGuardType.getMethod("getInstance").invoke(null);
            Object platform = worldGuardType.getMethod("getPlatform").invoke(worldGuard);
            Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
            Object query = container.getClass().getMethod("createQuery").invoke(container);
            Method applicableRegions = findCompatibleMethod(query.getClass(), "getApplicableRegions", worldEditLocation);
            Object regionSet = applicableRegions.invoke(query, worldEditLocation);
            Object result = regionSet.getClass().getMethod("getRegions").invoke(regionSet);
            if (!(result instanceof Collection<?> regions)) return true;
            for (Object region : regions) {
                String id = String.valueOf(region.getClass().getMethod("getId").invoke(region));
                if (!"__global__".equalsIgnoreCase(id)) return true;
            }
            return false;
        } catch (ClassNotFoundException absentPlugin) {
            return false;
        } catch (Throwable incompatiblePluginApi) {
            // Fail closed if WorldGuard is present but its protection state cannot be checked.
            try {
                Class.forName("com.sk89q.worldguard.WorldGuard");
                return true;
            } catch (ClassNotFoundException absentPlugin) {
                return false;
            }
        }
    }

    private static Object invokeStaticAdapter(Class<?> adapterType, Class<?> locationType, Object location) throws Exception {
        for (Method method : adapterType.getMethods()) {
            if (method.getName().equals("adapt") && method.getParameterCount() == 1
                && method.getParameterTypes()[0].equals(locationType)) {
                return method.invoke(null, location);
            }
        }
        throw new NoSuchMethodException("BukkitAdapter.adapt(Location)");
    }

    private static Method findCompatibleMethod(Class<?> type, String name, Object argument) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1
                && method.getParameterTypes()[0].isInstance(argument)) return method;
        }
        throw new NoSuchMethodException(name);
    }
}
