package com.liymod.item;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;

/** A stackable original-mod upgrade material whose zero-based tier is stored on the stack. */
public final class UpgradeItem extends Item {
    private static final String TIER_KEY = "LoliUpgradeTier";

    public enum Type {
        COAL(10, "LoliDodge"),
        IRON(10, "LoliDiggingSpeed"),
        GOLD(7, "LoliAttackDamage"),
        REDSTONE(4, "LoliAttackSpeed"),
        LAPIS(6, "LoliFortuneLevel"),
        DIAMOND(6, "LoliDiggingLevel"),
        EMERALD(5, "LoliDiggingRange"),
        OBSIDIAN(10, "LoliAntiInjury"),
        GLOW(3, "LoliBuff"),
        QUARTZ(3, "LoliHitRange"),
        NETHER_STAR(5, "LoliBackpackPage"),
        AUTO_FURNACE(1, "LoliAutoFurnace"),
        FLY(1, "LoliFly"),
        ENTITY_SOUL(7, null);

        private final int tierCount;
        private final String smallPickaxeDataKey;

        Type(int tierCount, String smallPickaxeDataKey) {
            this.tierCount = tierCount;
            this.smallPickaxeDataKey = smallPickaxeDataKey;
        }

        public int tierCount() {
            return tierCount;
        }

        public int maxTier() {
            return tierCount - 1;
        }

        public String smallPickaxeDataKey() {
            return smallPickaxeDataKey;
        }

        public boolean appliesToSmallPickaxe() {
            return smallPickaxeDataKey != null;
        }
    }

    private final Type type;

    public UpgradeItem(Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public Type type() {
        return type;
    }

    public int tierCount() {
        return type.tierCount();
    }

    public int maxTier() {
        return type.maxTier();
    }

    public int getTier(ItemStack stack) {
        if (!stack.is(this)) {
            return 0;
        }
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return Math.clamp(data.copyTag().getIntOr(TIER_KEY, 0), 0, maxTier());
    }

    public void setTier(ItemStack stack, int tier) {
        if (!stack.is(this)) {
            throw new IllegalArgumentException("Cannot set an upgrade tier on a different item");
        }
        int clampedTier = Math.clamp(tier, 0, maxTier());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TIER_KEY, clampedTier));
        updateTerminalModel(stack, clampedTier);
    }

    public ItemStack createStack(int tier) {
        ItemStack stack = new ItemStack(this);
        setTier(stack, tier);
        return stack;
    }

    @Override
    public ItemStack getDefaultInstance() {
        return createStack(0);
    }

    @Override
    public Component getName(ItemStack stack) {
        int tier = getTier(stack);
        Component tierName = Component.translatable(
                tier == maxTier() ? "item.loliMaterial.end" : "item.loliMaterial." + tier
        );
        return Component.translatable("item.loliMaterialFormat", super.getName(stack), tierName);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
        TooltipFlag flag
    ) {
        int tier = getTier(stack);
        if (tier < maxTier()) {
            Component current = Component.translatable("item.loliMaterial." + tier);
            Component next = Component.translatable(
                    tier == maxTier() - 1 ? "item.loliMaterial.end" : "item.loliMaterial." + (tier + 1)
            );
            Component maximum = Component.translatable("item.loliMaterial.end");
            tooltip.accept(Component.translatable("item.loliMaterial.recipe", current, next, maximum));
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }

    private void updateTerminalModel(ItemStack stack, int tier) {
        if (type == Type.ENTITY_SOUL && tier == maxTier()) {
            stack.set(
                    DataComponents.CUSTOM_MODEL_DATA,
                    new CustomModelData(List.of(), List.of(true), List.of("end"), List.of())
            );
        } else {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }
}
