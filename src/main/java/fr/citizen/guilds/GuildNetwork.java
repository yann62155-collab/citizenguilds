package fr.citizen.guilds;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = CitizenGuilds.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class GuildNetwork {
    private GuildNetwork() {}

    public record Request(String action) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CitizenGuilds.MOD_ID, "guild_request"));
        public static final StreamCodec<ByteBuf, Request> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Request::action, Request::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Snapshot(String json) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CitizenGuilds.MOD_ID, "guild_snapshot"));
        public static final StreamCodec<ByteBuf, Snapshot> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Snapshot::json, Snapshot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Request.TYPE, Request.CODEC, GuildNetwork::handleRequest);
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, GuildNetwork::handleSnapshot);
    }

    private static void handleRequest(Request request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!"refresh".equals(request.action())) return;
        PacketDistributor.sendToPlayer(player, new Snapshot(createSnapshot(player)));
    }

    private static String createSnapshot(ServerPlayer player) {
        GuildSavedData data = GuildSavedData.get(player.getServer());
        GuildSavedData.Guild guild = data.guildOf(player);
        JsonObject root = new JsonObject();
        root.addProperty("hasGuild", guild != null);
        root.addProperty("player", player.getGameProfile().getName());
        JsonArray guildList = new JsonArray();
        for (GuildSavedData.Guild item : data.allGuilds()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", item.name);
            entry.addProperty("members", item.members.size());
            guildList.add(entry);
        }
        root.add("guilds", guildList);
        if (guild == null) return root.toString();

        root.addProperty("name", guild.name);
        root.addProperty("leader", guild.leader);
        root.addProperty("isLeader", guild.leader.equals(player.getUUID().toString()));
        root.addProperty("canManage", guild.canManage(player.getUUID().toString()));
        root.addProperty("memberCount", guild.members.size());
        root.addProperty("totalDepositedItems", guild.totalDepositedItems);
        root.addProperty("dungeonWins", guild.dungeonWins);
        root.addProperty("dungeonDeaths", guild.dungeonDeaths);
        root.addProperty("isAdmin", player.hasPermissions(2));

        JsonArray members = new JsonArray();
        guild.members.forEach((uuid, name) -> {
            JsonObject m = new JsonObject();
            m.addProperty("name", name);
            m.addProperty("role", guild.roles.getOrDefault(uuid, "MEMBER"));
            m.addProperty("self", uuid.equals(player.getUUID().toString()));
            members.add(m);
        });
        root.add("members", members);

        JsonArray bank = new JsonArray();
        for (int i = 0; i < guild.bank.size(); i++) {
            var stack = guild.bank.get(i);
            JsonObject item = new JsonObject();
            item.addProperty("slot", i + 1);
            item.addProperty("name", stack.getHoverName().getString());
            item.addProperty("count", stack.getCount());
            bank.add(item);
        }
        root.add("bank", bank);

        JsonArray journal = new JsonArray();
        for (String line : guild.journal) journal.add(line);
        root.add("journal", journal);
        root.addProperty("autoDepositItem", data.getAutoDepositItemId());
        return root.toString();
    }

    private static void handleSnapshot(Snapshot snapshot, IPayloadContext context) {
        context.enqueueWork(() -> GuildManagementScreen.acceptSnapshot(snapshot.json()));
    }

    public static void requestSync() {
        PacketDistributor.sendToServer(new Request("refresh"));
    }
}
