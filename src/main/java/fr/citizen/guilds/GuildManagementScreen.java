package fr.citizen.guilds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Dark-fantasy guild interface with real server-command actions. */
public final class GuildManagementScreen extends Screen {
    private static final int OUTER=0xF00B0D13, SIDEBAR=0xFF11141D, PANEL=0xFF191E29;
    private static final int PANEL_ALT=0xFF202635, GOLD=0xFFE2B765, GOLD_DARK=0xFF80643A;
    private static final int WHITE=0xFFF4F0E8, MUTED=0xFF9CA6B8, LINE=0xFF343B49;
    private String tab="overview";
    private EditBox primary, secondary;
    private static JsonObject snapshot = new JsonObject();
    public static void acceptSnapshot(String json) { try { snapshot = JsonParser.parseString(json).getAsJsonObject(); } catch (Exception ignored) { snapshot = new JsonObject(); } }

    public GuildManagementScreen() { super(Component.translatable("screen.citizenguilds.title")); }
    @Override public boolean isPauseScreen() { return false; }

    @Override protected void init() {
        int x=(width-650)/2, y=(height-390)/2;
        int sx=x+14, sy=y+104;
        GuildNetwork.requestSync();
        nav("Vue d'ensemble","overview",sx,sy);
        nav("Membres & rôles","members",sx,sy+32);
        nav("Réserve","bank",sx,sy+64);
        nav("Jade & équipement","jade",sx,sy+96);
        nav("Activités & donjons","activities",sx,sy+128);
        nav("Journal","journal",sx,sy+160);
        nav("Aide & commandes","help",sx,sy+192);
        nav("Administration","admin",sx,sy+224);
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose()).bounds(x+615,y+14,24,22).build());
        buildTabControls(x,y);
    }

    private void nav(String label,String id,int x,int y) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            tab=id;
            clearWidgets();
            init();
        }).bounds(x,y,132,27).build());
    }

    private void buildTabControls(int x,int y) {
        int a=x+176, b=y+98;
        switch(tab) {
            case "overview" -> {
                primary=field(a+16,b+68,205,"Nom de la guilde");
                action("Fonder ma guilde",a+232,b+67,210,()->run("guild create \"" + safe(primary.getValue()) + "\""));
                secondary=field(a,b+258,205,"Nouveau nom de guilde");
                action("Renommer",a+215,b+257,140,()->run("guild rename \"" + safe(secondary.getValue()) + "\""));
                action("Actualiser",a+365,b+257,95,()->GuildNetwork.requestSync());
            }
            case "members" -> {
                primary=field(a,b+208,205,"Numéro de hibou du joueur");
                action("Recruter",a+215,b+207,105,()->run("guild invite "+positiveNumber(primary.getValue())));
                action("Accepter",a+326,b+207,64,()->run("guild accept"));
                action("Refuser",a+394,b+207,66,()->run("guild refuse"));
                secondary=field(a,b+247,205,"Pseudo membre (gestion)");
                action("Promouvoir / rétrograder",a+215,b+246,150,()->run("guild promote "+safe(secondary.getValue())));
                action("Exclure",a+371,b+246,89,()->run("guild kick "+safe(secondary.getValue())));
                action("Quitter la guilde",a,b+286,140,()->run("guild leave"));
                action("Dissoudre (chef)",a+148,b+286,150,()->run("guild disband"));
                action("Actualiser les membres",a+306,b+286,154,()->run("guild info"));
            }
            case "bank" -> {
                action("Déposer l'objet en main",a,b+208,190,()->run("guild deposit"));
                primary=field(a,b+248,90,"Emplacement");
                secondary=field(a+100,b+248,90,"Quantité");
                action("Retirer",a+200,b+247,100,()->run("guild withdraw "+number(primary.getValue(),1)+" "+number(secondary.getValue(),64)));
                action("Voir la réserve",a+310,b+247,150,()->GuildNetwork.requestSync());
            }
            case "journal" -> action("Actualiser le journal",a,b+300,170,()->GuildNetwork.requestSync());
            case "activities" -> action("Voir les informations",a,b+250,170,()->run("guild info"));
            case "jade" -> action("Actualiser les ressources",a,b+250,190,()->GuildNetwork.requestSync());
            case "help" -> {
                action("Créer une guilde",a,b+295,145,()->{tab="overview";clearWidgets();init();});
                action("Afficher les membres",a+153,b+295,160,()->run("guild info"));
            }
            case "admin" -> {
                primary=field(a,b+208,205,"Nom exact de la guilde");
                action("Lister les guildes",a+215,b+207,145,()->run("guild admin list"));
                action("Dissoudre la guilde",a,b+247,180,()->run("guild admin disband "+safe(primary.getValue())));
            }
        }
    }

    private EditBox field(int x,int y,int w,String hint) {
        EditBox box=new EditBox(font,x,y,w,20,Component.literal(hint));
        box.setHint(Component.literal(hint));
        addRenderableWidget(box);
        return box;
    }
    private void action(String label,int x,int y,int w,Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,22).build());
    }
    private void run(String command) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null && !command.contains("null") && !command.endsWith(" ")) {
            mc.player.connection.sendCommand(command);
            GuildNetwork.requestSync();
        }
    }
    private String safe(String value) {
        String trimmed=value==null?"":value.trim();
        return trimmed.matches("[\\p{L}\\p{N}_ -]{3,24}")?trimmed:"";
    }
    private String positiveNumber(String value) {
        try {
            int parsed=Integer.parseInt(value.trim());
            return parsed > 0 ? Integer.toString(parsed) : "0";
        } catch(Exception ignored) { return "0"; }
    }
    private String number(String value,int fallback) {
        try { return Integer.toString(Math.max(1,Math.min(64,Integer.parseInt(value.trim())))); }
        catch(Exception ignored) { return Integer.toString(fallback); }
    }

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        renderBackground(g,mouseX,mouseY,partialTick);
        int x=(width-650)/2, y=(height-390)/2;
        g.fill(x,y,x+650,y+390,OUTER); g.renderOutline(x,y,650,390,GOLD_DARK);
        g.fill(x+1,y+1,x+649,y+3,GOLD); g.fill(x+1,y+4,x+157,y+389,SIDEBAR);
        g.fill(x+157,y+4,x+158,y+389,LINE);
        g.drawString(font,"CITIZEN",x+18,y+18,GOLD,false);
        g.drawString(font,"GUILDES",x+18,y+34,WHITE,false);
        g.drawString(font,"GESTION DE COMPAGNIE",x+18,y+61,MUTED,false);
        g.fill(x+14,y+82,x+143,y+83,LINE);
        g.drawString(font,"NAVIGATION",x+18,y+91,MUTED,false);
        String[] navIds={"overview","members","bank","jade","activities","journal","help","admin"};
        int[] navY={104,136,168,200,232,264,296,328};
        for(int i=0;i<navIds.length;i++) if(tab.equals(navIds[i])) {
            g.fill(x+12,y+navY[i],x+145,y+navY[i]+27,PANEL_ALT);
            g.fill(x+12,y+navY[i],x+15,y+navY[i]+27,GOLD);
        }
        g.drawString(font,"ESPACE DE GUILDE",x+176,y+18,MUTED,false);
        g.drawString(font,snapshot.has("hasGuild") && snapshot.get("hasGuild").getAsBoolean() ? snapshot.get("name").getAsString() : "Aucune guilde sélectionnée",x+176,y+38,WHITE,false);
        g.drawString(font,tabTitle(),x+176,y+60,GOLD,false);
        g.fill(x+174,y+78,x+636,y+79,LINE);
        super.render(g,mouseX,mouseY,partialTick);
        int activeNavY = switch(tab) {
            case "members" -> y+136;
            case "bank" -> y+168;
            case "jade" -> y+200;
            case "activities" -> y+232;
            case "journal" -> y+264;
            case "help" -> y+296;
            case "admin" -> y+328;
            default -> y+104;
        };
        g.fill(x+12,activeNavY,x+15,activeNavY+26,GOLD);
        switch(tab) {
            case "members" -> renderMembers(g,x,y);
            case "bank" -> renderBank(g,x,y);
            case "jade" -> renderJade(g,x,y);
            case "activities" -> renderActivities(g,x,y);
            case "journal" -> renderJournal(g,x,y);
            case "help" -> renderHelp(g,x,y);
            case "admin" -> renderAdmin(g,x,y);
            default -> renderOverview(g,x,y);
        }
        // Keep the footer inside the sidebar: the old footer crossed the bottom action row.
        g.drawString(font,"CITIZEN GUILDS",x+18,y+370,GOLD_DARK,false);
    }
    private String tabTitle() {
        return switch(tab) {
            case "members" -> "MEMBRES ET RESPONSABILITÉS";
            case "bank" -> "RÉSERVE COMMUNE";
            case "jade" -> "MINERAIS ET ÉQUIPEMENT";
            case "activities" -> "ACTIVITÉS ET DONJONS";
            case "journal" -> "HISTORIQUE DE LA GUILDE";
            case "help" -> "GUIDE DU CHEF ET DES MEMBRES";
            case "admin" -> "OUTILS D'ADMINISTRATION";
            default -> "TABLEAU DE BORD";
        };
    }

    private void card(GuiGraphics g,int x,int y,int w,int h,String heading,String value,String detail) {
        g.fill(x,y,x+w,y+h,PANEL); g.renderOutline(x,y,w,h,LINE); g.fill(x+1,y+1,x+w-1,y+3,GOLD_DARK);
        g.drawString(font,heading,x+12,y+12,MUTED,false); g.drawString(font,value,x+12,y+34,GOLD,false);
        g.drawString(font,detail,x+12,y+h-18,MUTED,false);
    }
    private void section(GuiGraphics g,int x,int y,String title,String subtitle) {
        g.drawString(font,title,x,y,WHITE,false); g.drawString(font,subtitle,x,y+17,MUTED,false);
    }
    private void renderOverview(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+98;
        g.fill(a,b,a+460,b+94,PANEL_ALT); g.renderOutline(a,b,460,94,GOLD_DARK);
        g.drawString(font,"VOTRE HISTOIRE COMMENCE ICI",a+16,b+14,GOLD,false);
        g.drawString(font,snapshot.has("hasGuild") && snapshot.get("hasGuild").getAsBoolean() ? "Votre guilde : "+snapshot.get("name").getAsString() : "Fondez une guilde et rassemblez vos allies.",a+16,b+37,WHITE,false);
        g.drawString(font,snapshot.has("hasGuild") && snapshot.get("hasGuild").getAsBoolean() ? "Membres : "+snapshot.get("memberCount").getAsInt() : "Saisissez un nom puis utilisez le bouton pour la creer.",a+16,b+54,MUTED,false);
        card(g,a,b+108,142,82,"MEMBRES","/guild info","Effectif et grades");
        card(g,a+153,b+108,142,82,"ARMURE DE JADE","RARE","Butin des donjons");
        card(g,a+306,b+108,154,82,"RESERVE","PARTAGEE","Stock de ressources");
        section(g,a,b+213,"ACTIONS RAPIDES","Les commandes sont executees par le serveur.");
    }
    private void renderMembers(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103;
        section(g,a,b,"MEMBRES DE LA GUILDE",snapshot.has("name") ? snapshot.get("name").getAsString()+" • "+snapshot.get("memberCount").getAsInt()+" membre(s)" : "Aucune guilde active");
        if (snapshot.has("hasInvitation") && snapshot.get("hasInvitation").getAsBoolean()) {
            String invitedGuild = snapshot.has("invitationGuild") ? snapshot.get("invitationGuild").getAsString() : "Guilde inconnue";
            int invitedMembers = snapshot.has("invitationGuildMembers") ? snapshot.get("invitationGuildMembers").getAsInt() : 0;
            g.fill(a,b+29,a+460,b+66,PANEL_ALT);
            g.drawString(font,"INVITATION EN ATTENTE",a+10,b+34,GOLD,false);
            g.drawString(font,"Rejoindre "+invitedGuild+" • "+invitedMembers+" membre(s) — Accepter ou refuser ci-dessous",a+10,b+49,WHITE,false);
        } else {
            g.fill(a,b+29,a+460,b+66,PANEL);
            g.drawString(font,"Aucune invitation en attente.",a+10,b+43,MUTED,false);
        }
        com.google.gson.JsonArray members=snapshot.has("members")?snapshot.getAsJsonArray("members"):new com.google.gson.JsonArray();
        int yy=b+73;
        for(int i=0;i<Math.min(5,members.size());i++){
            com.google.gson.JsonObject m=members.get(i).getAsJsonObject();
            g.fill(a,yy,a+460,yy+22,PANEL);
            g.drawString(font,m.get("name").getAsString(),a+10,yy+7,WHITE,false);
            g.drawString(font,m.get("role").getAsString(),a+330,yy+7,GOLD,false);
            yy+=25;
        }
        if(members.isEmpty() && !(snapshot.has("hasInvitation") && snapshot.get("hasInvitation").getAsBoolean()))g.drawString(font,"Aucun membre à afficher. Créez une guilde ou rejoignez-en une.",a,b+82,MUTED,false);
    }
    private void renderBank(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103;
        section(g,a,b,"RESERVE DE RESSOURCES","Stock de minerais et matériaux • Dépôts membres • Retraits chef/officier");
        com.google.gson.JsonArray items=snapshot.has("bank")?snapshot.getAsJsonArray("bank"):new com.google.gson.JsonArray();
        int yy=b+43;
        for(int i=0;i<Math.min(8,items.size());i++){
            com.google.gson.JsonObject item=items.get(i).getAsJsonObject();
            g.fill(a,yy,a+460,yy+22,PANEL);
            g.drawString(font,item.get("slot").getAsString()+". "+item.get("name").getAsString(),a+10,yy+7,WHITE,false);
            g.drawString(font,"x"+item.get("count").getAsString(),a+410,yy+7,GOLD,false);
            yy+=25;
        }
        if(items.isEmpty())g.drawString(font,"La réserve de ressources est vide.",a,b+52,MUTED,false);
    }
    private void renderJade(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103;
        section(g,a,b,"PIERRE DE JADE — OBJETS DISPONIBLES","La ressource et les quatre pièces d'armure sont enregistrées dans le mod.");
        String item=snapshot.has("autoDepositItem")?snapshot.get("autoDepositItem").getAsString():"citizenguilds:raw_jade";
        card(g,a,b+38,220,78,"MINERAI BRUT","raw_jade","Butin de coffres de donjon");
        card(g,a+235,b+38,225,78,"LINGOT DE JADE","jade_ingot","Cuisson du minerai brut");
        g.drawString(font,"RECETTES D'ARMURE",a,b+132,GOLD,false);
        g.drawString(font,"Casque • Plastron • Jambières • Bottes",a,b+151,WHITE,false);
        g.drawString(font,"Minerai brut : butin de donjon (12 % par coffre ciblé).",a,b+178,MUTED,false);
        g.drawString(font,"Cuisson : four ou haut-fourneau. Armures : établi, lingots de Jade.",a,b+196,MUTED,false);
        g.drawString(font,"Dépôt automatique configuré : "+item,a,b+222,MUTED,false);
        g.drawString(font,"La Pierre de Jade est un minerai de fabrication, jamais une monnaie.",a,b+244,GOLD,false);
    }
    private void renderActivities(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103;
        section(g,a,b,"ACTIVITES","Indicateurs actuels de la guilde");
        int members=snapshot.has("memberCount")?snapshot.get("memberCount").getAsInt():0;
        int bank=snapshot.has("bank")?snapshot.getAsJsonArray("bank").size():0;
        int journal=snapshot.has("journal")?snapshot.getAsJsonArray("journal").size():0;
        long deposited=snapshot.has("totalDepositedItems")?snapshot.get("totalDepositedItems").getAsLong():0L;
        card(g,a,b+45,142,90,"MEMBRES",Integer.toString(members),"Effectif actuel");
        card(g,a+153,b+45,142,90,"DEPOTS",Long.toString(deposited),"Objets deposes");
        card(g,a+306,b+45,154,90,"RESERVE",Integer.toString(bank)+"/54","Emplacements occupes");
        int wins=snapshot.has("dungeonWins")?snapshot.get("dungeonWins").getAsInt():0;
        int deaths=snapshot.has("dungeonDeaths")?snapshot.get("dungeonDeaths").getAsInt():0;
        card(g,a,b+145,220,65,"DONJONS GAGNES",Integer.toString(wins),"Victoires de guilde");
        card(g,a+235,b+145,225,65,"ELIMINATIONS",Integer.toString(deaths),"Morts en donjon");
        g.drawString(font,"Journal : "+journal+" entree(s) conservee(s).",a,b+222,MUTED,false);
    }
    private void renderHelp(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103; section(g,a,b,"COMMANDES DE GUILDE","Les boutons executent les commandes cote serveur.");
        String[] lines={"/guild create <nom> - Fonder une guilde","/guild invite <numero_hibou> - Recruter via CitizenHibou",
            "/guild accept - Accepter une invitation","/guild refuse - Refuser une invitation","/guild info - Voir les membres","/guild deposit - Deposer l'objet en main dans la réserve",
            "/guild withdraw <slot> [quantite] - Retirer de la réserve","/guild promote <joueur> - Changer le grade",
            "/guild kick <joueur> - Exclure un membre","/guild leave - Quitter","/guild disband - Dissoudre",
            "/guild dungeon status - Etat de la session","/guild dungeon portal create - Placer la porte (OP)",
            "/guild dungeon start <guilde> - Ouvrir le donjon (OP)"};
        int yy=b+43; for(String line:lines){g.fill(a,yy-2,a+460,yy+14,PANEL);g.drawString(font,line,a+9,yy,MUTED,false);yy+=17;}
    }
    
    private void renderAdmin(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103;
        section(g,a,b,"ADMINISTRATION","Les commandes restent protegees par les permissions serveur.");
        boolean admin=snapshot.has("isAdmin") && snapshot.get("isAdmin").getAsBoolean();
        g.fill(a,b+43,a+460,b+75,PANEL);
        g.drawString(font,admin ? "Acces administrateur confirme." : "Acces en lecture limite ; les actions necessitent le niveau OP 2.",a+10,b+55,admin?GOLD:MUTED,false);
        com.google.gson.JsonArray guilds=snapshot.has("guilds")?snapshot.getAsJsonArray("guilds"):new com.google.gson.JsonArray();
        int yy=b+88;
        for(int i=0;i<Math.min(8,guilds.size());i++){
            com.google.gson.JsonObject guild=guilds.get(i).getAsJsonObject();
            g.fill(a,yy,a+460,yy+22,PANEL);
            g.drawString(font,guild.get("name").getAsString(),a+10,yy+7,WHITE,false);
            g.drawString(font,guild.get("members").getAsString()+" membre(s)",a+330,yy+7,GOLD,false);
            yy+=25;
        }
        if(guilds.isEmpty())g.drawString(font,"Aucune guilde enregistree.",a,b+100,MUTED,false);
    }

    private void renderJournal(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103;
        section(g,a,b,"JOURNAL DE GUILDE","Historique des actions de la guilde");
        g.fill(a,b+45,a+460,b+190,PANEL); g.renderOutline(a,b+45,460,145,LINE);
        com.google.gson.JsonArray logs=snapshot.has("journal")?snapshot.getAsJsonArray("journal"):new com.google.gson.JsonArray();
        int yy=b+57;
        for(int i=0;i<Math.min(7,logs.size());i++){
            g.drawString(font,"• "+logs.get(i).getAsString(),a+10,yy,MUTED,false); yy+=18;
        }
        if(logs.isEmpty())g.drawString(font,"Aucune activité enregistrée.",a+12,b+61,MUTED,false);
    }
}
