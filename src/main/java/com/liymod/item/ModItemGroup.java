package com.liymod.item;

import com.liymod.LiyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import static com.liymod.LiyMod.MOD_ID;

public final class ModItemGroup {
    public static final CreativeModeTab LIYMOD_GROUP = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MOD_ID, "liymod"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemgroup.liymod"))
                    .icon(() -> new ItemStack(ModItems.LOLI_PICKAXE))
                    .displayItems((displayContext, entries) -> ModItems.CREATIVE_TAB_ITEMS.forEach(entries::accept))
                    .build()
    );

    private ModItemGroup() {
    }

    public static void registerModItemGroup() {
        LiyMod.LOGGER.info("Registering item group for {}", MOD_ID);
    }
}
