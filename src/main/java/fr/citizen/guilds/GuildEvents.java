package fr.citizen.guilds;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/** Automatically routes the configured resource into the guild resource reserve. */
@EventBusSubscriber(modid = CitizenGuilds.MOD_ID)
public final class GuildEvents {
    private GuildEvents() {}

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        GuildSavedData data = GuildSavedData.get(player.getServer());
        String pickedId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getOriginalStack().getItem()).toString();
        if (!pickedId.equals(data.getAutoDepositItemId())) return;
        int pickedUp = event.getOriginalStack().getCount() - event.getCurrentStack().getCount();
        if (pickedUp <= 0) return;
        data.autoDepositResource(player, pickedUp);
    }
}
