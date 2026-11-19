package fr.citizen.guilds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent one-life dungeon session and wave progression. */
public final class DungeonSessionData extends SavedData {
    private static final String DATA_NAME = "citizenguilds_dungeon_session";
    private boolean active;
    private boolean finished;
    private boolean victory;
    private boolean wavePending;
    private int wave;
    private String guildId = "";
    private String guildName = "";
    private BlockPos site;
    private final Map<String, String> participants = new HashMap<>();
    private final Set<String> eliminated = new HashSet<>();
    private final List<String> activeMobIds = new ArrayList<>();

    public static DungeonSessionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(DungeonSessionData::new, DungeonSessionData::load), DATA_NAME);
    }

    public static DungeonSessionData load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonSessionData data = new DungeonSessionData();
        data.active = tag.getBoolean("active");
        data.finished = tag.getBoolean("finished");
        data.victory = tag.getBoolean("victory");
        data.wavePending = tag.getBoolean("wavePending");
        data.wave = tag.getInt("wave");
        data.guildId = tag.getString("guildId");
        data.guildName = tag.getString("guildName");
        if (tag.getBoolean("hasSite")) data.site = new BlockPos(tag.getInt("siteX"), tag.getInt("siteY"), tag.getInt("siteZ"));
        ListTag players = tag.getList("participants", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            String uuid = entry.getString("uuid");
            data.participants.put(uuid, entry.getString("name"));
            if (entry.getBoolean("eliminated")) data.eliminated.add(uuid);
        }
        ListTag mobs = tag.getList("activeMobs", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < mobs.size(); i++) data.activeMobIds.add(mobs.getCompound(i).getString("uuid"));
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("active", active);
        tag.putBoolean("finished", finished);
        tag.putBoolean("victory", victory);
        tag.putBoolean("wavePending", wavePending);
        tag.putInt("wave", wave);
        tag.putString("guildId", guildId);
        tag.putString("guildName", guildName);
        tag.putBoolean("hasSite", site != null);
        if (site != null) {
            tag.putInt("siteX", site.getX());
            tag.putInt("siteY", site.getY());
            tag.putInt("siteZ", site.getZ());
        }
        ListTag players = new ListTag();
        participants.forEach((uuid, name) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("uuid", uuid);
            entry.putString("name", name);
            entry.putBoolean("eliminated", eliminated.contains(uuid));
            players.add(entry);
        });
        tag.put("participants", players);
        ListTag mobs = new ListTag();
        for (String uuid : activeMobIds) {
            CompoundTag entry = new CompoundTag();
            entry.putString("uuid", uuid);
            mobs.add(entry);
        }
        tag.put("activeMobs", mobs);
        return tag;
    }

    public boolean active() { return active; }
    public boolean finished() { return finished; }
    public boolean victory() { return victory; }
    public boolean wavePending() { return wavePending; }
    public int wave() { return wave; }
    public String guildId() { return guildId; }
    public String guildName() { return guildName; }
    public BlockPos site() { return site; }
    public int participantCount() { return participants.size(); }
    public int aliveCount() { return participants.size() - eliminated.size(); }
    public boolean isEliminated(String uuid) { return eliminated.contains(uuid); }
    public boolean isParticipant(String uuid) { return participants.containsKey(uuid); }

    public boolean start(String id, String name, BlockPos pos) {
        if (active || pos == null) return false;
        active = true;
        finished = false;
        victory = false;
        wavePending = false;
        wave = 0;
        guildId = id;
        guildName = name;
        site = pos.immutable();
        participants.clear();
        eliminated.clear();
        activeMobIds.clear();
        setDirty();
        return true;
    }

    public boolean join(ServerPlayer player, GuildSavedData guildData) {
        if (!active || finished || site == null || isEliminated(player.getUUID().toString())) return false;
        GuildSavedData.Guild guild = guildData.guildOf(player);
        if (guild == null || !guild.id.equals(guildId)) return false;
        String uuid = player.getUUID().toString();
        boolean added = !participants.containsKey(uuid);
        participants.putIfAbsent(uuid, player.getGameProfile().getName());
        if (wave == 0 && !wavePending) wavePending = true;
        if (added) setDirty();
        return true;
    }

    public boolean eliminate(ServerPlayer player) {
        String uuid = player.getUUID().toString();
        if (!active || finished || !participants.containsKey(uuid) || eliminated.contains(uuid)) return false;
        eliminated.add(uuid);
        setDirty();
        return true;
    }

    public void nextWave() {
        wave++;
        wavePending = true;
        activeMobIds.clear();
        setDirty();
    }

    public void setWaveMobs(List<String> ids) {
        activeMobIds.clear();
        activeMobIds.addAll(ids);
        wavePending = false;
        setDirty();
    }

    public int pruneMobIds(ServerLevel level) {
        activeMobIds.removeIf(id -> {
            try {
                Entity entity = level.getEntity(UUID.fromString(id));
                return entity == null || !entity.isAlive();
            } catch (IllegalArgumentException ignored) {
                return true;
            }
        });
        setDirty();
        return activeMobIds.size();
    }

    public void finish(boolean won) {
        finished = true;
        victory = won;
        wavePending = false;
        activeMobIds.clear();
        setDirty();
    }

    public Set<String> participantIds() { return new HashSet<>(participants.keySet()); }

    public Set<String> livingParticipantIds() {
        Set<String> living = new HashSet<>(participants.keySet());
        living.removeAll(eliminated);
        return living;
    }

    public void removeActiveMobs(ServerLevel level) {
        for (String id : activeMobIds) {
            try {
                Entity entity = level.getEntity(UUID.fromString(id));
                if (entity != null) entity.discard();
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupted UUID entries in an old session save.
            }
        }
        activeMobIds.clear();
        setDirty();
    }

    public void end() {
        active = false;
        finished = false;
        victory = false;
        wavePending = false;
        wave = 0;
        guildId = "";
        guildName = "";
        site = null;
        participants.clear();
        eliminated.clear();
        activeMobIds.clear();
        setDirty();
    }
}
