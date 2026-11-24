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
    private int rewardJade = 3;
    private boolean autoSpawnEnabled = true;
    private int spawnIntervalMinutes = 30;
    private long portalSpawnGameTime;
    private BlockPos lastSite = null;
    private long nextSpawnGameTime;
    private String lastSearchNote = "";
    private BlockPos riftPosition = null;
    private int riftLevel = 0;
    private long nextRiftCheckGameTime = 0L;
    private int fractureDeformationPercent = 65;
    private long fractureStartedAt = 0L;
    private int riftDifficulty = 0;

    public static DungeonData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(DungeonData::new, DungeonData::load), DATA_NAME);
    }

    public static DungeonData load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonData data = new DungeonData();
        data.minX = tag.contains("minX") ? tag.getInt("minX") : 2381;
        data.minY = tag.contains("minY") ? tag.getInt("minY") : 68;
        data.minZ = tag.contains("minZ") ? tag.getInt("minZ") : 75;
        data.rewardJade = tag.contains("rewardJade") ? Math.max(0, Math.min(64, tag.getInt("rewardJade"))) : 3;
        data.autoSpawnEnabled = !tag.contains("autoSpawnEnabled") || tag.getBoolean("autoSpawnEnabled");
        data.spawnIntervalMinutes = tag.contains("spawnIntervalMinutes") ? Math.max(5, Math.min(1440, tag.getInt("spawnIntervalMinutes"))) : 30;
        data.portalSpawnGameTime = tag.getLong("portalSpawnGameTime");
        data.nextSpawnGameTime = tag.getLong("nextSpawnGameTime");
        data.lastSearchNote = tag.getString("lastSearchNote");
        if (tag.getBoolean("hasRift")) data.riftPosition = new BlockPos(tag.getInt("riftX"), tag.getInt("riftY"), tag.getInt("riftZ"));
        data.riftLevel = Math.max(0, Math.min(6, tag.getInt("riftLevel")));
        data.nextRiftCheckGameTime = tag.getLong("nextRiftCheckGameTime");
        data.fractureDeformationPercent = Math.max(0, Math.min(100, tag.contains("fractureDeformationPercent") ? tag.getInt("fractureDeformationPercent") : 65));
        data.fractureStartedAt = tag.getLong("fractureStartedAt");
        data.riftDifficulty = Math.max(0, Math.min(3, tag.getInt("riftDifficulty")));
        if (tag.getBoolean("hasSite")) data.lastSite = new BlockPos(tag.getInt("siteX"), tag.getInt("siteY"), tag.getInt("siteZ"));
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("minX", minX);
        tag.putInt("minY", minY);
        tag.putInt("minZ", minZ);
        tag.putInt("rewardJade", rewardJade);
        tag.putBoolean("autoSpawnEnabled", autoSpawnEnabled);
        tag.putInt("spawnIntervalMinutes", spawnIntervalMinutes);
        tag.putLong("portalSpawnGameTime", portalSpawnGameTime);
        tag.putLong("nextSpawnGameTime", nextSpawnGameTime);
        tag.putString("lastSearchNote", lastSearchNote == null ? "" : lastSearchNote);
        tag.putBoolean("hasRift", riftPosition != null);
        if (riftPosition != null) { tag.putInt("riftX", riftPosition.getX()); tag.putInt("riftY", riftPosition.getY()); tag.putInt("riftZ", riftPosition.getZ()); }
        tag.putInt("riftLevel", riftLevel);
        tag.putLong("nextRiftCheckGameTime", nextRiftCheckGameTime);
        tag.putInt("fractureDeformationPercent", fractureDeformationPercent);
        tag.putLong("fractureStartedAt", fractureStartedAt);
        tag.putInt("riftDifficulty", riftDifficulty);
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
    public int rewardJade() { return rewardJade; }
    public boolean autoSpawnEnabled() { return autoSpawnEnabled; }
    public int spawnIntervalMinutes() { return spawnIntervalMinutes; }
    public long portalSpawnGameTime() { return portalSpawnGameTime; }
    public BlockPos lastSite() { return lastSite; }
    public BlockPos riftPosition() { return riftPosition; }
    public int riftLevel() { return riftLevel; }
    public long nextRiftCheckGameTime() { return nextRiftCheckGameTime; }
    public int fractureDeformationPercent() { return fractureDeformationPercent; }
    public long fractureStartedAt() { return fractureStartedAt; }
    public int riftDifficulty() { return riftDifficulty; }
    public void setRiftDifficulty(int difficulty) { riftDifficulty = Math.max(0, Math.min(3, difficulty)); setDirty(); }
    public void setFractureDeformationPercent(int percent) { fractureDeformationPercent = Math.max(0, Math.min(100, percent)); setDirty(); }
    public void setFractureStartedAt(long time) { fractureStartedAt = time; setDirty(); }
    public void setRiftPosition(BlockPos pos) { riftPosition = pos == null ? null : pos.immutable(); riftLevel = 0; nextRiftCheckGameTime = 0L; setDirty(); }
    public void setRiftLevel(int level) { riftLevel = Math.max(0, Math.min(6, level)); setDirty(); }
    public void setNextRiftCheckGameTime(long time) { nextRiftCheckGameTime = time; setDirty(); }

    public void setPortalSpawnGameTime(long gameTime) { portalSpawnGameTime = gameTime; setDirty(); }
    public long nextSpawnGameTime() { return nextSpawnGameTime; }
    public void setNextSpawnGameTime(long gameTime) { nextSpawnGameTime = gameTime; setDirty(); }
    public String lastSearchNote() { return lastSearchNote == null ? "" : lastSearchNote; }
    public void setLastSearchNote(String note) {
        String clean = note == null ? "" : note;
        if (!clean.equals(lastSearchNote)) { lastSearchNote = clean; setDirty(); }
    }
    /** True when the tracked site is a real placed portal and not only a scanned candidate. */
    public boolean hasPlacedPortal() { return lastSite != null && portalSpawnGameTime > 0L; }

    public void setAutoSpawnEnabled(boolean enabled) { autoSpawnEnabled = enabled; setDirty(); }
    public void setSpawnIntervalMinutes(int minutes) { spawnIntervalMinutes = Math.max(5, Math.min(1440, minutes)); setDirty(); }

    public void setRewardJade(int amount) {
        rewardJade = Math.max(0, Math.min(64, amount));
        setDirty();
    }

    public void setMinimum(int x, int y, int z) {
        minX = x; minY = y; minZ = z; setDirty();
    }

    public void setLastSite(BlockPos pos) {
        lastSite = pos == null ? null : pos.immutable();
        if (pos == null) portalSpawnGameTime = 0L;
        setDirty();
    }

    public void clearLastSite() {
        lastSite = null;
        portalSpawnGameTime = 0L;
        setDirty();
    }
}
