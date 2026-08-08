package com.liymod.item;

import com.liymod.combat.LoliErasureService;
import com.liymod.protection.LoliProtection;
import com.liymod.registry.ModContent;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import com.liymod.config.FinalToolSettings;
import com.liymod.config.LogicalEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class FinalLoliPickaxeItem extends PickaxeItem {
    public static final double RIGHT_CLICK_RANGE = 1024.0D;
    public static final double REACH = 1024.0D;

    public FinalLoliPickaxeItem(Properties properties) {
        super(LoliToolTier.INSTANCE, Integer.MAX_VALUE, Float.POSITIVE_INFINITY, properties);
    }

    public static void refresh(ItemStack stack) {
        stack.setDamageValue(0);
        var tag = stack.getOrCreateTag();
        tag.putBoolean("Unbreakable", true);
        if (!tag.getBoolean("LoliDefaultEnchantmentsInitialized")) {
            if (stack.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE) <= 0) {
                LogicalEnchantments.setLevel(stack, Enchantments.BLOCK_FORTUNE, LogicalEnchantments.MAXIMUM_LEVEL);
            }
            if (ModContent.LOLI_AUTO_FURNACE.isPresent()
                    && stack.getEnchantmentLevel(ModContent.LOLI_AUTO_FURNACE.get()) <= 0) {
                LogicalEnchantments.setLevel(stack, ModContent.LOLI_AUTO_FURNACE.get(), 1);
            }
            FinalToolSettings.setMapValue(stack, FinalToolSettings.ENCHANTMENTS,
                    net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(Enchantments.BLOCK_FORTUNE),
                    stack.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE), LogicalEnchantments.MAXIMUM_LEVEL);
            tag.putBoolean("LoliDefaultEnchantmentsInitialized", true);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        refresh(stack);
        if (!level.isClientSide && entity instanceof Player player && !stack.getOrCreateTag().hasUUID("LoliOwner")) {
            stack.getOrCreateTag().putUUID("LoliOwner", player.getUUID());
            stack.getOrCreateTag().putString("LoliOwnerName", player.getName().getString());
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    public static UUID owner(ItemStack stack) { return stack.getOrCreateTag().hasUUID("LoliOwner") ? stack.getOrCreateTag().getUUID("LoliOwner") : null; }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) LoliErasureService.executeAbsolute(attacker, target);
        refresh(stack);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND && LoliProtection.isMainHandProtected(player)) {
            double maximumDistanceSquared = RIGHT_CLICK_RANGE * RIGHT_CLICK_RANGE;
            List<Entity> targets = new ArrayList<>();
            for (Entity target : ((ServerLevel) level).getAllEntities()) targets.add(target);
            for (Entity target : targets) {
                if (target == player || target instanceof LightningBolt
                        || target.isRemoved() || player.distanceToSqr(target) > maximumDistanceSquared) continue;
                double x = target.getX(), y = target.getY(), z = target.getZ();
                if (LoliErasureService.executeAbsolute(player, target) == LoliErasureService.Result.EXECUTED) {
                    LightningBolt lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
                    if (lightning != null) { lightning.moveTo(x, y, z); level.addFreshEntity(lightning); }
                }
            }
        }
        return InteractionResultHolder.success(stack);
    }

    @Override public float getDestroySpeed(ItemStack stack, BlockState state) { return Float.MAX_VALUE; }
    @Override public boolean isCorrectToolForDrops(BlockState state) { return true; }
    @Override public boolean isEnchantable(ItemStack stack) { return true; }
    @Override public int getEnchantmentValue() { return 30; }
    @Override public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModContent.LOLI.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("liymod.loli_pickaxe.tip").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
