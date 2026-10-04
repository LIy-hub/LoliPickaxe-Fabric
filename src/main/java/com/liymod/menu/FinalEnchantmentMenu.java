package com.liymod.menu;

import java.util.LinkedHashMap;
import java.util.Map;
import com.liymod.compat.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import com.liymod.compat.ItemEnchantments;

public final class FinalEnchantmentMenu extends AbstractFinalToolMenu {
    public FinalEnchantmentMenu(int containerId, Inventory inventory, ToolMenuData data) {
        super(ModMenus.FINAL_ENCHANTMENT, containerId, inventory, data);
    }

    public Map<ResourceLocation, Integer> getEnchantments() {
        Map<ResourceLocation, Integer> values = new LinkedHashMap<>();
        ItemEnchantments enchantments = com.liymod.compat.LegacyComponents.getOrDefault(getOwnerStack(), DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        enchantments.entrySet().forEach(entry -> values.put(
                net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(entry.getKey()), entry.getValue()));
        return Map.copyOf(values);
    }
}
