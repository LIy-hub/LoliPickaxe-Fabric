package com.liymod.item;


import com.liymod.LiyMod;
import com.liymod.tool.ModToolMaterials;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static com.liymod.LiyMod.MOD_ID;

public final class ModItems {
    private ModItems() {
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, name), item);
    }

    public static final Item LOLI = registerItem("loli", new Item(new Item.Settings().fireproof()));
    public static final Item LOLI_PICKAXE = registerItem(
            "loli_pickaxe",
            new LoliPickaxeItem(
                    ModToolMaterials.LOLI,
                    Integer.MAX_VALUE,
                    Float.POSITIVE_INFINITY,
                    new Item.Settings().fireproof().maxCount(1)
            )
    );

    public static void registerModItems() {
        LiyMod.LOGGER.info("Registering items for {}", MOD_ID);
    }
}
