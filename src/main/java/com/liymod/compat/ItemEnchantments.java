package com.liymod.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class ItemEnchantments {
    public static final ItemEnchantments EMPTY=new ItemEnchantments(Map.of());
    private final Map<Enchantment,Integer> levels;
    private ItemEnchantments(Map<Enchantment,Integer> levels) { this.levels=new HashMap<>(levels); }
    public int getLevel(Enchantment enchantment) { return levels.getOrDefault(enchantment,0); }
    public int size() { return levels.size(); }
    public Set<Map.Entry<Enchantment,Integer>> entrySet() { return levels.entrySet(); }
    public void set(Enchantment enchantment,int level) { if(level==0) levels.remove(enchantment); else levels.put(enchantment,level); }
    public static ItemEnchantments getEnchantmentsForCrafting(ItemStack stack) { return new ItemEnchantments(EnchantmentHelper.getEnchantments(stack)); }
    public static void updateEnchantments(ItemStack stack,Consumer<ItemEnchantments> update) {
        ItemEnchantments mutable=getEnchantmentsForCrafting(stack);update.accept(mutable);EnchantmentHelper.setEnchantments(mutable.levels,stack);
    }
}
