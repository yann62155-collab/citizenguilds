package fr.citizen.guilds;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
@EventBusSubscriber(modid = CitizenGuilds.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
 public static final KeyMapping OPEN_GUILD = new KeyMapping("key.citizenguilds.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.citizenguilds");
 private ClientSetup() {}
 @SubscribeEvent public static void registerKeyMappings(RegisterKeyMappingsEvent event) { event.register(OPEN_GUILD); }
}
