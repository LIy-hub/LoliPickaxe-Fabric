package com.liymod.item;

import com.liymod.registry.ModContent;
import com.liymod.config.LogicalEnchantments;
import com.liymod.mixin.PlayerAccessor;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class SmallLoliPickaxeItem extends PickaxeItem {
    private static final String RADIUS_KEY = "LoliCurrentDiggingRange";

    public SmallLoliPickaxeItem(Properties properties) {
        super(LoliToolTier.INSTANCE, 0, -2.8F, properties);
    }

    public static int getUpgradeTier(ItemStack stack, UpgradeItem.Type type) {
        if (!(stack.getItem() instanceof SmallLoliPickaxeItem) || !type.appliesToSmallPickaxe()) return -1;
        return stack.getOrCreateTag().contains(type.pickaxeKey())
                ? Mth.clamp(stack.getOrCreateTag().getInt(type.pickaxeKey()), 0, type.maxTier())
                : -1;
    }

    public static void setUpgradeTier(ItemStack stack, UpgradeItem.Type type, int tier) {
        if (!(stack.getItem() instanceof SmallLoliPickaxeItem) || !type.appliesToSmallPickaxe()) return;
        stack.getOrCreateTag().putInt(type.pickaxeKey(), Mth.clamp(tier, 0, type.maxTier()));
        refreshEnchantments(stack);
    }

    public static ItemStack fullyUpgraded(Item item) {
        ItemStack stack = new ItemStack(item);
        for (UpgradeItem.Type type : UpgradeItem.Type.values()) if (type.appliesToSmallPickaxe()) setUpgradeTier(stack, type, type.maxTier());
        return stack;
    }

    public static boolean isFullyUpgraded(ItemStack stack) {
        if (!(stack.getItem() instanceof SmallLoliPickaxeItem)) return false;
        for (UpgradeItem.Type type : UpgradeItem.Type.values()) if (type.appliesToSmallPickaxe() && getUpgradeTier(stack, type) != type.maxTier()) return false;
        return true;
    }

    public static float miningSpeed(ItemStack stack) { int t = getUpgradeTier(stack, UpgradeItem.Type.IRON); return t < 0 ? 1.0F : 4 << t; }
    public static int harvestLevel(ItemStack stack) {
        return switch (getUpgradeTier(stack, UpgradeItem.Type.DIAMOND)) {
            case 0 -> 1; case 1 -> 3; case 2 -> 7; case 3 -> 13; case 4 -> 21; case 5 -> 32;
            default -> -1;
        };
    }
    public static double attackDamage(ItemStack stack) { int t = getUpgradeTier(stack, UpgradeItem.Type.GOLD); return t < 0 ? 0.0D : 4.0D + Math.pow(2.0D, Math.pow(2.0D, t)); }
    public static double attackSpeed(ItemStack stack) { int t = getUpgradeTier(stack, UpgradeItem.Type.REDSTONE); return t < 0 ? 0.0D : 2 << t; }
    public static int fortune(ItemStack stack) { int t = getUpgradeTier(stack, UpgradeItem.Type.LAPIS); return t < 0 ? 0 : 1 << t; }
    public static int miningRange(ItemStack stack) { int t = getUpgradeTier(stack, UpgradeItem.Type.EMERALD); return t < 0 ? 0 : t * 2 + 3; }
    public static double dodge(ItemStack stack) { int t = getUpgradeTier(stack, UpgradeItem.Type.COAL); return t < 0 ? 0.0D : (t + 1) / 10.0D; }
    public static double reflect(ItemStack stack) { int t = getUpgradeTier(stack, UpgradeItem.Type.OBSIDIAN); return t < 0 ? 0.0D : (t + 1) / 10.0D; }
    public static int buff(ItemStack stack) { int t = getUpgradeTier(stack, UpgradeItem.Type.GLOW); return t < 0 ? 0 : t + 1; }
    public static int hitRange(ItemStack stack) { int t = getUpgradeTier(stack, UpgradeItem.Type.QUARTZ); return t < 0 ? 0 : t * 10 + 6; }
    public static int storagePages(ItemStack stack) { int t = getUpgradeTier(stack, UpgradeItem.Type.NETHER_STAR); return t < 0 ? 0 : 2 << t; }
    public static boolean autoFurnace(ItemStack stack) { return getUpgradeTier(stack, UpgradeItem.Type.AUTO_FURNACE) == 0; }
    public static boolean canFly(ItemStack stack) { return getUpgradeTier(stack, UpgradeItem.Type.FLY) == 0; }

    public static int radius(ItemStack stack) {
        int max = Math.max(0, (miningRange(stack) - 1) / 2);
        return Mth.clamp(stack.getOrCreateTag().getInt(RADIUS_KEY), 0, max);
    }

    public static int cycleRadius(ItemStack stack) {
        int max = Math.max(0, (miningRange(stack) - 1) / 2);
        int next = (radius(stack) + 1) % (max + 1);
        stack.getOrCreateTag().putInt(RADIUS_KEY, next);
        return next;
    }

    private static void refreshEnchantments(ItemStack stack) {
        int level = fortune(stack);
        if (stack.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE) != level)
            LogicalEnchantments.setLevel(stack, Enchantments.BLOCK_FORTUNE, level);
        if (stack.getEnchantmentLevel(Enchantments.MOB_LOOTING) != level)
            LogicalEnchantments.setLevel(stack, Enchantments.MOB_LOOTING, level);
        stack.getOrCreateTag().putBoolean("Unbreakable", true);
        stack.setDamageValue(0);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        refreshEnchantments(stack);
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player.isShiftKeyDown() && hitRange(stack) > 0) {
            AABB area = player.getBoundingBox().inflate(hitRange(stack) / 2.0D + 0.7D);
            PlayerAccessor accessor = (PlayerAccessor) player;
            int savedStrength = accessor.liymod$getAttackStrengthTicker();
            for (Entity target : ((ServerLevel) level).getEntities(player, area, SmallLoliPickaxeItem::validRangeTarget)) {
                accessor.liymod$setAttackStrengthTicker(savedStrength);
                player.attack(target);
            }
            player.resetAttackStrengthTicker();
            return InteractionResultHolder.success(stack);
        }
        if (!level.isClientSide && !player.isShiftKeyDown() && miningRange(stack) > 0) {
            int radius = cycleRadius(stack);
            int side = radius * 2 + 1;
            player.sendSystemMessage(Component.translatable("loliPickaxe.range", side, side, side));
            level.playSound(null, player.blockPosition(), ModContent.LOLI_SUCCESS.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static boolean validRangeTarget(Entity entity) {
        if (!entity.isAttackable() || entity instanceof Player || entity instanceof ArmorStand || entity instanceof AmbientCreature) return false;
        return !(entity instanceof PathfinderMob) || entity instanceof Enemy;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!isCorrectToolForDrops(stack, state)) return 1.0F;
        return radius(stack) > 0 ? Float.MAX_VALUE : miningSpeed(stack);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        int level = harvestLevel(stack);
        if (level < 0 || !state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return false;
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return level >= 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return level >= 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return level >= 1;
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> modifiers = ImmutableMultimap.builder();
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                "Loli attack damage", attackDamage(stack), AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                "Loli attack speed", attackSpeed(stack), AttributeModifier.Operation.ADDITION));
        return modifiers.build();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("smallLoliPickaxe.LoliDiggingSpeed", miningSpeed(stack)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("smallLoliPickaxe.LoliDiggingLevel", harvestLevel(stack)).withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("smallLoliPickaxe.LoliAttackDamage", attackDamage(stack)).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("smallLoliPickaxe.LoliFortuneLevel", fortune(stack)).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
