package com.liymod.item;


import com.liymod.LiyMod;
import com.liymod.tool.ModToolMaterials;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;

import static com.liymod.LiyMod.MOD_ID;

public final class ModItems {
    private ModItems() {
    }

    private static Item registerItem(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static final Item LOLI = registerItem(
            "loli",
            Item::new,
            new Item.Properties().fireResistant()
    );
    public static final Item LOLI_PICKAXE = registerItem(
            "loli_pickaxe",
            LoliPickaxeItem::new,
            new Item.Properties()
                    .fireResistant()
                    .stacksTo(1)
                    .pickaxe(ModToolMaterials.LOLI, Integer.MAX_VALUE, Float.POSITIVE_INFINITY)
                    .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
    );

    public static void registerModItems() {
        LiyMod.LOGGER.info("Registering items for {}", MOD_ID);
    }
}
