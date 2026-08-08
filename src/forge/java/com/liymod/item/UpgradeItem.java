package com.liymod.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class UpgradeItem extends Item {
    private static final String TIER_KEY = "LoliUpgradeTier";

    public enum Type {
        COAL(10, "LoliDodge"), IRON(10, "LoliDiggingSpeed"), GOLD(7, "LoliAttackDamage"),
        REDSTONE(4, "LoliAttackSpeed"), LAPIS(6, "LoliFortuneLevel"), DIAMOND(6, "LoliDiggingLevel"),
        EMERALD(5, "LoliDiggingRange"), OBSIDIAN(10, "LoliAntiInjury"), GLOW(3, "LoliBuff"),
        QUARTZ(3, "LoliHitRange"), NETHER_STAR(5, "LoliBackpackPage"),
        AUTO_FURNACE(1, "LoliAutoFurnace"), FLY(1, "LoliFly"), ENTITY_SOUL(7, null);

        private final int tiers;
        private final String pickaxeKey;

        Type(int tiers, String pickaxeKey) { this.tiers = tiers; this.pickaxeKey = pickaxeKey; }
        public int tierCount() { return tiers; }
        public int maxTier() { return tiers - 1; }
        public String pickaxeKey() { return pickaxeKey; }
        public boolean appliesToSmallPickaxe() { return pickaxeKey != null; }
    }

    private final Type type;

    public UpgradeItem(Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public Type type() { return type; }
    public int maxTier() { return type.maxTier(); }

    public int getTier(ItemStack stack) {
        return stack.is(this) ? Mth.clamp(stack.getOrCreateTag().getInt(TIER_KEY), 0, type.maxTier()) : 0;
    }

    public void setTier(ItemStack stack, int tier) {
        int value = Mth.clamp(tier, 0, type.maxTier());
        stack.getOrCreateTag().putInt(TIER_KEY, value);
        if (type == Type.ENTITY_SOUL && value == type.maxTier()) {
            stack.getOrCreateTag().putInt("CustomModelData", 1);
        } else {
            stack.getOrCreateTag().remove("CustomModelData");
        }
    }

    public ItemStack createStack(int tier) {
        ItemStack stack = new ItemStack(this);
        setTier(stack, tier);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        int tier = getTier(stack);
        Component tierName = Component.translatable(tier == type.maxTier() ? "item.loliMaterial.end" : "item.loliMaterial." + tier);
        return Component.translatable("item.loliMaterialFormat", super.getName(stack), tierName);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int tier = getTier(stack);
        if (tier < type.maxTier()) {
            Component current = Component.translatable("item.loliMaterial." + tier);
            Component next = Component.translatable(tier == type.maxTier() - 1 ? "item.loliMaterial.end" : "item.loliMaterial." + (tier + 1));
            tooltip.add(Component.translatable("item.loliMaterial.recipe", current, next, Component.translatable("item.loliMaterial.end")));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
