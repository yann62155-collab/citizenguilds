package fr.citizen.guilds;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Menu <-> server link. Every menu button is a direct server action, the answer is a fresh snapshot. */
@EventBusSubscriber(modid = CitizenGuilds.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class GuildNetwork {
    private GuildNetwork() {}

    public record Request(String action, String arg) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CitizenGuilds.MOD_ID, "guild_action"));
        public static final StreamCodec<ByteBuf, Request> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), Request::action,
            ByteBufCodecs.stringUtf8(256), Request::arg,
            Request::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Snapshot(String json) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CitizenGuilds.MOD_ID, "guild_state"));
        public static final StreamCodec<ByteBuf, Snapshot> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(262144), Snapshot::json, Snapshot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(Request.TYPE, Request.CODEC, GuildNetwork::handleRequest);
        registrar.playToClient(Snapshot.TYPE, Snapshot.CODEC, GuildNetwork::handleSnapshot);
        registrar.playToClient(RiftSkyStatePayload.TYPE, RiftSkyStatePayload.CODEC,
            (payload, context) -> context.enqueueWork(() ->
                net.neoforged.fml.DistExecutor.unsafeRunWhenOn(net.neoforged.api.distmarker.Dist.CLIENT,
                    () -> () -> RiftSkyClient.setActive(payload.active()))));
    }

    private static void handleRequest(Request request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            DungeonService.Result result = perform(player, request.action(), request.arg() == null ? "" : request.arg().trim());
            PacketDistributor.sendToPlayer(player, new Snapshot(createSnapshot(player, result)));
        });
    }

    // ------------------------------------------------------------------ actions

    private static DungeonService.Result ok(String m) { return DungeonService.Result.ok(m); }
    private static DungeonService.Result fail(String m) { return DungeonService.Result.fail(m); }

    private static DungeonService.Result perform(ServerPlayer player, String action, String arg) {
        MinecraftServer server = player.getServer();
        GuildSavedData data = GuildSavedData.get(server);
        GuildSavedData.Guild guild = data.guildOf(player);
        boolean admin = player.hasPermissions(2);
        switch (action) {
            case "refresh": return null;
            case "create":
                return data.create(player, arg) != null ? ok("La guilde " + arg + " est fondee. Tu en es le chef.")
                    : fail("Creation impossible : nom deja pris, 3 a 24 caracteres, ou tu as deja une guilde.");
            case "rename":
                return data.rename(player, arg) ? ok("Guilde renommee en " + arg + ".") : fail("Seul le chef peut renommer (3 a 24 caracteres, nom libre).");
            case "invite": {
                int number;
                try { number = Integer.parseInt(arg); } catch (NumberFormatException e) { return fail("Indique un numero de hibou valide."); }
                return data.inviteByHibou(player, number) ? ok("Invitation envoyee au hibou n°" + number + ".")
                    : fail("Invitation impossible : numero inconnu, joueur deja en guilde/invite ou droits insuffisants.");
            }
            case "accept": return data.accept(player) ? ok("Bienvenue dans ta nouvelle guilde !") : fail("Aucune invitation valide.");
            case "refuse": return data.refuse(player) ? ok("Invitation refusee.") : fail("Aucune invitation a refuser.");
            case "leave": return data.leave(player) ? ok("Tu as quitte la guilde.") : fail("Le chef ne peut pas quitter : dissous la guilde.");
            case "disband":
                if (guild == null || !guild.leader.equals(player.getUUID().toString())) return fail("Seul le chef peut dissoudre la guilde.");
                data.disband(player);
                return ok("La guilde a ete dissoute.");
            case "kick": return data.kickByUuid(player, arg) ? ok("Membre exclu.") : fail("Exclusion refusee (reserve au chef).");
            case "promote": return data.promoteByUuid(player, arg) ? ok("Grade modifie.") : fail("Changement de grade refuse (reserve au chef).");
            case "deposit": {
                ItemStack hand = player.getMainHandItem();
                if (hand.isEmpty()) return fail("Prends en main l'objet a deposer.");
                int count = hand.getCount();
                String name = hand.getHoverName().getString();
                return data.deposit(player, hand) ? ok(count + "x " + name + " depose(s) dans la reserve.") : fail("Depot impossible (pas de guilde ou reserve pleine).");
            }
            case "withdraw": {
                String[] parts = arg.split(":");
                int slot, count;
                try { slot = Integer.parseInt(parts[0]); count = parts.length > 1 ? Integer.parseInt(parts[1]) : 64; }
                catch (NumberFormatException e) { return fail("Emplacement invalide."); }
                ItemStack stack = data.withdraw(player, slot, count);
                if (stack.isEmpty()) return fail("Retrait reserve au chef et aux officiers.");
                String label = stack.getCount() + "x " + stack.getHoverName().getString();
                if (!player.getInventory().add(stack)) player.drop(stack, false);
                return ok(label + " retire(s) de la reserve.");
            }
            default: break;
        }
        if (!action.startsWith("admin_")) return fail("Action inconnue.");
        if (!admin) return fail("Action reservee aux administrateurs (OP 2).");
        DungeonData dungeon = DungeonData.get(server);
        switch (action) {
            case "admin_autospawn": {
                boolean enabled = !dungeon.autoSpawnEnabled();
                dungeon.setAutoSpawnEnabled(enabled);
                if (enabled) dungeon.setNextSpawnGameTime(0L);
                return ok("Apparition automatique " + (enabled ? "activee : premiere porte dans ~2 min." : "desactivee."));
            }
            case "admin_spawnnow": return DungeonService.spawnNow(server);
            case "admin_removeportal":
                if (DungeonSessionData.get(server).active()) return fail("Une expedition est en cours.");
                if (!DungeonService.isPortalPresent(server)) return fail("Aucune porte ouverte.");
                DungeonService.removePortal(server);
                return ok("Porte retiree.");
            case "admin_end": return DungeonService.endSession(server, "§7[Donjon] L'expedition a ete fermee par un administrateur.");
            case "admin_interval": {
                int delta;
                try { delta = Integer.parseInt(arg); } catch (NumberFormatException e) { return fail("Valeur invalide."); }
                dungeon.setSpawnIntervalMinutes(dungeon.spawnIntervalMinutes() + delta);
                return ok("Intervalle : " + dungeon.spawnIntervalMinutes() + " min.");
            }
            case "admin_reward": {
                int delta;
                try { delta = Integer.parseInt(arg); } catch (NumberFormatException e) { return fail("Valeur invalide."); }
                dungeon.setRewardJade(dungeon.rewardJade() + delta);
                return ok("Recompense : " + dungeon.rewardJade() + " Jade par survivant.");
            }
            case "admin_disband":
                return data.adminDisband(arg) ? ok("Guilde " + arg + " dissoute.") : fail("Guilde introuvable.");
            default: return fail("Action inconnue.");
        }
    }

    // ------------------------------------------------------------------ snapshot

    private static String createSnapshot(ServerPlayer player, DungeonService.Result result) {
        MinecraftServer server = player.getServer();
        GuildSavedData data = GuildSavedData.get(server);
        GuildSavedData.Guild guild = data.guildOf(player);
        JsonObject root = new JsonObject();
        if (result != null) {
            root.addProperty("notice", result.message());
            root.addProperty("noticeOk", result.ok());
        }
        root.addProperty("hasGuild", guild != null);
        root.addProperty("player", player.getGameProfile().getName());
        root.addProperty("isAdmin", player.hasPermissions(2));
        GuildSavedData.Guild invitation = data.pendingInvitationFor(player);
        root.addProperty("hasInvitation", invitation != null);
        if (invitation != null) {
            root.addProperty("invitationGuild", invitation.name);
            root.addProperty("invitationGuildMembers", invitation.members.size());
        }
        JsonArray guildList = new JsonArray();
        for (GuildSavedData.Guild item : data.allGuilds()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", item.name);
            entry.addProperty("members", item.members.size());
            entry.addProperty("leader", item.members.getOrDefault(item.leader, "?"));
            entry.addProperty("wins", item.dungeonWins);
            guildList.add(entry);
        }
        root.add("guilds", guildList);
        root.add("dungeon", DungeonService.describe(server, guild));
        root.addProperty("autoDepositItem", data.getAutoDepositItemId());
        if (guild == null) return root.toString();

        String self = player.getUUID().toString();
        root.addProperty("name", guild.name);
        root.addProperty("role", guild.roles.getOrDefault(self, "MEMBER"));
        root.addProperty("isLeader", guild.leader.equals(self));
        root.addProperty("canManage", guild.canManage(self));
        root.addProperty("memberCount", guild.members.size());
        root.addProperty("totalDepositedItems", guild.totalDepositedItems);
        root.addProperty("dungeonWins", guild.dungeonWins);
        root.addProperty("dungeonDeaths", guild.dungeonDeaths);
        JsonArray members = new JsonArray();
        guild.members.forEach((uuid, name) -> {
            JsonObject m = new JsonObject();
            m.addProperty("uuid", uuid);
            m.addProperty("name", name);
            m.addProperty("role", guild.roles.getOrDefault(uuid, "MEMBER"));
            m.addProperty("self", uuid.equals(self));
            m.addProperty("online", DungeonService.online(server, uuid) != null);
            members.add(m);
        });
        root.add("members", members);

        JsonArray bank = new JsonArray();
        for (int i = 0; i < guild.bank.size(); i++) {
            ItemStack stack = guild.bank.get(i);
            JsonObject item = new JsonObject();
            item.addProperty("slot", i);
            item.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            item.addProperty("name", stack.getHoverName().getString());
            item.addProperty("count", stack.getCount());
            bank.add(item);
        }
        root.add("bank", bank);

        JsonArray journal = new JsonArray();
        for (String line : guild.journal) journal.add(line);
        root.add("journal", journal);
        return root.toString();
    }

    private static void handleSnapshot(Snapshot snapshot, IPayloadContext context) {
        context.enqueueWork(() -> GuildManagementScreen.acceptSnapshot(snapshot.json()));
    }

    public static void send(String action, String arg) {
        PacketDistributor.sendToServer(new Request(action, arg == null ? "" : arg));
    }

    public static void requestSync() { send("refresh", ""); }
}
