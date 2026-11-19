package fr.citizen.guilds;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/** Adds a small chance of Jade to dungeon treasure chests only. */
@EventBusSubscriber(modid = CitizenGuilds.MOD_ID)
public final class JadeDungeonLoot {
    private JadeDungeonLoot() {}

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        String namespace = event.getName().getNamespace();
        String path = event.getName().getPath();
        if (!isDungeonChest(namespace, path)) return;

        LootPool jadePool = LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1))
            .when(LootItemRandomChanceCondition.randomChance(0.12F))
            .add(LootItem.lootTableItem(CitizenJadeItems.RAW_JADE.get())
                .setWeight(1)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
            .build();
        event.getTable().addPool(jadePool);
    }

    private static boolean isDungeonChest(String namespace, String path) {
        if (!path.startsWith("chests/")) return false;
        if (namespace.equals("minecraft")) {
            return path.equals("chests/simple_dungeon")
                || path.equals("chests/abandoned_mineshaft")
                || path.equals("chests/stronghold_corridor")
                || path.equals("chests/stronghold_crossing")
                || path.equals("chests/ancient_city");
        }
        return path.contains("dungeon") || path.contains("catacomb") || path.contains("crypt");
    }
}
