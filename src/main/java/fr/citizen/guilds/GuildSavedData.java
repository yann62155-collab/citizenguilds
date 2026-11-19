package fr.citizen.guilds;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent server-side storage for Citizen Guilds. */
public final class GuildSavedData extends SavedData {
    private static final String DATA_NAME = "citizenguilds_data";
    private final Map<String, Guild> guilds = new LinkedHashMap<>();
    private final Map<String, String> membership = new HashMap<>();
    private final Map<String, String> invitations = new HashMap<>();

    public static GuildSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(GuildSavedData::new, GuildSavedData::load), DATA_NAME);
    }

    public static GuildSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GuildSavedData data = new GuildSavedData();
        ListTag list = tag.getList("guilds", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Guild guild = Guild.load(entry, registries);
            data.guilds.put(guild.id, guild);
            for (String uuid : guild.members.keySet()) data.membership.put(uuid, guild.id);
        }
        ListTag invites = tag.getList("invitations", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < invites.size(); i++) {
            CompoundTag invite = invites.getCompound(i);
            data.invitations.put(invite.getString("player"), invite.getString("guild"));
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Guild guild : guilds.values()) list.add(guild.save(registries));
        tag.put("guilds", list);
        ListTag invites = new ListTag();
        invitations.forEach((player, guild) -> {
            CompoundTag invite = new CompoundTag();
            invite.putString("player", player);
            invite.putString("guild", guild);
            invites.add(invite);
        });
        tag.put("invitations", invites);
        return tag;
    }

    public Guild guildOf(ServerPlayer player) {
        String id = membership.get(player.getUUID().toString());
        return id == null ? null : guilds.get(id);
    }
    public Guild guild(String id) { return guilds.get(id); }
    public Collection<Guild> allGuilds() { return Collections.unmodifiableCollection(guilds.values()); }

    public Guild create(ServerPlayer leader, String name) {
        String uuid = leader.getUUID().toString();
        if (membership.containsKey(uuid)) return null;
        String id = UUID.randomUUID().toString();
        Guild guild = new Guild(id, name, uuid, leader.getGameProfile().getName());
        guilds.put(id, guild);
        membership.put(uuid, id);
        guild.log("La guilde " + name + " a ete fondee par " + leader.getGameProfile().getName());
        setDirty();
        return guild;
    }
    public boolean invite(ServerPlayer inviter, ServerPlayer target) {
        Guild guild = guildOf(inviter);
        if (guild == null || !guild.canManage(inviter.getUUID().toString())) return false;
        if (membership.containsKey(target.getUUID().toString())) return false;
        invitations.put(target.getUUID().toString(), guild.id);
        guild.log(inviter.getGameProfile().getName() + " a invite " + target.getGameProfile().getName());
        setDirty();
        return true;
    }
    public boolean accept(ServerPlayer player) {
        String uuid = player.getUUID().toString();
        String id = invitations.remove(uuid);
        Guild guild = id == null ? null : guilds.get(id);
        if (guild == null || membership.containsKey(uuid)) { setDirty(); return false; }
        guild.members.put(uuid, player.getGameProfile().getName());
        guild.roles.put(uuid, "MEMBER");
        membership.put(uuid, id);
        guild.log(player.getGameProfile().getName() + " a rejoint la guilde");
        setDirty();
        return true;
    }
    public boolean leave(ServerPlayer player) {
        Guild guild = guildOf(player);
        if (guild == null || guild.leader.equals(player.getUUID().toString())) return false;
        String uuid = player.getUUID().toString();
        guild.members.remove(uuid); guild.roles.remove(uuid); membership.remove(uuid);
        guild.log(player.getGameProfile().getName() + " a quitte la guilde");
        setDirty(); return true;
    }
    public boolean kick(ServerPlayer actor, ServerPlayer target) {
        Guild guild = guildOf(actor);
        if (guild == null || !guild.leader.equals(actor.getUUID().toString()) || target == actor) return false;
        String uuid = target.getUUID().toString();
        if (!guild.id.equals(membership.get(uuid))) return false;
        guild.members.remove(uuid); guild.roles.remove(uuid); membership.remove(uuid);
        guild.log(target.getGameProfile().getName() + " a ete exclu par " + actor.getGameProfile().getName());
        setDirty(); return true;
    }
    public boolean promote(ServerPlayer actor, ServerPlayer target) {
        Guild guild = guildOf(actor);
        if (guild == null || !guild.leader.equals(actor.getUUID().toString())) return false;
        String uuid = target.getUUID().toString();
        if (!guild.id.equals(membership.get(uuid)) || uuid.equals(guild.leader)) return false;
        String role = guild.roles.getOrDefault(uuid, "MEMBER");
        guild.roles.put(uuid, role.equals("OFFICER") ? "MEMBER" : "OFFICER");
        guild.log(target.getGameProfile().getName() + " est maintenant " + guild.roles.get(uuid));
        setDirty(); return true;
    }
    public boolean deposit(ServerPlayer player, ItemStack stack) {
        Guild guild = guildOf(player);
        if (guild == null || stack.isEmpty()) return false;
        guild.bank.add(stack.copy());
        guild.log(player.getGameProfile().getName() + " a depose " + stack.getCount() + "x " + stack.getHoverName().getString());
        stack.setCount(0); setDirty(); return true;
    }
    public ItemStack withdraw(ServerPlayer player, int slot, int count) {
        Guild guild = guildOf(player);
        if (guild == null || !guild.canManageBank(player.getUUID().toString()) || slot < 0 || slot >= guild.bank.size()) return ItemStack.EMPTY;
        ItemStack stored = guild.bank.get(slot);
        int amount = Math.min(Math.max(1, count), stored.getCount());
        ItemStack result = stored.split(amount);
        if (stored.isEmpty()) guild.bank.remove(slot);
        guild.log(player.getGameProfile().getName() + " a retire " + amount + "x " + result.getHoverName().getString());
        setDirty(); return result;
    }
    public void disband(ServerPlayer player) {
        Guild guild = guildOf(player);
        if (guild == null || !guild.leader.equals(player.getUUID().toString())) return;
        for (String uuid : guild.members.keySet()) membership.remove(uuid);
        guilds.remove(guild.id);
        invitations.values().removeIf(guild.id::equals);
        setDirty();
    }

    public static final class Guild {
        public final String id, name, leader;
        public final Map<String, String> members = new LinkedHashMap<>();
        public final Map<String, String> roles = new HashMap<>();
        public final List<ItemStack> bank = new ArrayList<>();
        public final List<String> journal = new ArrayList<>();
        Guild(String id, String name, String leader, String leaderName) {
            this.id=id; this.name=name; this.leader=leader;
            members.put(leader, leaderName); roles.put(leader, "LEADER");
        }
        public boolean canManage(String uuid) {
            String role=roles.get(uuid); return "LEADER".equals(role) || "OFFICER".equals(role);
        }
        public boolean canManageBank(String uuid) { return canManage(uuid); }
        public void log(String message) {
            journal.add(0, message);
            while (journal.size() > 50) journal.remove(journal.size()-1);
        }
        CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag tag=new CompoundTag();
            tag.putString("id",id); tag.putString("name",name); tag.putString("leader",leader);
            ListTag people=new ListTag();
            members.forEach((uuid, playerName)->{
                CompoundTag m=new CompoundTag(); m.putString("uuid",uuid); m.putString("name",playerName);
                m.putString("role",roles.getOrDefault(uuid,"MEMBER")); people.add(m);
            });
            tag.put("members",people);
            ListTag items=new ListTag();
            for(ItemStack stack:bank) if(!stack.isEmpty()) items.add(stack.save(registries));
            tag.put("bank",items);
            ListTag logs=new ListTag(); for(String line:journal){CompoundTag e=new CompoundTag();e.putString("text",line);logs.add(e);}
            tag.put("journal",logs);
            return tag;
        }
        static Guild load(CompoundTag tag, HolderLookup.Provider registries) {
            String leader=tag.getString("leader");
            Guild g=new Guild(tag.getString("id"),tag.getString("name"),leader,"");
            g.members.clear(); g.roles.clear();
            ListTag people=tag.getList("members",CompoundTag.TAG_COMPOUND);
            for(int i=0;i<people.size();i++){CompoundTag m=people.getCompound(i);String uuid=m.getString("uuid");g.members.put(uuid,m.getString("name"));g.roles.put(uuid,m.getString("role"));}
            ListTag items=tag.getList("bank",CompoundTag.TAG_COMPOUND);
            for(int i=0;i<items.size();i++) ItemStack.parse(registries,items.getCompound(i)).ifPresent(g.bank::add);
            ListTag logs=tag.getList("journal",CompoundTag.TAG_COMPOUND);
            for(int i=0;i<logs.size();i++)g.journal.add(logs.getCompound(i).getString("text"));
            return g;
        }
    }
}
