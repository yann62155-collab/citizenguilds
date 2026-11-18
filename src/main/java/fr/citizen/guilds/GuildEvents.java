package fr.citizen.guilds;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Periodically converts the configured Jade item from player inventories into guild currency. */
@EventBusSubscriber(modid = CitizenGuilds.MOD_ID)
public final class GuildEvents {
    private GuildEvents() {}
    @SubscribeEvent public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        GuildSavedData data=GuildSavedData.get(player.getServer());
        String jadeId=data.jadeItemId();
        if(jadeId.isEmpty() || data.guildOf(player)==null) return;
        int amount=0;
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            ItemStack stack=player.getInventory().getItem(i);
            if(!stack.isEmpty() && jadeId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                amount+=stack.getCount(); stack.setCount(0);
            }
        }
        if(amount>0) data.addJade(player,amount);
    }
}
