package fr.citizen.guilds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent dungeon boundary and last verified candidate. */
public final class DungeonData extends SavedData {
    private static final String DATA_NAME = "citizenguilds_dungeons";
    private int minX = 2381;
    private int minY = 68;
    private int minZ = 75;
    private BlockPos lastSite = null;

    public static DungeonData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(DungeonData::new, DungeonData::load), DATA_NAME);
    }

    public static DungeonData load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonData data = new DungeonData();
        data.minX = tag.contains("minX") ? tag.getInt("minX") : 2381;
        data.minY = tag.contains("minY") ? tag.getInt("minY") : 68;
        data.minZ = tag.contains("minZ") ? tag.getInt("minZ") : 75;
        if (tag.getBoolean("hasSite")) data.lastSite = new BlockPos(tag.getInt("siteX"), tag.getInt("siteY"), tag.getInt("siteZ"));
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("minX", minX);
        tag.putInt("minY", minY);
        tag.putInt("minZ", minZ);
        tag.putBoolean("hasSite", lastSite != null);
        if (lastSite != null) {
            tag.putInt("siteX", lastSite.getX());
            tag.putInt("siteY", lastSite.getY());
            tag.putInt("siteZ", lastSite.getZ());
        }
        return tag;
    }

    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public BlockPos lastSite() { return lastSite; }

    public void setMinimum(int x, int y, int z) {
        minX = x; minY = y; minZ = z; setDirty();
    }

    public void setLastSite(BlockPos pos) {
        lastSite = pos == null ? null : pos.immutable();
        setDirty();
    }
}
