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
        nav("Membres","members",sx,sy+38);
        nav("Réserve de ressources","bank",sx,sy+76);
        nav("Armure de Jade","jade",sx,sy+114);
        nav("Activites","activities",sx,sy+152);
        nav("Journal","journal",sx,sy+190);
        nav("Commandes","help",sx,sy+228);
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
                action("Fonder ma guilde",a+232,b+67,210,()->run("guild create "+safe(primary.getValue())));
                action("Voir ma guilde",a,b+315,140,()->GuildNetwork.requestSync());
            }
            case "members" -> {
                primary=field(a,b+208,205,"Pseudo du joueur");
                action("Inviter",a+215,b+207,105,()->run("guild invite "+safe(primary.getValue())));
                action("Accepter invitation",a+326,b+207,134,()->run("guild accept"));
                secondary=field(a,b+247,205,"Pseudo membre");
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
            case "jade" -> action("Voir les recettes",a,b+250,170,()->GuildNetwork.requestSync());
            case "help" -> {
                action("Créer une guilde",a,b+295,145,()->{tab="overview";clearWidgets();init();});
                action("Afficher les membres",a+153,b+295,160,()->run("guild info"));
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
        if(mc.player!=null && !command.contains("null") && !command.endsWith(" ")) mc.player.connection.sendCommand(command);
    }
    private String safe(String value) {
        String trimmed=value==null?"":value.trim();
        return trimmed.matches("[A-Za-z0-9_\\-]{1,24}")?trimmed:"";
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
        g.drawString(font,"G U I L D S",x+18,y+33,WHITE,false);
        g.drawString(font,"CHRONIQUES & COMPAGNIES",x+18,y+61,MUTED,false);
        g.fill(x+14,y+83,x+143,y+84,LINE);
        g.drawString(font,"ESPACE DE GUILDE",x+176,y+20,MUTED,false);
        g.drawString(font,snapshot.has("hasGuild") && snapshot.get("hasGuild").getAsBoolean() ? snapshot.get("name").getAsString() : "Le sanctuaire des compagnons",x+176,y+39,WHITE,false);
        g.drawString(font,"ACTIONS CONNECTEES AU SERVEUR",x+176,y+59,GOLD,false);
        g.fill(x+174,y+78,x+636,y+79,LINE);
        super.render(g,mouseX,mouseY,partialTick);
        switch(tab) {
            case "members" -> renderMembers(g,x,y);
            case "bank" -> renderBank(g,x,y);
            case "jade" -> renderJade(g,x,y);
            case "activities" -> renderActivities(g,x,y);
            case "journal" -> renderJournal(g,x,y);
            case "help" -> renderHelp(g,x,y);
            default -> renderOverview(g,x,y);
        }
        g.drawString(font,"CITIZEN GUILDS  •  INTERFACE CONNECTEE",x+176,y+370,MUTED,false);
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
        com.google.gson.JsonArray members=snapshot.has("members")?snapshot.getAsJsonArray("members"):new com.google.gson.JsonArray();
        int yy=b+43;
        for(int i=0;i<Math.min(8,members.size());i++){
            com.google.gson.JsonObject m=members.get(i).getAsJsonObject();
            g.fill(a,yy,a+460,yy+22,PANEL);
            g.drawString(font,m.get("name").getAsString(),a+10,yy+7,WHITE,false);
            g.drawString(font,m.get("role").getAsString(),a+330,yy+7,GOLD,false);
            yy+=25;
        }
        if(members.isEmpty())g.drawString(font,"Aucun membre a afficher. Actualisez les donnees.",a,b+52,MUTED,false);
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
        section(g,a,b,"MINERAIS & FABRICATION","Ressource configurée pour le dépôt automatique");
        String item=snapshot.has("autoDepositItem")?snapshot.get("autoDepositItem").getAsString():"citizenguilds:raw_jade";
        card(g,a,b+45,220,90,"RESSOURCE CONFIGUREE",item,"Choisie par un administrateur");
        card(g,a+235,b+45,225,90,"FABRICATION","LINGOT","Four / haut-fourneau puis armure");
        g.drawString(font,"Le ramassage de cette ressource alimente automatiquement la réserve de ressources.",a,b+158,MUTED,false);
    }
    private void renderActivities(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103;
        section(g,a,b,"ACTIVITES","Indicateurs actuels de la guilde");
        int members=snapshot.has("memberCount")?snapshot.get("memberCount").getAsInt():0;
        int bank=snapshot.has("bank")?snapshot.getAsJsonArray("bank").size():0;
        int journal=snapshot.has("journal")?snapshot.getAsJsonArray("journal").size():0;
        card(g,a,b+45,142,90,"MEMBRES",Integer.toString(members),"Effectif actuel");
        card(g,a+153,b+45,142,90,"STOCK",Integer.toString(bank)+"/54","Emplacements occupes");
        card(g,a+306,b+45,154,90,"JOURNAL",Integer.toString(journal),"Evenements conserves");
        g.drawString(font,"Les statistiques se mettent a jour avec la synchronisation serveur.",a,b+158,MUTED,false);
    }
    private void renderHelp(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103; section(g,a,b,"COMMANDES DE GUILDE","Les boutons executent les commandes cote serveur.");
        String[] lines={"/guild create <nom> - Fonder une guilde","/guild invite <joueur> - Inviter un joueur",
            "/guild accept - Accepter une invitation","/guild info - Voir les membres","/guild deposit - Deposer l'objet en main dans la réserve",
            "/guild withdraw <slot> [quantite] - Retirer de la réserve","/guild promote <joueur> - Changer le grade",
            "/guild kick <joueur> - Exclure un membre","/guild leave - Quitter","/guild disband - Dissoudre"};
        int yy=b+43; for(String line:lines){g.fill(a,yy-3,a+460,yy+15,PANEL);g.drawString(font,line,a+9,yy,MUTED,false);yy+=21;}
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
        if(logs.isEmpty())g.drawString(font,"Aucune activite enregistree.",a+12,b+61,MUTED,false);
    }
}
