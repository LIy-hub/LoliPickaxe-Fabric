package com.liymod.item;


import com.liymod.LiyMod;
import com.liymod.tool.ModToolMaterials;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;

import java.util.function.Function;

import static com.liymod.LiyMod.MOD_ID;

public final class ModItems {
    private ModItems() {
    }

    private static Item registerItem(
            String name,
            Function<Item.Settings, Item> factory,
            Item.Settings settings
    ) {
        Identifier id = Identifier.of(MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        Item item = factory.apply(settings.registryKey(key));
        return Registry.register(Registries.ITEM, id, item);
    }

    public static final Item LOLI = registerItem(
            "loli",
            Item::new,
            new Item.Settings().fireproof()
    );
    public static final Item LOLI_PICKAXE = registerItem(
            "loli_pickaxe",
            LoliPickaxeItem::new,
            new Item.Settings()
                    .fireproof()
                    .maxCount(1)
                    .pickaxe(
                            ModToolMaterials.LOLI,
                            Integer.MAX_VALUE,
                            Float.POSITIVE_INFINITY
                    )
                    .component(DataComponentTypes.UNBREAKABLE, Unit.INSTANCE)
    );

    public static void registerModItems() {
        LiyMod.LOGGER.info("Registering items for {}", MOD_ID);
    }
}
