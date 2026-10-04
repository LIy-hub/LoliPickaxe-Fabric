package com.liymod.menu;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class FinalEnchantmentMenu extends AbstractFinalToolMenu {
    public FinalEnchantmentMenu(int containerId, Inventory inventory, ToolMenuData data) {
        super(ModMenus.FINAL_ENCHANTMENT, containerId, inventory, data);
    }

    public Map<Identifier, Integer> getEnchantments() {
        Map<Identifier, Integer> values = new LinkedHashMap<>();
        ItemEnchantments enchantments = getOwnerStack().getOrDefault(
                DataComponents.ENCHANTMENTS,
                ItemEnchantments.EMPTY
        );
        enchantments.entrySet().forEach(entry -> entry.getKey().unwrapKey().ifPresent(key ->
                values.put(key.identifier(), entry.getIntValue())));
        return Map.copyOf(values);
    }
}
