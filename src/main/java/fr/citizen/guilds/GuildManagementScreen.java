package fr.citizen.guilds;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Guild menu (key N). Responsive medieval panel, horizontal tabs, every button sends a direct
 * server action (no chat command typed for the player) and the server answers with a fresh snapshot.
 */
public final class GuildManagementScreen extends Screen {
    // Citizen palette: black, old gold, dark red, parchment.
    private static final int BG = 0xF0120E0B, HEADER = 0xFF1C1611, CARD = 0xFF241C15, CARD_HI = 0xFF33271B;
    private static final int GOLD = 0xFFD9B45A, GOLD_DARK = 0xFF7A5E2C, LINE = 0xFF3D3022;
    private static final int TEXT = 0xFFF1E4C3, MUTED = 0xFFA8957A, RED = 0xFFC8524C, GREEN = 0xFF7FC97F, RED_DARK = 0xFF5A1E1C;

    private static JsonObject snapshot = new JsonObject();
    private static String tab = "home";

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean inside(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<Hit> rightHits = new ArrayList<>();
    private EditBox nameBox, hibouBox;
    private String nameText = "", hibouText = "";
    private int scroll, maxScroll;
    private int px, py, pw, ph;
    private int ticks;
    private boolean previousHideGui;
    private String confirmKey = "";
    private long confirmUntil;
    private String notice = "";
    private boolean noticeOk = true;
    private long noticeUntil;
    private List<Component> tooltip;
    private String structureKey = "";

    public GuildManagementScreen() { super(Component.translatable("screen.citizenguilds.title")); }

    public static void acceptSnapshot(String json) {
        try { snapshot = JsonParser.parseString(json).getAsJsonObject(); }
        catch (Exception ignored) { snapshot = new JsonObject(); }
        if (Minecraft.getInstance().screen instanceof GuildManagementScreen screen) screen.onSnapshot();
    }

    private void onSnapshot() {
        if (snapshot.has("notice")) {
            notice = str(snapshot, "notice", "");
            noticeOk = bool(snapshot, "noticeOk");
            noticeUntil = System.currentTimeMillis() + 6000L;
        }
        if (!isAvailable(tab)) tab = "home";
        if (!structureKey().equals(structureKey)) rebuildWidgets();
    }

    private String structureKey() {
        return tab + "|" + bool(snapshot, "hasGuild") + "|" + bool(snapshot, "canManage") + "|" + bool(snapshot, "isLeader") + "|" + width + "x" + height;
    }

    @Override public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------ layout

    @Override protected void init() {
        if (structureKey.isEmpty()) {
            previousHideGui = minecraft.options.hideGui;
            minecraft.options.hideGui = true; // hide the HUD behind the menu
            GuildNetwork.requestSync();
        }
        pw = Math.min(width - 16, 540);
        ph = Math.min(height - 16, 310);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        if (nameBox != null) nameText = nameBox.getValue();
        if (hibouBox != null) hibouText = hibouBox.getValue();
        nameBox = null;
        hibouBox = null;
        int cx = px + 12, cy = contentTop();
        boolean hasGuild = bool(snapshot, "hasGuild");
        switch (tab) {
            case "home" -> {
                if (!hasGuild) nameBox = box(cx + 10, cy + (bool(snapshot, "hasInvitation") ? 92 : 40), 170, "Nom de la guilde", nameText, 24);
                else if (bool(snapshot, "isLeader")) nameBox = box(cx + 10, cy + 150, 170, "Nouveau nom", nameText, 24);
            }
            case "members" -> {
                if (hasGuild && bool(snapshot, "canManage")) hibouBox = box(cx, cy + 2, 110, "N° de hibou", hibouText, 8);
            }
            default -> { }
        }
        structureKey = structureKey();
    }

    private EditBox box(int x, int y, int w, String hint, String value, int max) {
        EditBox b = new EditBox(font, x, y, w, 18, Component.literal(hint));
        b.setHint(Component.literal(hint).withColor(MUTED));
        b.setMaxLength(max);
        b.setValue(value);
        b.setTextColor(TEXT);
        addRenderableWidget(b);
        return b;
    }

    private int contentTop() { return py + 56; }
    private int contentBottom() { return py + ph - 22; }
    private int contentWidth() { return pw - 24; }

    @Override public void tick() {
        super.tick();
        if (++ticks % 100 == 0) GuildNetwork.requestSync(); // keeps timers and the member list live
    }

    @Override public void removed() {
        minecraft.options.hideGui = previousHideGui;
        super.removed();
    }

    // ------------------------------------------------------------------ input

    @Override public boolean mouseClicked(double mx, double my, int button) {
        List<Hit> list = button == 1 ? rightHits : button == 0 ? hits : List.of();
        for (int i = list.size() - 1; i >= 0; i--) {
            Hit hit = list.get(i);
            if (hit.inside(mx, my)) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                hit.action().run();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(scrollY) * 14));
        return true;
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = (nameBox != null && nameBox.isFocused()) || (hibouBox != null && hibouBox.isFocused());
        if (!typing && ClientSetup.OPEN_GUILD.matches(keyCode, scanCode)) { onClose(); return true; }
        if (typing && (keyCode == 257 || keyCode == 335)) { // Enter submits the focused field
            if (hibouBox != null && hibouBox.isFocused()) invite();
            else if (nameBox != null && nameBox.isFocused()) submitName();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void send(String action, String arg) { GuildNetwork.send(action, arg); }

    private void switchTab(String id) {
        if (id.equals(tab)) return;
        tab = id;
        scroll = 0;
        confirmKey = "";
        rebuildWidgets();
    }

    private void invite() {
        if (hibouBox == null) return;
        String v = hibouBox.getValue().trim();
        if (!v.matches("\\d{1,8}")) { localNotice("Saisis le numero de hibou du joueur (chiffres uniquement).", false); return; }
        send("invite", v);
        hibouBox.setValue("");
    }

    private void submitName() {
        if (nameBox == null) return;
        String v = nameBox.getValue().trim();
        if (v.length() < 3 || v.length() > 24) { localNotice("Le nom doit faire entre 3 et 24 caracteres.", false); return; }
        send(bool(snapshot, "hasGuild") ? "rename" : "create", v);
        nameBox.setValue("");
    }

    private void localNotice(String text, boolean ok) {
        notice = text; noticeOk = ok; noticeUntil = System.currentTimeMillis() + 5000L;
    }

    /** Destructive actions need a second click within 3 seconds. */
    private Runnable confirm(String key, Runnable action) {
        return () -> {
            if (key.equals(confirmKey) && System.currentTimeMillis() < confirmUntil) { confirmKey = ""; action.run(); }
            else { confirmKey = key; confirmUntil = System.currentTimeMillis() + 3000L; }
        };
    }

    private String confirmLabel(String key, String label) {
        return key.equals(confirmKey) && System.currentTimeMillis() < confirmUntil ? "Confirmer ?" : label;
    }

    // ------------------------------------------------------------------ rendering

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xC8000000);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        rightHits.clear();
        tooltip = null;
        renderBackground(g, mouseX, mouseY, partialTick);
        drawFrame(g, mouseX, mouseY);
        int top = contentTop(), bottom = contentBottom();
        switch (tab) {
            case "members" -> renderMembers(g, mouseX, mouseY, top, bottom);
            case "bank" -> renderBank(g, mouseX, mouseY, top, bottom);
            case "dungeon" -> renderDungeon(g, mouseX, mouseY, top, bottom);
            case "journal" -> renderJournal(g, mouseX, mouseY, top, bottom);
            case "admin" -> renderAdmin(g, mouseX, mouseY, top, bottom);
            default -> renderHome(g, mouseX, mouseY, top, bottom);
        }
        if (nameBox != null) nameBox.render(g, mouseX, mouseY, partialTick);
        if (hibouBox != null) hibouBox.render(g, mouseX, mouseY, partialTick);
        drawFooter(g);
        if (tooltip != null) g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    private void drawFrame(GuiGraphics g, int mx, int my) {
        g.fill(px, py, px + pw, py + ph, BG);
        g.renderOutline(px, py, pw, ph, GOLD_DARK);
        g.renderOutline(px + 2, py + 2, pw - 4, ph - 4, LINE);
        g.fill(px + 3, py + 3, px + pw - 3, py + 30, HEADER);
        g.fill(px + 3, py + 30, px + pw - 3, py + 31, GOLD_DARK);
        g.drawString(font, "⚜ GUILDES DE CITIZEN", px + 12, py + 12, GOLD, false);
        String right = bool(snapshot, "hasGuild")
            ? str(snapshot, "name", "") + "  •  " + roleLabel(str(snapshot, "role", "MEMBER"))
            : "Sans guilde";
        int closeX = px + pw - 22;
        g.drawString(font, ellipsize(right, pw / 2), closeX - 8 - Math.min(font.width(right), pw / 2), py + 12, TEXT, false);
        button(g, closeX, py + 8, 15, 15, "✕", true, mx, my, this::onClose);

        List<String[]> tabs = new ArrayList<>();
        tabs.add(new String[]{"home", "Accueil"});
        if (bool(snapshot, "hasGuild")) {
            tabs.add(new String[]{"members", "Membres"});
            tabs.add(new String[]{"bank", "Réserve"});
        }
        tabs.add(new String[]{"dungeon", "Donjon"});
        if (bool(snapshot, "hasGuild")) tabs.add(new String[]{"journal", "Journal"});
        if (bool(snapshot, "isAdmin")) tabs.add(new String[]{"admin", "Admin"});
        int tx = px + 8, tw = (pw - 16) / tabs.size();
        for (String[] t : tabs) {
            boolean active = t[0].equals(tab);
            boolean hover = !active && inside(mx, my, tx, py + 34, tw - 2, 16);
            g.fill(tx, py + 34, tx + tw - 2, py + 50, active ? CARD_HI : hover ? CARD : HEADER);
            if (active) g.fill(tx, py + 48, tx + tw - 2, py + 50, GOLD);
            String label = ellipsize(t[1], tw - 6);
            g.drawString(font, label, tx + (tw - 2 - font.width(label)) / 2, py + 38, active ? GOLD : hover ? TEXT : MUTED, false);
            String id = t[0];
            if (!active) hits.add(new Hit(tx, py + 34, tw - 2, 16, () -> switchTab(id)));
            tx += tw;
        }
    }

    private void drawFooter(GuiGraphics g) {
        int y = py + ph - 16;
        g.fill(px + 3, y - 3, px + pw - 3, y - 2, LINE);
        if (!notice.isEmpty() && System.currentTimeMillis() < noticeUntil) {
            g.drawString(font, ellipsize((noticeOk ? "✔ " : "✖ ") + notice, pw - 24), px + 12, y + 1, noticeOk ? GREEN : RED, false);
        } else {
            String hint = "N ou Échap pour fermer";
            g.drawString(font, hint, px + pw - 12 - font.width(hint), y + 1, GOLD_DARK, false);
            g.drawString(font, "Citizen Guilds", px + 12, y + 1, GOLD_DARK, false);
        }
    }

    // ------------------------------------------------------------------ tabs

    private void renderHome(GuiGraphics g, int mx, int my, int top, int bottom) {
        int x = px + 12, w = contentWidth();
        if (!bool(snapshot, "hasGuild")) {
            int y = top;
            if (bool(snapshot, "hasInvitation")) {
                g.fill(x, y, x + w, y + 46, CARD_HI);
                g.renderOutline(x, y, w, 46, GOLD);
                g.drawString(font, "INVITATION REÇUE", x + 10, y + 7, GOLD, false);
                g.drawString(font, ellipsize("La guilde " + str(snapshot, "invitationGuild", "?") + " (" + num(snapshot, "invitationGuildMembers")
                    + " membre(s)) souhaite te recruter.", w - 190), x + 10, y + 24, TEXT, false);
                button(g, x + w - 172, y + 15, 80, 18, "Accepter", true, mx, my, () -> send("accept", ""));
                button(g, x + w - 86, y + 15, 76, 18, "Refuser", true, mx, my, () -> send("refuse", ""));
                y += 52;
            }
            card(g, x, y, w, 64);
            g.drawString(font, "FONDER UNE GUILDE", x + 10, y + 7, GOLD, false);
            g.drawString(font, "Choisis un nom (3 à 24 caractères) : tu deviendras chef.", x + 10, y + 46, MUTED, false);
            if (nameBox != null) nameBox.setY(y + 22);
            button(g, x + 188, y + 22, 90, 18, "Fonder", true, mx, my, this::submitName);
            y += 72;
            sectionTitle(g, x, y, "GUILDES DU ROYAUME");
            y += 14;
            JsonArray guilds = arr(snapshot, "guilds");
            int listBottom = bottom;
            beginList(g, x, y, w, listBottom, guilds.size() * 20);
            int ry = y - scroll;
            for (JsonElement e : guilds) {
                JsonObject o = e.getAsJsonObject();
                if (ry + 18 > y && ry < listBottom) {
                    g.fill(x, ry, x + w, ry + 18, CARD);
                    g.drawString(font, ellipsize(str(o, "name", "?"), w / 2 - 10), x + 8, ry + 5, TEXT, false);
                    g.drawString(font, "Chef : " + str(o, "leader", "?"), x + w / 2, ry + 5, MUTED, false);
                    String c = num(o, "members") + " membre(s)";
                    g.drawString(font, c, x + w - 8 - font.width(c), ry + 5, GOLD, false);
                }
                ry += 20;
            }
            if (guilds.isEmpty()) g.drawString(font, "Aucune guilde pour l'instant. Sois le premier !", x + 4, y + 4, MUTED, false);
            endList(g);
            return;
        }
        int y = top;
        int cw = (w - 18) / 4;
        stat(g, x, y, cw, "MEMBRES", Integer.toString(num(snapshot, "memberCount")));
        stat(g, x + (cw + 6), y, cw, "TON GRADE", roleLabel(str(snapshot, "role", "MEMBER")));
        stat(g, x + (cw + 6) * 2, y, cw, "DONJONS GAGNÉS", Integer.toString(num(snapshot, "dungeonWins")));
        stat(g, x + (cw + 6) * 3, y, cw, "OBJETS DÉPOSÉS", Long.toString(lng(snapshot, "totalDepositedItems")));
        y += 48;
        JsonObject d = obj(snapshot, "dungeon");
        card(g, x, y, w, 40);
        g.drawString(font, "PORTE DE DONJON", x + 10, y + 7, GOLD, false);
        g.drawString(font, ellipsize(dungeonSummary(d), w - 120), x + 10, y + 23, TEXT, false);
        button(g, x + w - 100, y + 11, 90, 18, "Voir le donjon", true, mx, my, () -> switchTab("dungeon"));
        y += 48;
        boolean leader = bool(snapshot, "isLeader");
        card(g, x, y, w, 48);
        g.drawString(font, leader ? "RENOMMER LA GUILDE" : "TA GUILDE", x + 10, y + 7, GOLD, false);
        if (leader) {
            if (nameBox != null) nameBox.setY(y + 22);
            button(g, x + 188, y + 22, 90, 18, "Renommer", true, mx, my, this::submitName);
            button(g, x + w - 120, y + 22, 110, 18, confirmLabel("disband", "Dissoudre"), true, mx, my, RED_DARK,
                confirm("disband", () -> send("disband", "")));
        } else {
            g.drawString(font, ellipsize("Chef et officiers recrutent et gèrent la réserve.", w - 140), x + 10, y + 27, MUTED, false);
            button(g, x + w - 120, y + 22, 110, 18, confirmLabel("leave", "Quitter la guilde"), true, mx, my, RED_DARK,
                confirm("leave", () -> send("leave", "")));
        }
    }

    private void renderMembers(GuiGraphics g, int mx, int my, int top, int bottom) {
        int x = px + 12, w = contentWidth(), y = top;
        boolean manage = bool(snapshot, "canManage"), leader = bool(snapshot, "isLeader");
        if (manage) {
            button(g, x + 116, y + 2, 80, 18, "Inviter", true, mx, my, this::invite);
            g.drawString(font, ellipsize("Recrute avec le numéro de hibou du joueur.", w - 206), x + 204, y + 7, MUTED, false);
            y += 26;
        } else {
            g.drawString(font, "Seuls le chef et les officiers peuvent recruter.", x, y + 4, MUTED, false);
            y += 18;
        }
        JsonArray members = arr(snapshot, "members");
        sectionTitle(g, x, y, "MEMBRES (" + members.size() + ")");
        y += 14;
        beginList(g, x, y, w, bottom, members.size() * 22);
        int ry = y - scroll;
        for (JsonElement e : members) {
            JsonObject m = e.getAsJsonObject();
            if (ry + 20 > y && ry < bottom) {
                boolean self = bool(m, "self");
                g.fill(x, ry, x + w, ry + 20, self ? CARD_HI : CARD);
                g.fill(x + 6, ry + 7, x + 12, ry + 13, bool(m, "online") ? GREEN : 0xFF555555);
                g.drawString(font, ellipsize(str(m, "name", "?") + (self ? " (toi)" : ""), w / 3), x + 18, ry + 6, TEXT, false);
                String role = str(m, "role", "MEMBER");
                g.drawString(font, roleLabel(role), x + w / 3 + 26, ry + 6, role.equals("LEADER") ? GOLD : role.equals("OFFICER") ? 0xFFE0C27A : MUTED, false);
                if (leader && !self && !role.equals("LEADER")) {
                    String uuid = str(m, "uuid", "");
                    boolean visible = ry >= y && ry + 20 <= bottom;
                    button(g, x + w - 172, ry + 2, 92, 16, role.equals("OFFICER") ? "Rétrograder" : "Promouvoir", visible, mx, my,
                        () -> send("promote", uuid));
                    button(g, x + w - 76, ry + 2, 72, 16, confirmLabel("kick" + uuid, "Exclure"), visible, mx, my, RED_DARK,
                        confirm("kick" + uuid, () -> send("kick", uuid)));
                }
            }
            ry += 22;
        }
        endList(g);
    }

    private void renderBank(GuiGraphics g, int mx, int my, int top, int bottom) {
        int x = px + 12, w = contentWidth(), y = top;
        boolean manage = bool(snapshot, "canManage");
        JsonArray bank = arr(snapshot, "bank");
        int slot = 18, cols = 9, rows = 6;
        int gridW = cols * slot, gridH = rows * slot;
        card(g, x, y, gridW + 12, gridH + 12);
        int gx = x + 6, gy = y + 6;
        for (int i = 0; i < cols * rows; i++) {
            int sx = gx + (i % cols) * slot, sy = gy + (i / cols) * slot;
            boolean hover = inside(mx, my, sx, sy, slot, slot);
            g.fill(sx, sy, sx + slot - 1, sy + slot - 1, hover ? 0xFF4A3A26 : 0xFF17120E);
            g.renderOutline(sx, sy, slot - 1, slot - 1, LINE);
            if (i >= bank.size()) continue;
            JsonObject o = bank.get(i).getAsJsonObject();
            ItemStack stack = stackOf(str(o, "id", ""), num(o, "count"));
            g.renderItem(stack, sx, sy);
            g.renderItemDecorations(font, stack, sx, sy);
            if (hover) {
                tooltip = new ArrayList<>();
                tooltip.add(Component.literal(str(o, "name", "?") + " ×" + num(o, "count")));
                if (manage) tooltip.add(Component.literal("Clic gauche : tout retirer • Clic droit : retirer 1").withColor(MUTED));
                int index = num(o, "slot");
                if (manage) {
                    hits.add(new Hit(sx, sy, slot, slot, () -> send("withdraw", index + ":64")));
                    rightHits.add(new Hit(sx, sy, slot, slot, () -> send("withdraw", index + ":1")));
                }
            }
        }
        int rx = x + gridW + 22, rw = w - gridW - 22;
        sectionTitle(g, rx, y, "RÉSERVE COMMUNE");
        g.drawString(font, bank.size() + " / 54 emplacements", rx, y + 14, TEXT, false);
        button(g, rx, y + 30, Math.min(rw, 180), 20, "Déposer l'objet en main", true, mx, my, () -> send("deposit", ""));
        int ty = y + 60;
        for (FormattedCharSequence line : font.split(Component.literal(manage
            ? "Tous les membres peuvent déposer. Clique sur un objet pour le retirer."
            : "Tous les membres peuvent déposer. Le retrait est réservé au chef et aux officiers."), rw)) {
            g.drawString(font, line, rx, ty, MUTED, false); ty += 10;
        }
        ty += 6;
        ItemStack auto = stackOf(str(snapshot, "autoDepositItem", "citizenguilds:raw_jade"), 1);
        g.renderItem(auto, rx, ty);
        for (FormattedCharSequence line : font.split(Component.literal("Ramassé automatiquement dans la réserve : " + auto.getHoverName().getString()), rw - 22)) {
            g.drawString(font, line, rx + 22, ty + 1, MUTED, false); ty += 10;
        }
    }

    private void renderDungeon(GuiGraphics g, int mx, int my, int top, int bottom) {
        int x = px + 12, w = contentWidth(), y = top;
        JsonObject d = obj(snapshot, "dungeon");
        boolean portal = bool(d, "portal"), active = bool(d, "active");
        card(g, x, y, w, 52);
        g.fill(x + 1, y + 1, x + 4, y + 51, portal ? GOLD : MUTED);
        g.drawString(font, "LA PORTE ANCIENNE", x + 12, y + 7, GOLD, false);
        if (portal) {
            g.drawString(font, "Ouverte en X " + num(d, "x") + "  Y " + num(d, "y") + "  Z " + num(d, "z"), x + 12, y + 21, TEXT, false);
            long closes = lng(d, "closesIn");
            g.drawString(font, closes >= 0 ? "Se referme dans " + closes + " min si personne n'entre." : "Une expédition l'utilise.", x + 12, y + 35, MUTED, false);
        } else if (!bool(d, "auto")) {
            g.drawString(font, "Aucune porte : l'apparition automatique est désactivée.", x + 12, y + 21, TEXT, false);
        } else {
            long next = lng(d, "nextIn");
            g.drawString(font, next > 0 ? "Prochaine apparition dans ~" + next + " min." : "Apparition imminente…", x + 12, y + 21, TEXT, false);
            g.drawString(font, "Toutes les " + num(d, "interval") + " min, une porte surgit dans la nature.", x + 12, y + 35, MUTED, false);
        }
        y += 58;
        card(g, x, y, w, 40);
        g.drawString(font, "EXPÉDITION", x + 12, y + 7, GOLD, false);
        if (active) {
            String state = str(d, "state", "EN COURS");
            int color = state.equals("VICTOIRE") ? GREEN : state.equals("ECHEC") ? RED : TEXT;
            g.drawString(font, ellipsize(str(d, "guild", "?") + (bool(d, "mine") ? " (ta guilde)" : "") + "  •  " + state
                + "  •  Vague " + num(d, "wave") + "/3  •  " + num(d, "alive") + "/" + num(d, "participants") + " en vie", w - 24), x + 12, y + 23, color, false);
        } else {
            g.drawString(font, "Libre : la première guilde à toucher la porte l'occupe.", x + 12, y + 23, TEXT, false);
        }
        y += 46;
        sectionTitle(g, x, y, "COMMENT ÇA MARCHE");
        y += 14;
        String[] rules = {
            "Clic droit sur la porte : ta guilde ouvre l'expédition et tu entres.",
            "Tes compagnons de guilde peuvent te rejoindre par la même porte.",
            "Trois salles : deux vagues, des pièges runiques, puis le Gardien des profondeurs.",
            "Une seule vie : mourir t'élimine de l'expédition.",
            "Victoire : " + num(d, "reward") + " minerai(s) de Jade par survivant. La porte se referme ensuite."
        };
        for (String r : rules) {
            if (y + 10 > bottom) break;
            g.drawString(font, ellipsize("• " + r, w), x + 4, y, MUTED, false);
            y += 12;
        }
        if (bool(snapshot, "hasGuild") && y + 12 <= bottom)
            g.drawString(font, "Ta guilde : " + num(snapshot, "dungeonWins") + " victoire(s), " + num(snapshot, "dungeonDeaths") + " élimination(s).", x + 4, y + 4, GOLD, false);
    }

    private void renderJournal(GuiGraphics g, int mx, int my, int top, int bottom) {
        int x = px + 12, w = contentWidth();
        JsonArray journal = arr(snapshot, "journal");
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (JsonElement e : journal) lines.addAll(font.split(Component.literal("• " + e.getAsString()), w - 12));
        beginList(g, x, top, w, bottom, lines.size() * 11 + 6);
        int ry = top + 4 - scroll;
        for (FormattedCharSequence line : lines) {
            if (ry + 10 > top && ry < bottom) g.drawString(font, line, x + 6, ry, MUTED, false);
            ry += 11;
        }
        if (lines.isEmpty()) g.drawString(font, "Aucune activité enregistrée.", x + 6, top + 4, MUTED, false);
        endList(g);
    }

    private void renderAdmin(GuiGraphics g, int mx, int my, int top, int bottom) {
        int x = px + 12, w = contentWidth(), y = top;
        JsonObject d = obj(snapshot, "dungeon");
        card(g, x, y, w, 86);
        g.drawString(font, "DONJONS AUTOMATIQUES", x + 10, y + 7, GOLD, false);
        boolean auto = bool(d, "auto");
        button(g, x + 10, y + 20, 150, 18, "Auto : " + (auto ? "ACTIVÉE" : "DÉSACTIVÉE"), true, mx, my, auto ? 0xFF2E4A2A : RED_DARK, () -> send("admin_autospawn", ""));
        button(g, x + 166, y + 20, 140, 18, "Faire apparaître", !bool(d, "portal") && !bool(d, "active"), mx, my, () -> send("admin_spawnnow", ""));
        if (bool(d, "active"))
            button(g, x + 312, y + 20, Math.min(130, w - 322), 18, confirmLabel("end", "Fermer l'expédition"), true, mx, my, RED_DARK, confirm("end", () -> send("admin_end", "")));
        else
            button(g, x + 312, y + 20, Math.min(130, w - 322), 18, "Retirer la porte", bool(d, "portal"), mx, my, () -> send("admin_removeportal", ""));
        g.drawString(font, "Intervalle", x + 10, y + 47, MUTED, false);
        button(g, x + 70, y + 43, 18, 16, "-", true, mx, my, () -> send("admin_interval", "-5"));
        String iv = num(d, "interval") + " min";
        g.drawString(font, iv, x + 108 - font.width(iv) / 2, y + 47, TEXT, false);
        button(g, x + 128, y + 43, 18, 16, "+", true, mx, my, () -> send("admin_interval", "5"));
        g.drawString(font, "Récompense", x + 166, y + 47, MUTED, false);
        button(g, x + 232, y + 43, 18, 16, "-", true, mx, my, () -> send("admin_reward", "-1"));
        String rw = num(d, "reward") + " Jade";
        g.drawString(font, rw, x + 272 - font.width(rw) / 2, y + 47, TEXT, false);
        button(g, x + 294, y + 43, 18, 16, "+", true, mx, my, () -> send("admin_reward", "1"));
        g.drawString(font, ellipsize(dungeonSummary(d), w - 20), x + 10, y + 64, TEXT, false);
        String note = str(d, "note", "");
        g.drawString(font, ellipsize(note.isEmpty() ? "Zone : X ≥ " + num(d, "minX") + ", Z ≥ " + num(d, "minZ") + ", Y ≥ " + num(d, "minY") : "Recherche : " + note, w - 20), x + 10, y + 75, MUTED, false);
        y += 92;
        JsonArray guilds = arr(snapshot, "guilds");
        sectionTitle(g, x, y, "GUILDES (" + guilds.size() + ")");
        y += 14;
        beginList(g, x, y, w, bottom, guilds.size() * 20);
        int ry = y - scroll;
        for (JsonElement e : guilds) {
            JsonObject o = e.getAsJsonObject();
            if (ry + 18 > y && ry < bottom) {
                g.fill(x, ry, x + w, ry + 18, CARD);
                String name = str(o, "name", "?");
                g.drawString(font, ellipsize(name + "  —  " + num(o, "members") + " membre(s), chef " + str(o, "leader", "?"), w - 100), x + 8, ry + 5, TEXT, false);
                boolean visible = ry >= y && ry + 18 <= bottom;
                button(g, x + w - 86, ry + 1, 82, 16, confirmLabel("adm" + name, "Dissoudre"), visible, mx, my, RED_DARK,
                    confirm("adm" + name, () -> send("admin_disband", name)));
            }
            ry += 20;
        }
        endList(g);
    }

    // ------------------------------------------------------------------ widgets

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean enabled, int mx, int my, Runnable action) {
        button(g, x, y, w, h, label, enabled, mx, my, 0, action);
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean enabled, int mx, int my, int tint, Runnable action) {
        if (w <= 4) return;
        boolean hover = enabled && inside(mx, my, x, y, w, h);
        int base = tint != 0 ? tint : 0xFF3A2C1C;
        int bg = !enabled ? 0xFF1E1813 : hover ? brighten(base) : base;
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, enabled ? 0x40FFFFFF : 0x10FFFFFF);
        g.renderOutline(x, y, w, h, !enabled ? LINE : hover ? GOLD : GOLD_DARK);
        String s = ellipsize(label, w - 6);
        g.drawString(font, s, x + (w - font.width(s)) / 2, y + (h - 8) / 2 + 1, enabled ? (hover ? 0xFFFFF4D6 : TEXT) : 0xFF6B5E4C, false);
        if (enabled) hits.add(new Hit(x, y, w, h, action));
    }

    private static int brighten(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 28), gr = Math.min(255, ((color >> 8) & 0xFF) + 22), b = Math.min(255, (color & 0xFF) + 14);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    private void card(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, CARD);
        g.renderOutline(x, y, w, h, LINE);
    }

    private void stat(GuiGraphics g, int x, int y, int w, String title, String value) {
        card(g, x, y, w, 42);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, GOLD_DARK);
        g.drawString(font, ellipsize(title, w - 12), x + 8, y + 8, MUTED, false);
        g.drawString(font, ellipsize(value, w - 12), x + 8, y + 24, GOLD, false);
    }

    private void sectionTitle(GuiGraphics g, int x, int y, String title) {
        g.drawString(font, title, x, y, GOLD, false);
        g.fill(x + font.width(title) + 6, y + 4, px + pw - 12, y + 5, LINE);
    }

    private int listTop, listBottom, listX, listW;

    private void beginList(GuiGraphics g, int x, int top, int w, int bottom, int contentHeight) {
        listX = x; listTop = top; listW = w; listBottom = bottom;
        maxScroll = Math.max(0, contentHeight - (bottom - top));
        scroll = Math.min(scroll, maxScroll);
        g.enableScissor(x, top, x + w, bottom);
    }

    private void endList(GuiGraphics g) {
        g.disableScissor();
        if (maxScroll > 0) {
            int track = listBottom - listTop;
            int thumb = Math.max(14, track * track / (track + maxScroll));
            int ty = listTop + (track - thumb) * scroll / maxScroll;
            g.fill(listX + listW + 3, listTop, listX + listW + 5, listBottom, LINE);
            g.fill(listX + listW + 3, ty, listX + listW + 5, ty + thumb, GOLD_DARK);
        }
    }

    // ------------------------------------------------------------------ helpers

    private String dungeonSummary(JsonObject d) {
        if (bool(d, "active")) return "Expédition en cours : " + str(d, "guild", "?") + " (vague " + num(d, "wave") + "/3).";
        if (bool(d, "portal")) return "Ouverte en X " + num(d, "x") + ", Y " + num(d, "y") + ", Z " + num(d, "z") + " — fonce !";
        if (!bool(d, "auto")) return "Aucune porte (apparition automatique désactivée).";
        long next = lng(d, "nextIn");
        return next > 0 ? "Fermée. Prochaine porte dans ~" + next + " min." : "Une porte va bientôt apparaître…";
    }

    private static String roleLabel(String role) {
        return switch (role) { case "LEADER" -> "Chef"; case "OFFICER" -> "Officier"; default -> "Membre"; };
    }

    private boolean isAvailable(String id) {
        boolean guild = bool(snapshot, "hasGuild");
        return switch (id) {
            case "members", "bank", "journal" -> guild;
            case "admin" -> bool(snapshot, "isAdmin");
            default -> true;
        };
    }

    private static ItemStack stackOf(String id, int count) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        Item item = key == null ? Items.BARRIER : BuiltInRegistries.ITEM.get(key);
        if (item == Items.AIR) item = Items.BARRIER;
        return new ItemStack(item, Math.max(1, count));
    }

    private String ellipsize(String s, int max) {
        if (s == null) return "";
        if (max <= 0) return "";
        if (font.width(s) <= max) return s;
        return font.plainSubstrByWidth(s, Math.max(0, max - font.width("…"))) + "…";
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean bool(JsonObject o, String k) {
        try { return o != null && o.has(k) && o.get(k).getAsBoolean(); } catch (Exception e) { return false; }
    }
    private static int num(JsonObject o, String k) {
        try { return o != null && o.has(k) ? o.get(k).getAsInt() : 0; } catch (Exception e) { return 0; }
    }
    private static long lng(JsonObject o, String k) {
        try { return o != null && o.has(k) ? o.get(k).getAsLong() : 0L; } catch (Exception e) { return 0L; }
    }
    private static String str(JsonObject o, String k, String def) {
        try { return o != null && o.has(k) ? o.get(k).getAsString() : def; } catch (Exception e) { return def; }
    }
    private static JsonArray arr(JsonObject o, String k) {
        try { return o != null && o.has(k) ? o.getAsJsonArray(k) : new JsonArray(); } catch (Exception e) { return new JsonArray(); }
    }
    private static JsonObject obj(JsonObject o, String k) {
        try { return o != null && o.has(k) ? o.getAsJsonObject(k) : new JsonObject(); } catch (Exception e) { return new JsonObject(); }
    }
}
