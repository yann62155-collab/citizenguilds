package fr.citizen.guilds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** Première base visuelle. Les actions connectées aux données serveur restent à implémenter. */
public final class GuildManagementScreen extends Screen {
 private static final int BACKGROUND=0xE9101219, PANEL=0xF01B1E29, GOLD=0xFFE0B45C, MUTED=0xFF9CA3B4, WHITE=0xFFF4F0E8;
 public GuildManagementScreen() { super(Component.translatable("screen.citizenguilds.title")); }
 @Override public boolean isPauseScreen() { return false; }
 @Override protected void init() {
  int left=(width-520)/2, top=(height-300)/2, y=top+72;
  addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Vue d'ensemble"), b -> {}).bounds(left+16,y,112,22).build());
  addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Membres"), b -> {}).bounds(left+134,y,82,22).build());
  addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Banque"), b -> {}).bounds(left+222,y,72,22).build());
  addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Activites"), b -> {}).bounds(left+300,y,82,22).build());
  addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Journal"), b -> {}).bounds(left+388,y,72,22).build());
  addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Fermer"), b -> onClose()).bounds(left+454,top+12,50,20).build());
 }
 @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
  renderBackground(g,mouseX,mouseY,partialTick);
  int x=(width-520)/2,y=(height-300)/2;
  g.fill(x,y,x+520,y+300,BACKGROUND); g.renderOutline(x,y,520,300,GOLD); g.fill(x+1,y+1,x+519,y+4,GOLD);
  g.drawString(font,"CITIZEN  /  GUILDES",x+18,y+15,GOLD,false);
  g.drawString(font,"LES CHRONIQUES DE VOTRE COMPAGNIE",x+18,y+31,MUTED,false);
  g.fill(x+14,y+59,x+506,y+60,0xFF45404A);
  super.render(g,mouseX,mouseY,partialTick);
  g.drawString(font,"GUILDE",x+22,y+112,GOLD,false);
  g.drawString(font,"Votre guilde sera presente ici",x+22,y+130,WHITE,false);
  g.drawString(font,"Creez une guilde ou rejoignez une aventure pour commencer.",x+22,y+147,MUTED,false);
  stat(g,x+18,y+181,"MEMBRES","--"); stat(g,x+184,y+181,"PIERRES DE JADE","--"); stat(g,x+350,y+181,"RESSOURCES","--");
  g.drawString(font,"Prototype visuel — donnees serveur a connecter",x+18,y+276,MUTED,false);
 }
 private void stat(GuiGraphics g,int x,int y,String title,String value) {
  g.fill(x,y,x+152,y+70,PANEL); g.renderOutline(x,y,152,70,0xFF514A42);
  g.drawString(font,title,x+10,y+10,MUTED,false); g.drawString(font,value,x+10,y+32,GOLD,false);
 }
}
