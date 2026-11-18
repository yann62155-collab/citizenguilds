package fr.citizen.guilds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
@Mod(CitizenGuilds.MOD_ID)
public final class CitizenGuilds {
 public static final String MOD_ID = "citizenguilds";
 public CitizenGuilds(IEventBus modBus) {
  modBus.addListener(ClientSetup::registerKeyMappings);
  net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ClientSetup.class);
 }
}
