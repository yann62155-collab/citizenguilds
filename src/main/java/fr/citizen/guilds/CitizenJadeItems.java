package fr.citizen.guilds;

import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Jade is a dungeon reward used to craft high-tier armor. */
public final class CitizenJadeItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CitizenGuilds.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, CitizenGuilds.MOD_ID);

    public static final DeferredHolder<Item, Item> RAW_JADE =
        ITEMS.register("raw_jade", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> JADE_INGOT =
        ITEMS.register("jade_ingot", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JADE_ARMOR =
        ARMOR_MATERIALS.register("jade", () -> new ArmorMaterial(
            Map.of(
                ArmorItem.Type.HELMET, 3,
                ArmorItem.Type.CHESTPLATE, 8,
                ArmorItem.Type.LEGGINGS, 6,
                ArmorItem.Type.BOOTS, 3
            ),
            18,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            () -> Ingredient.of(JADE_INGOT.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(CitizenGuilds.MOD_ID, "jade"))),
            2.5F,
            0.1F
        ));

    public static final DeferredHolder<Item, Item> JADE_HELMET = ITEMS.register("jade_helmet",
        () -> new ArmorItem(JADE_ARMOR, ArmorItem.Type.HELMET,
            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(36))));
    public static final DeferredHolder<Item, Item> JADE_CHESTPLATE = ITEMS.register("jade_chestplate",
        () -> new ArmorItem(JADE_ARMOR, ArmorItem.Type.CHESTPLATE,
            new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(36))));
    public static final DeferredHolder<Item, Item> JADE_LEGGINGS = ITEMS.register("jade_leggings",
        () -> new ArmorItem(JADE_ARMOR, ArmorItem.Type.LEGGINGS,
            new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(36))));
    public static final DeferredHolder<Item, Item> JADE_BOOTS = ITEMS.register("jade_boots",
        () -> new ArmorItem(JADE_ARMOR, ArmorItem.Type.BOOTS,
            new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(36))));

    private CitizenJadeItems() {}
}
