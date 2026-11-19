package fr.citizen.guilds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent one-life dungeon session state. */
public final class DungeonSessionData extends SavedData {
    private static final String DATA_NAME = "citizenguilds_dungeon_session";
    private boolean active;
    private String guildId = "";
    private String guildName = "";
    private BlockPos site;
    private final Map<String, String> participants = new HashMap<>();
    private final Set<String> eliminated = new HashSet<>();

    public static DungeonSessionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(DungeonSessionData::new, DungeonSessionData::load), DATA_NAME);
    }

    public static DungeonSessionData load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonSessionData data = new DungeonSessionData();
        data.active = tag.getBoolean("active");
        data.guildId = tag.getString("guildId");
        data.guildName = tag.getString("guildName");
        if (tag.getBoolean("hasSite")) {
            data.site = new BlockPos(tag.getInt("siteX"), tag.getInt("siteY"), tag.getInt("siteZ"));
        }
        ListTag players = tag.getList("participants", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            String uuid = entry.getString("uuid");
            data.participants.put(uuid, entry.getString("name"));
            if (entry.getBoolean("eliminated")) data.eliminated.add(uuid);
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("active", active);
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
        return tag;
    }

    public boolean active() { return active; }
    public String guildId() { return guildId; }
    public String guildName() { return guildName; }
    public BlockPos site() { return site; }
    public int participantCount() { return participants.size(); }
    public int aliveCount() { return participants.size() - eliminated.size(); }
    public boolean isEliminated(String uuid) { return eliminated.contains(uuid); }

    public boolean start(String id, String name, BlockPos pos) {
        if (active || pos == null) return false;
        active = true;
        guildId = id;
        guildName = name;
        site = pos.immutable();
        participants.clear();
        eliminated.clear();
        setDirty();
        return true;
    }

    public boolean join(ServerPlayer player, GuildSavedData guildData) {
        if (!active || site == null || isEliminated(player.getUUID().toString())) return false;
        GuildSavedData.Guild guild = guildData.guildOf(player);
        if (guild == null || !guild.id.equals(guildId)) return false;
        String uuid = player.getUUID().toString();
        participants.putIfAbsent(uuid, player.getGameProfile().getName());
        setDirty();
        return true;
    }

    public boolean eliminate(ServerPlayer player) {
        String uuid = player.getUUID().toString();
        if (!active || !participants.containsKey(uuid) || eliminated.contains(uuid)) return false;
        eliminated.add(uuid);
        setDirty();
        return true;
    }

    public void end() {
        active = false;
        guildId = "";
        guildName = "";
        site = null;
        participants.clear();
        eliminated.clear();
        setDirty();
    }
}
