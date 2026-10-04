package com.liymod.compat;

import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class LegacyComponents {
    private LegacyComponents() { }
    @SuppressWarnings("unchecked")
    public static <T> T getOrDefault(ItemStack stack,DataComponents key,T fallback) {
        return switch(key) {
            case CUSTOM_DATA -> (T) CustomData.get(stack);
            case ENCHANTMENTS -> (T) ItemEnchantments.getEnchantmentsForCrafting(stack);
            case ATTRIBUTE_MODIFIERS -> stack.hasTag() && stack.getTag().contains("AttributeModifiers",Tag.TAG_LIST)
                    ? (T) new ItemAttributeModifiers(stack.getTag().getList("AttributeModifiers",Tag.TAG_COMPOUND).copy()):fallback;
            default -> fallback;
        };
    }
    public static void set(ItemStack stack,DataComponents key,Object value) {
        switch(key) {
            case CUSTOM_DATA -> stack.setTag(((CustomData)value).copyTag());
            case ATTRIBUTE_MODIFIERS -> stack.getOrCreateTag().put("AttributeModifiers",((ItemAttributeModifiers)value).tag().copy());
            case CUSTOM_MODEL_DATA -> stack.getOrCreateTag().putInt("CustomModelData",((CustomModelData)value).value());
            case UNBREAKABLE -> {
                stack.getOrCreateTag().putBoolean("Unbreakable",true);
                if(!((Unbreakable)value).showInTooltip()) stack.getOrCreateTag().putInt("HideFlags",stack.getOrCreateTag().getInt("HideFlags")|4);
            }
            default -> throw new IllegalArgumentException("Unsupported legacy component mutation: "+key);
        }
    }
    public static void remove(ItemStack stack,DataComponents key) {
        if(key!=DataComponents.CUSTOM_MODEL_DATA) throw new IllegalArgumentException("Unsupported removal: "+key);
        stack.removeTagKey("CustomModelData");
    }
}
