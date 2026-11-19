package fr.citizen.guilds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Dark-fantasy guild dashboard. This first milestone is visual; server actions are not wired yet. */
public final class GuildManagementScreen extends Screen {
    private static final int OUTER=0xF00B0D13, SIDEBAR=0xFF11141D, PANEL=0xFF191E29;
    private static final int PANEL_ALT=0xFF202635, GOLD=0xFFE2B765, GOLD_DARK=0xFF80643A;
    private static final int WHITE=0xFFF4F0E8, MUTED=0xFF9CA6B8, LINE=0xFF343B49;
    private String tab="overview";

    public GuildManagementScreen() { super(Component.translatable("screen.citizenguilds.title")); }
    @Override public boolean isPauseScreen() { return false; }

    @Override protected void init() {
        int x=(width-650)/2, y=(height-390)/2;
        int sx=x+14, sy=y+104;
        nav("Vue d'ensemble","overview",sx,sy);
        nav("Membres","members",sx,sy+38);
        nav("Banque de guilde","bank",sx,sy+76);
        nav("Pierres de Jade","jade",sx,sy+114);
        nav("Activites","activities",sx,sy+152);
        nav("Journal","journal",sx,sy+190);
        nav("Commandes","help",sx,sy+228);
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("✕"), b -> onClose())
            .bounds(x+615,y+14,24,22).build());
    }

    private void nav(String label,String id,int x,int y) {
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal(label), b -> tab=id)
            .bounds(x,y,132,27).build());
    }

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        renderBackground(g,mouseX,mouseY,partialTick);
        int x=(width-650)/2, y=(height-390)/2;
        g.fill(x,y,x+650,y+390,OUTER);
        g.renderOutline(x,y,650,390,GOLD_DARK);
        g.fill(x+1,y+1,x+649,y+3,GOLD);
        g.fill(x+1,y+4,x+157,y+389,SIDEBAR);
        g.fill(x+157,y+4,x+158,y+389,LINE);

        g.drawString(font,"CITIZEN",x+18,y+18,GOLD,false);
        g.drawString(font,"G U I L D S",x+18,y+33,WHITE,false);
        g.drawString(font,"CHRONIQUES & COMPAGNIES",x+18,y+61,MUTED,false);
        g.fill(x+14,y+83,x+143,y+84,LINE);

        g.drawString(font,"ESPACE DE GUILDE",x+176,y+20,MUTED,false);
        g.drawString(font,"Le sanctuaire des compagnons",x+176,y+39,WHITE,false);
        g.drawString(font,"NEUTRE  •  AUCUNE GUILDE SELECTIONNEE",x+176,y+59,GOLD,false);
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
        g.drawString(font,"CITIZEN GUILDS  •  APERCU DE DEVELOPPEMENT",x+176,y+370,MUTED,false);
    }

    private void card(GuiGraphics g,int x,int y,int w,int h,String heading,String value,String detail) {
        g.fill(x,y,x+w,y+h,PANEL); g.renderOutline(x,y,w,h,LINE);
        g.fill(x+1,y+1,x+w-1,y+3,GOLD_DARK);
        g.drawString(font,heading,x+12,y+12,MUTED,false);
        g.drawString(font,value,x+12,y+34,GOLD,false);
        g.drawString(font,detail,x+12,y+h-18,MUTED,false);
    }
    private void section(GuiGraphics g,int x,int y,String title,String subtitle) {
        g.drawString(font,title,x,y,WHITE,false); g.drawString(font,subtitle,x,y+17,MUTED,false);
    }
    private void renderOverview(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+98;
        g.fill(a,b,a+460,b+94,PANEL_ALT); g.renderOutline(a,b,460,94,GOLD_DARK);
        g.drawString(font,"VOTRE HISTOIRE COMMENCE ICI",a+16,b+14,GOLD,false);
        g.drawString(font,"Fondez une guilde, rassemblez vos allies",a+16,b+37,WHITE,false);
        g.drawString(font,"et construisez votre legende sur CITIZEN.",a+16,b+54,MUTED,false);
        card(g,a,b+108,142,82,"MEMBRES","—","Effectif de la guilde");
        card(g,a+153,b+108,142,82,"PIERRES DE JADE","—","Tresor collectif");
        card(g,a+306,b+108,154,82,"BANQUE","—","Objets partages");
        section(g,a,b+213,"ACTIVITE RECENTE","Les evenements de votre guilde apparaitront ici.");
        g.fill(a,b+249,a+460,b+291,PANEL); g.renderOutline(a,b+249,460,42,LINE);
        g.drawString(font,"Aucune activite pour le moment.",a+12,b+265,MUTED,false);
    }
    private void renderMembers(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103; section(g,a,b,"MEMBRES DE LA GUILDE","Invitez des aventuriers et gerez leurs grades.");
        card(g,a,b+45,220,90,"EFFECTIF","—","Membres inscrits");
        card(g,a+235,b+45,225,90,"INVITATIONS","—","En attente de reponse");
        g.drawString(font,"La liste des membres sera affichee dans cette zone.",a,b+158,MUTED,false);
    }
    private void renderBank(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103; section(g,a,b,"BANQUE DE GUILDE","Un inventaire commun protege par les permissions.");
        card(g,a,b+45,220,90,"EMPLACEMENTS","—","Capacite du coffre");
        card(g,a+235,b+45,225,90,"OBJETS STOCKES","—","Ressources partagees");
        g.drawString(font,"Les depots et retraits seront traces dans le journal.",a,b+158,MUTED,false);
    }
    private void renderJade(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103; section(g,a,b,"TRESOR DE JADE","Les pierres gagnees en donjon alimenteront le stock de guilde.");
        card(g,a,b+45,220,90,"PIERRES COLLECTEES","—","Total de la guilde");
        card(g,a+235,b+45,225,90,"DERNIER DEPOT","—","Historique automatique");
    }
    private void renderActivities(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103; section(g,a,b,"ACTIVITES","Donjons, boss et contributions des compagnons.");
        card(g,a,b+45,220,90,"DONJONS","—","Participations");
        card(g,a+235,b+45,225,90,"BOSS VAINCUS","—","Succes de la guilde");
    }
    private void renderHelp(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103; section(g,a,b,"COMMANDES DE GUILDE","Toutes les fonctions du systeme sont disponibles en jeu.");
        String[] lines={
            "/guild create <nom>  - Fonder une guilde",
            "/guild invite <joueur>  - Inviter un joueur",
            "/guild accept  - Accepter une invitation",
            "/guild info  - Voir les membres et le tresor",
            "/guild deposit  - Deposer l objet tenu en main",
            "/guild withdraw <emplacement> [quantite]  - Retirer du coffre",
            "/guild promote <joueur>  - Basculer membre/officier (chef)",
            "/guild kick <joueur>  - Exclure un membre (chef)",
            "/guild leave  - Quitter sa guilde",
            "/guild disband  - Dissoudre sa guilde (chef)",
            "/guild jadeitem <namespace:objet>  - Configurer la Pierre de Jade (admin)"
        };
        int yy=b+43; for(String line:lines){g.fill(a,yy-3,a+460,yy+15,PANEL);g.drawString(font,line,a+9,yy,MUTED,false);yy+=21;}
    }

    private void renderJournal(GuiGraphics g,int x,int y) {
        int a=x+176,b=y+103; section(g,a,b,"JOURNAL DE GUILDE","Une trace des invitations, depots, retraits et promotions.");
        g.fill(a,b+45,a+460,b+235,PANEL); g.renderOutline(a,b+45,460,190,LINE);
        g.drawString(font,"Aucun evenement enregistre.",a+14,b+61,MUTED,false);
    }
}
