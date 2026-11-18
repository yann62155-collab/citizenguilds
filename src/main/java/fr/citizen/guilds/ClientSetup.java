package fr.citizen.guilds;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CitizenGuilds.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
 private static final KeyMapping OPEN_GUILD = new KeyMapping("key.citizenguilds.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.citizenguilds");
 private ClientSetup() {}
 @SubscribeEvent public static void registerKeyMappings(RegisterKeyMappingsEvent event) { event.register(OPEN_GUILD); }
 @SubscribeEvent public static void onClientTick(ClientTickEvent.Post event) {
  while (OPEN_GUILD.consumeClick()) {
   Minecraft minecraft = Minecraft.getInstance();
   if (minecraft.player != null && minecraft.screen == null) minecraft.setScreen(new GuildManagementScreen());
  }
 }
}
