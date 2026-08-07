package com.liymod.item;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;

/** The original upgradeable pickaxe stage that precedes the final Loli Pickaxe. */
public final class SmallLoliPickaxeItem extends Item {
    public SmallLoliPickaxeItem(Properties properties) {
        super(properties);
    }

    public static int getUpgradeTier(ItemStack stack, UpgradeItem.Type type) {
        if (!type.appliesToSmallPickaxe()) {
            return -1;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.clamp(tag.getIntOr(type.smallPickaxeDataKey(), -1), -1, type.maxTier());
    }

    public static void setUpgradeTier(ItemStack stack, UpgradeItem.Type type, int tier) {
        if (!(stack.getItem() instanceof SmallLoliPickaxeItem) || !type.appliesToSmallPickaxe()) {
            throw new IllegalArgumentException("Upgrade cannot be applied to this stack");
        }
        int clampedTier = Math.clamp(tier, 0, type.maxTier());
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putInt(type.smallPickaxeDataKey(), clampedTier)
        );
        refreshDerivedComponents(stack);
    }

    public static ItemStack createFullyUpgraded(Item item) {
        ItemStack stack = new ItemStack(item);
        for (UpgradeItem.Type type : UpgradeItem.Type.values()) {
            if (type.appliesToSmallPickaxe()) {
                setUpgradeTier(stack, type, type.maxTier());
            }
        }
        return stack;
    }

    public static boolean isFullyUpgraded(ItemStack stack) {
        if (!(stack.getItem() instanceof SmallLoliPickaxeItem)) {
            return false;
        }
        for (UpgradeItem.Type type : UpgradeItem.Type.values()) {
            if (type.appliesToSmallPickaxe() && getUpgradeTier(stack, type) != type.maxTier()) {
                return false;
            }
        }
        return true;
    }

    public static float getMiningSpeed(ItemStack stack) {
        int tier = getUpgradeTier(stack, UpgradeItem.Type.IRON);
        return tier < 0 ? 1.0F : 4 << tier;
    }

    public static int getHarvestLevel(ItemStack stack) {
        return switch (getUpgradeTier(stack, UpgradeItem.Type.DIAMOND)) {
            case 0 -> 1;
            case 1 -> 3;
            case 2 -> 7;
            case 3 -> 13;
            case 4 -> 21;
            case 5 -> 32;
            default -> -1;
        };
    }

    public static double getAttackDamage(ItemStack stack) {
        int tier = getUpgradeTier(stack, UpgradeItem.Type.GOLD);
        return tier < 0 ? 0.0D : 4.0D + Math.pow(2.0D, Math.pow(2.0D, tier));
    }

    public static double getAttackSpeed(ItemStack stack) {
        int tier = getUpgradeTier(stack, UpgradeItem.Type.REDSTONE);
        return tier < 0 ? 0.0D : 2 << tier;
    }

    public static int getFortuneLevel(ItemStack stack) {
        int tier = getUpgradeTier(stack, UpgradeItem.Type.LAPIS);
        return tier < 0 ? 0 : 1 << tier;
    }

    public static int getLootingLevel(ItemStack stack) {
        return getFortuneLevel(stack);
    }

    public static int getMiningRange(ItemStack stack) {
        int tier = getUpgradeTier(stack, UpgradeItem.Type.EMERALD);
        return tier < 0 ? 0 : tier * 2 + 3;
    }

    public static double getDodgeChance(ItemStack stack) {
        int tier = getUpgradeTier(stack, UpgradeItem.Type.COAL);
        return tier < 0 ? 0.0D : (tier + 1) / 10.0D;
    }

    public static double getDamageReturnChance(ItemStack stack) {
        int tier = getUpgradeTier(stack, UpgradeItem.Type.OBSIDIAN);
        return tier < 0 ? 0.0D : (tier + 1) / 10.0D;
    }

    public static int getBuffLevel(ItemStack stack) {
        int tier = getUpgradeTier(stack, UpgradeItem.Type.GLOW);
        return tier < 0 ? 0 : tier + 1;
    }

    public static int getHitRange(ItemStack stack) {
        int tier = getUpgradeTier(stack, UpgradeItem.Type.QUARTZ);
        return tier < 0 ? 0 : tier * 10 + 6;
    }

    public static int getStoragePages(ItemStack stack) {
        int tier = getUpgradeTier(stack, UpgradeItem.Type.NETHER_STAR);
        return tier < 0 ? 0 : 2 << tier;
    }

    public static boolean hasAutoFurnace(ItemStack stack) {
        return getUpgradeTier(stack, UpgradeItem.Type.AUTO_FURNACE) == 0;
    }

    public static boolean canFly(ItemStack stack) {
        return getUpgradeTier(stack, UpgradeItem.Type.FLY) == 0;
    }

    public static void refreshDerivedComponents(ItemStack stack) {
        if (!(stack.getItem() instanceof SmallLoliPickaxeItem)) {
            return;
        }
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Item.BASE_ATTACK_DAMAGE_ID,
                                getAttackDamage(stack),
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                Item.BASE_ATTACK_SPEED_ID,
                                getAttackSpeed(stack),
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
        if (!modifiers.equals(stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY))) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
        }
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        refreshDerivedComponents(stack);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        refreshDerivedComponents(stack);
        super.inventoryTick(stack, level, entity, slot);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return isCorrectToolForDrops(stack, state) ? getMiningSpeed(stack) : 1.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        int harvestLevel = getHarvestLevel(stack);
        if (harvestLevel < 0 || !state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return false;
        }
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return harvestLevel >= 3;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
            return harvestLevel >= 2;
        }
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            return harvestLevel >= 1;
        }
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        appendLevel(tooltip, stack, UpgradeItem.Type.COAL, getDodgeChance(stack));
        appendLevel(tooltip, stack, UpgradeItem.Type.IRON, getMiningSpeed(stack));
        appendLevel(tooltip, stack, UpgradeItem.Type.GOLD, getAttackDamage(stack));
        appendLevel(tooltip, stack, UpgradeItem.Type.REDSTONE, getAttackSpeed(stack));
        appendLevel(tooltip, stack, UpgradeItem.Type.LAPIS, getFortuneLevel(stack));
        appendLevel(tooltip, stack, UpgradeItem.Type.DIAMOND, getHarvestLevel(stack));
        appendLevel(tooltip, stack, UpgradeItem.Type.EMERALD, getMiningRange(stack));
        appendLevel(tooltip, stack, UpgradeItem.Type.OBSIDIAN, getDamageReturnChance(stack));
        appendLevel(tooltip, stack, UpgradeItem.Type.GLOW, getBuffLevel(stack));
        appendLevel(tooltip, stack, UpgradeItem.Type.QUARTZ, getHitRange(stack));
        appendLevel(tooltip, stack, UpgradeItem.Type.NETHER_STAR, getStoragePages(stack));
        appendLevel(tooltip, stack, UpgradeItem.Type.AUTO_FURNACE, 0);
        appendLevel(tooltip, stack, UpgradeItem.Type.FLY, 0);
        super.appendHoverText(stack, context, display, tooltip, flag);
    }

    private static void appendLevel(
            Consumer<Component> tooltip,
            ItemStack stack,
            UpgradeItem.Type type,
            Number value
    ) {
        if (getUpgradeTier(stack, type) >= 0) {
            tooltip.accept(Component.translatable("smallLoliPickaxe." + type.smallPickaxeDataKey(), value));
        }
    }
}
