package com.liymod.item;

import com.liymod.LiyMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.liymod.LiyMod.MOD_ID;

public final class ModItemGroup {
    public static final ItemGroup LIYMOD_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(MOD_ID, "liymod"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemgroup.liymod"))
                    .icon(() -> new ItemStack(ModItems.LOLI_PICKAXE))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.LOLI_PICKAXE);
                        entries.add(ModItems.LOLI);
                    })
                    .build()
    );

    private ModItemGroup() {
    }

    public static void registerModItemGroup() {
        LiyMod.LOGGER.info("Registering item group for {}", MOD_ID);
    }
}
