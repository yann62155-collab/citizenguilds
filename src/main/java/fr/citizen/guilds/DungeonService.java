package fr.citizen.guilds;

import com.google.gson.JsonObject;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Whole life cycle of the guild dungeon:
 * portal appears automatically -> a guild member clicks it (session starts) -> waves -> victory/defeat
 * -> survivors are brought back after a short delay -> portal disappears -> next portal is scheduled.
 */
public final class DungeonService {
    private static final long MINUTE = 20L * 60L;
    public static final long PORTAL_LIFETIME = 120L * MINUTE;
    private static final long FIRST_SPAWN_DELAY = 2L * MINUTE;
    private static final long RETRY_DELAY = MINUTE;
    private static final long CLOSE_DELAY = 20L * 20L;
    private static final long IDLE_TIMEOUT = 10L * MINUTE;
    private static final long NO_ENTRY_TIMEOUT = 5L * MINUTE;
    private static long lastActivityGameTime;

    private DungeonService() {}

    public record Result(boolean ok, String message) {
        public static Result ok(String message) { return new Result(true, message); }
        public static Result fail(String message) { return new Result(false, message); }
    }

    private static void broadcast(MinecraftServer server, String text) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
    }

    private static long now(MinecraftServer server) { return server.overworld().getGameTime(); }

    // ------------------------------------------------------------------ portal

    public static boolean isPortalPresent(MinecraftServer server) {
        DungeonData data = DungeonData.get(server);
        if (!data.hasPlacedPortal()) return false;
        ServerLevel overworld = server.overworld();
        BlockPos site = data.lastSite();
        // While the chunk is unloaded we trust the saved state: the gate block is unbreakable.
        if (!overworld.hasChunkAt(site)) return true;
        return overworld.getBlockState(site).is(CitizenDungeonBlocks.DUNGEON_GATE.get());
    }

    /** Builds the monument on a ground position (feet level) and registers it. */
    public static Result placePortal(MinecraftServer server, BlockPos ground) {
        ServerLevel overworld = server.overworld();
        if (!DungeonPortalStructure.place(overworld, ground))
            return Result.fail("Impossible de construire la porte a cet endroit (terrain occupe ou non charge).");
        DungeonData data = DungeonData.get(server);
        BlockPos gate = ground.above(2);
        data.setLastSite(gate);
        data.setPortalSpawnGameTime(now(server));
        data.setNextSpawnGameTime(0L);
        data.setLastSearchNote("Porte ouverte en " + ground.getX() + " " + ground.getY() + " " + ground.getZ() + ".");
        broadcast(server, "§6[Donjon] §fUne porte ancienne vient d'apparaitre en §eX " + ground.getX() + ", Y " + ground.getY()
            + ", Z " + ground.getZ() + "§f (Overworld). La premiere guilde qui la touche ouvre l'expedition ! Elle se refermera dans 2 heures.");
        return Result.ok("Porte placee en X " + ground.getX() + ", Y " + ground.getY() + ", Z " + ground.getZ() + ".");
    }

    /** Removes the monument (if any), forgets the site and schedules the next automatic portal. */
    public static void removePortal(MinecraftServer server) {
        DungeonData data = DungeonData.get(server);
        BlockPos site = data.lastSite();
        ServerLevel overworld = server.overworld();
        if (site != null && data.hasPlacedPortal()) {
            overworld.getChunkAt(site);
            if (overworld.getBlockState(site).is(CitizenDungeonBlocks.DUNGEON_GATE.get()))
                DungeonPortalStructure.remove(overworld, site);
        }
        RiftDungeonController.clear(server);
        data.clearLastSite();
        scheduleNext(server);
    }

    public static void scheduleNext(MinecraftServer server) {
        DungeonData data = DungeonData.get(server);
        data.setNextSpawnGameTime(now(server) + data.spawnIntervalMinutes() * MINUTE);
    }

    /** Immediate search + placement (admin button / command). */
    public static Result spawnNow(MinecraftServer server) {
        if (DungeonSessionData.get(server).active()) return Result.fail("Une expedition est en cours : attends sa fin.");
        if (isPortalPresent(server)) return Result.fail("Une porte est deja ouverte. Retire-la d'abord.");
        DungeonData data = DungeonData.get(server);
        data.clearLastSite();
        DungeonSiteFinder.Search search = DungeonSiteFinder.findAuto(server.overworld(), data);
        data.setLastSearchNote(search.note());
        if (search.site() == null) return Result.fail(search.note());
        return RiftDungeonController.begin(server, search.site());
    }

    // ------------------------------------------------------------------ session

    public static Result startSession(MinecraftServer server, GuildSavedData.Guild guild) {
        if (guild == null) return Result.fail("Guilde introuvable.");
        DungeonSessionData session = DungeonSessionData.get(server);
        if (session.active())
            return Result.fail("Le donjon est deja occupe par la guilde " + session.guildName() + ".");
        DungeonData data = DungeonData.get(server);
        if (!isPortalPresent(server)) return Result.fail("Aucune porte de donjon n'est ouverte pour le moment.");
        BlockPos site = data.lastSite();
        server.overworld().getChunkAt(site);
        ServerLevel dungeon = DungeonArena.prepare(server);
        if (dungeon == null) return Result.fail("Le monde du donjon est indisponible (redemarre le serveur apres la mise a jour).");
        if (!session.start(guild.id, guild.name, site)) return Result.fail("Impossible de demarrer l'expedition.");
        session.setStartedGameTime(now(server));
        lastActivityGameTime = now(server);
        GuildSavedData.get(server).recordDungeonStart(guild.id);
        broadcast(server, "§6[Donjon] §fLa guilde §e" + guild.name + "§f a ouvert la porte en X " + site.getX()
            + ", Y " + site.getY() + ", Z " + site.getZ() + ". Ses membres peuvent la rejoindre.");
        return Result.ok("Expedition ouverte pour " + guild.name + ".");
    }

    /** Ends the session, brings everyone back, removes the portal and schedules the next one. */
    public static Result endSession(MinecraftServer server, String announcement) {
        DungeonSessionData session = DungeonSessionData.get(server);
        if (!session.active()) return Result.fail("Aucune expedition en cours.");
        String guildName = session.guildName();
        BlockPos site = session.site();
        ServerLevel dungeon = server.getLevel(CitizenDimensions.DUNGEON);
        if (dungeon != null) session.removeActiveMobs(dungeon);
        ServerLevel overworld = server.overworld();
        BlockPos exit = exitPosition(overworld, site);
        if (dungeon != null) {
            for (ServerPlayer player : java.util.List.copyOf(dungeon.players())) {
                if (player.hasPermissions(2) && !session.isParticipant(player.getUUID().toString())) continue;
                player.teleportTo(overworld, exit.getX() + 0.5D, exit.getY(), exit.getZ() + 0.5D, player.getYRot(), player.getXRot());
            }
        }
        session.end();
        removePortal(server);
        if (announcement != null && !announcement.isBlank()) broadcast(server, announcement);
        return Result.ok("Expedition de " + guildName + " fermee.");
    }

    public static BlockPos exitPosition(ServerLevel overworld, BlockPos gate) {
        if (gate == null) return overworld.getSharedSpawnPos();
        int x = gate.getX();
        int z = gate.getZ() + 2;
        overworld.getChunkAt(new BlockPos(x, gate.getY(), z));
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, Math.max(y, gate.getY() - 2), z);
    }

    // ------------------------------------------------------------------ timers (called every second)

    /** Returns true when the session was handled here and the wave logic must not run this second. */
    public static boolean tickSession(MinecraftServer server, DungeonSessionData session, boolean participantInside) {
        long time = now(server);
        if (session.startedGameTime() == 0L) { session.setStartedGameTime(time); lastActivityGameTime = time; }
        if (session.finished()) {
            if (session.finishedGameTime() == 0L) {
                session.setFinishedGameTime(time);
                broadcast(server, "§7[Donjon] Retour a la surface dans 20 secondes. La porte va se refermer.");
            } else if (time - session.finishedGameTime() >= CLOSE_DELAY) {
                endSession(server, "§7[Donjon] La porte s'est refermee. Une nouvelle porte apparaitra dans environ "
                    + DungeonData.get(server).spawnIntervalMinutes() + " minutes.");
            }
            return true;
        }
        if (participantInside) { lastActivityGameTime = time; return false; }
        if (lastActivityGameTime < session.startedGameTime()) lastActivityGameTime = session.startedGameTime();
        long limit = session.participantCount() == 0 ? NO_ENTRY_TIMEOUT : IDLE_TIMEOUT;
        if (time - lastActivityGameTime >= limit) {
            GuildSavedData.get(server).recordDungeonFailure(session.guildId());
            endSession(server, "§c[Donjon] L'expedition de " + session.guildName()
                + " a ete abandonnee (personne dans le donjon). La porte s'est refermee.");
            return true;
        }
        return false;
    }

    public static void tickAutoSpawn(MinecraftServer server) {
        DungeonData data = DungeonData.get(server);
        long time = now(server);
        if (RiftDungeonController.isCharging()) return;
        if (data.hasPlacedPortal()) {
            if (time - data.portalSpawnGameTime() >= PORTAL_LIFETIME || time < data.portalSpawnGameTime()) {
                removePortal(server);
                broadcast(server, "§7[Donjon] Personne n'a franchi la porte ancienne : elle s'est refermee.");
            } else if (!isPortalPresent(server)) {
                data.clearLastSite();
                scheduleNext(server);
            }
            return;
        }
        if (!data.autoSpawnEnabled()) return;
        if (data.nextSpawnGameTime() <= 0L || data.nextSpawnGameTime() - time > data.spawnIntervalMinutes() * MINUTE) {
            data.setNextSpawnGameTime(time + FIRST_SPAWN_DELAY);
            return;
        }
        if (time < data.nextSpawnGameTime()) return;
        if (server.getPlayerList().getPlayerCount() == 0) return; // nobody to enjoy it: wait for the first login
        DungeonSiteFinder.Search search = DungeonSiteFinder.findAuto(server.overworld(), data);
        data.setLastSearchNote(search.note());
        if (search.site() == null) {
            data.setNextSpawnGameTime(time + RETRY_DELAY);
            return;
        }
        if (!RiftDungeonController.begin(server, search.site()).ok()) data.setNextSpawnGameTime(time + RETRY_DELAY);
    }

    // ------------------------------------------------------------------ menu data

    public static JsonObject describe(MinecraftServer server, GuildSavedData.Guild viewerGuild) {
        DungeonData data = DungeonData.get(server);
        DungeonSessionData session = DungeonSessionData.get(server);
        long time = now(server);
        JsonObject root = new JsonObject();
        boolean portal = isPortalPresent(server);
        root.addProperty("portal", portal);
        if (portal) {
            BlockPos ground = data.lastSite().below(2);
            root.addProperty("x", ground.getX());
            root.addProperty("y", ground.getY());
            root.addProperty("z", ground.getZ());
            long left = PORTAL_LIFETIME - (time - data.portalSpawnGameTime());
            root.addProperty("closesIn", session.active() ? -1 : Math.max(0L, (left + MINUTE - 1) / MINUTE));
        }
        long next = data.nextSpawnGameTime() - time;
        root.addProperty("nextIn", data.nextSpawnGameTime() <= 0L ? -1L : Math.max(0L, (next + MINUTE - 1) / MINUTE));
        root.addProperty("auto", data.autoSpawnEnabled());
        root.addProperty("interval", data.spawnIntervalMinutes());
        root.addProperty("reward", data.rewardJade());
        root.addProperty("note", data.lastSearchNote());
        root.addProperty("minX", data.minX());
        root.addProperty("minY", data.minY());
        root.addProperty("minZ", data.minZ());
        root.addProperty("active", session.active());
        if (session.active()) {
            root.addProperty("guild", session.guildName());
            root.addProperty("mine", viewerGuild != null && viewerGuild.id.equals(session.guildId()));
            root.addProperty("state", session.finished() ? (session.victory() ? "VICTOIRE" : "ECHEC") : "EN COURS");
            root.addProperty("wave", session.wave());
            root.addProperty("participants", session.participantCount());
            root.addProperty("alive", session.aliveCount());
        }
        return root;
    }

    public static String statusLine(MinecraftServer server) {
        JsonObject d = describe(server, null);
        StringBuilder sb = new StringBuilder();
        sb.append("Auto : ").append(d.get("auto").getAsBoolean() ? "activee" : "desactivee")
          .append(" | Intervalle : ").append(d.get("interval").getAsInt()).append(" min");
        if (d.get("portal").getAsBoolean()) sb.append(" | Porte ouverte en ").append(d.get("x").getAsInt()).append(' ')
            .append(d.get("y").getAsInt()).append(' ').append(d.get("z").getAsInt());
        else if (d.get("nextIn").getAsLong() >= 0) sb.append(" | Prochaine porte dans ~").append(d.get("nextIn").getAsLong()).append(" min");
        if (d.get("active").getAsBoolean()) sb.append(" | Expedition : ").append(d.get("guild").getAsString());
        String note = d.get("note").getAsString();
        if (!note.isEmpty()) sb.append(" | Derniere recherche : ").append(note);
        return sb.toString();
    }

    static ServerPlayer online(MinecraftServer server, String uuid) {
        try { return server.getPlayerList().getPlayer(UUID.fromString(uuid)); }
        catch (IllegalArgumentException e) { return null; }
    }
}
