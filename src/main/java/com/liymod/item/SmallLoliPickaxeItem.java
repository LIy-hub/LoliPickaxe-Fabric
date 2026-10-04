package com.liymod.item;

import com.liymod.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** The original upgradeable pickaxe stage that precedes the final Loli Pickaxe. */
public final class SmallLoliPickaxeItem extends Item {
    private static final String CURRENT_MINING_RADIUS_KEY = "LoliCurrentDiggingRange";

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

    public static int getCurrentMiningRadius(ItemStack stack) {
        int maximumRadius = Math.max(0, (getMiningRange(stack) - 1) / 2);
        int stored = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getIntOr(CURRENT_MINING_RADIUS_KEY, 0);
        return Math.clamp(stored, 0, maximumRadius);
    }

    public static int cycleMiningRadius(ItemStack stack) {
        int maximumRadius = Math.max(0, (getMiningRange(stack) - 1) / 2);
        int next = (getCurrentMiningRadius(stack) + 1) % (maximumRadius + 1);
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                stack,
                tag -> tag.putInt(CURRENT_MINING_RADIUS_KEY, next)
        );
        return next;
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

    public static void refreshEnchantments(ItemStack stack, ServerLevel level) {
        if (!(stack.getItem() instanceof SmallLoliPickaxeItem)) {
            return;
        }
        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> fortune = registry.getOrThrow(Enchantments.FORTUNE);
        Holder<Enchantment> looting = registry.getOrThrow(Enchantments.LOOTING);
        int targetLevel = getFortuneLevel(stack);
        if (EnchantmentHelper.getItemEnchantmentLevel(fortune, stack) != targetLevel
                || EnchantmentHelper.getItemEnchantmentLevel(looting, stack) != targetLevel) {
            EnchantmentHelper.updateEnchantments(stack, mutable -> {
                mutable.set(fortune, targetLevel);
                mutable.set(looting, targetLevel);
            });
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
        refreshEnchantments(stack, level);
        super.inventoryTick(stack, level, entity, slot);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown() && getHitRange(stack) > 0) {
            attackNearbyHostiles((ServerLevel) level, player, stack);
            player.resetAttackStrengthTicker();
            return InteractionResult.SUCCESS_SERVER;
        }
        if (!player.isShiftKeyDown() && getMiningRange(stack) > 0) {
            int radius = cycleMiningRadius(stack);
            int sideLength = radius * 2 + 1;
            player.sendSystemMessage(Component.translatableWithFallback(
                    "loliPickaxe.range",
                    "Mining range changed to %s x %s x %s",
                    sideLength,
                    sideLength,
                    sideLength
            ));
            level.playSound(
                    null,
                    player.blockPosition(),
                    ModSounds.LOLI_SUCCESS,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    private static void attackNearbyHostiles(ServerLevel level, Player player, ItemStack stack) {
        double radius = getHitRange(stack) / 2.0D;
        AABB area = player.getBoundingBox().inflate(radius + 0.7D, radius + 0.1D, radius + 0.7D);
        for (Entity target : level.getEntities(player, area, SmallLoliPickaxeItem::isRangeAttackTarget)) {
            player.attack(target);
        }
    }

    private static boolean isRangeAttackTarget(Entity entity) {
        if (!entity.isAttackable()
                || entity instanceof Player
                || entity instanceof ArmorStand
                || entity instanceof AmbientCreature) {
            return false;
        }
        return !(entity instanceof PathfinderMob) || entity instanceof Enemy;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!isCorrectToolForDrops(stack, state)) {
            return 1.0F;
        }
        // Range mode must fire as one action instead of waiting for the origin block's
        // ordinary progressive mining animation to finish first.
        return getCurrentMiningRadius(stack) > 0 ? Float.MAX_VALUE : getMiningSpeed(stack);
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
