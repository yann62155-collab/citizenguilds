package fr.citizen.guilds;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid = CitizenGuilds.MOD_ID, value = Dist.CLIENT)
public final class ClientTickHandler {
 private ClientTickHandler() {}
 @SubscribeEvent public static void onClientTick(ClientTickEvent.Post event) {
  while (ClientSetup.OPEN_GUILD.consumeClick()) {
   Minecraft minecraft = Minecraft.getInstance();
   if (minecraft.player != null && minecraft.screen == null) minecraft.setScreen(new GuildManagementScreen());
  }
 }
}
