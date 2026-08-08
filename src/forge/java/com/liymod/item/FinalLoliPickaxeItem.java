package com.liymod.item;

import com.liymod.combat.LoliErasureService;
import com.liymod.config.FinalToolSettings;
import com.liymod.config.LogicalEnchantments;
import com.liymod.protection.LoliProtection;
import com.liymod.registry.ModContent;
import com.liymod.storage.LoliStorageData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class FinalLoliPickaxeItem extends PickaxeItem {
    public static final double RIGHT_CLICK_RANGE = 1024.0D;
    public static final double REACH = 1024.0D;
    private static final int SWING_RANGE = 1024;
    private static final int SWING_CONE_DEGREES = 6;
    private static final String DIVINE_DESCRIPTION_KEY = "liymod.loli_pickaxe.tooltip.divine";
    private static final ChatFormatting[] DIVINE_COLORS = {
            ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.AQUA,
            ChatFormatting.GOLD,
            ChatFormatting.YELLOW,
            ChatFormatting.RED,
            ChatFormatting.BLUE
    };
    private static final long DIVINE_ANIMATION_STEP_MILLIS = 85L;
    private static final int DIVINE_GLITCH_PERIOD_STEPS = 55;
    private static final int DIVINE_GLITCH_DURATION_STEPS = 5;

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
        tooltip.add(divineDescription());
        tooltip.add(Component.translatable("liymod.loli_pickaxe.tip").withStyle(ChatFormatting.AQUA));
        int miningWidth = FinalToolSettings.radius(stack) * 2 + 1;

        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.combat",
                value((int) RIGHT_CLICK_RANGE),
                value(SWING_RANGE),
                value(SWING_CONE_DEGREES)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.mining",
                value(miningWidth),
                value(LogicalEnchantments.MAXIMUM_LEVEL),
                value((int) REACH)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.processing",
                state(FinalToolSettings.autoFurnace(stack)),
                state(FinalToolSettings.autoAccept(stack)),
                value(LoliStorageData.FINAL_PAGES),
                value(LoliStorageData.FINAL_PAGES * LoliStorageData.SLOTS_PER_PAGE)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.defense",
                state(FinalToolSettings.thorns(stack)),
                state(true)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.automatic_attack",
                state(FinalToolSettings.autoKill(stack)),
                value(FinalToolSettings.autoKillRange(stack))
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.mining_rules",
                state(FinalToolSettings.stopOnLiquid(stack))
        ).withStyle(ChatFormatting.GRAY));
        appendAdvancedStatus(stack, tooltip);
        tooltip.add(Component.translatable("liymod.loli_pickaxe.tooltip.keys.primary")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("liymod.loli_pickaxe.tooltip.keys.secondary")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    private static Component divineDescription() {
        String localizedText = Component.translatable(DIVINE_DESCRIPTION_KEY).getString();
        long animationStep = System.currentTimeMillis() / DIVINE_ANIMATION_STEP_MILLIS;
        boolean glitchWindow = Math.floorMod(animationStep, DIVINE_GLITCH_PERIOD_STEPS)
                < DIVINE_GLITCH_DURATION_STEPS;
        MutableComponent result = Component.empty();
        int[] codePoints = localizedText.codePoints().toArray();

        for (int index = 0; index < codePoints.length; index++) {
            int codePoint = codePoints[index];
            MutableComponent glyph = Component.literal(new String(Character.toChars(codePoint)));
            boolean glitchGlyph = glitchWindow
                    && Character.isLetterOrDigit(codePoint)
                    && Math.floorMod(index * 11L + animationStep * 7L, 5L) == 0L;
            if (glitchGlyph) {
                glyph.withStyle(ChatFormatting.RED, ChatFormatting.BOLD, ChatFormatting.OBFUSCATED);
            } else {
                int colorIndex = (int) Math.floorMod(animationStep + index * 2L, DIVINE_COLORS.length);
                glyph.withStyle(ChatFormatting.BOLD, DIVINE_COLORS[colorIndex]);
            }
            result.append(glyph);
        }
        return result;
    }

    private static void appendAdvancedStatus(ItemStack stack, List<Component> tooltip) {
        MutableComponent enabled = Component.empty();
        int enabledCount = 0;
        enabledCount = appendAdvancedOption(enabled, enabledCount, FinalToolSettings.targetFriendly(stack), "target_friendly_entities");
        enabledCount = appendAdvancedOption(enabled, enabledCount, FinalToolSettings.targetAll(stack), "target_all_entities");
        enabledCount = appendAdvancedOption(enabled, enabledCount, FinalToolSettings.forceRemove(stack), "force_remove");
        enabledCount = appendAdvancedOption(enabled, enabledCount, FinalToolSettings.clearInventory(stack), "clear_inventory");
        enabledCount = appendAdvancedOption(enabled, enabledCount, FinalToolSettings.dropEquipment(stack), "drop_equipment");
        enabledCount = appendAdvancedOption(enabled, enabledCount, FinalToolSettings.kickPlayer(stack), "kick_player");
        enabledCount = appendAdvancedOption(enabled, enabledCount, FinalToolSettings.reincarnation(stack), "reincarnation");
        enabledCount = appendAdvancedOption(enabled, enabledCount, FinalToolSettings.soulRedemption(stack), "soul_redemption");

        Component status = enabledCount == 0
                ? Component.translatable("liymod.loli_pickaxe.tooltip.none").withStyle(ChatFormatting.DARK_GRAY)
                : enabled;
        tooltip.add(Component.translatable("liymod.loli_pickaxe.tooltip.advanced", status)
                .withStyle(ChatFormatting.GRAY));
    }

    private static int appendAdvancedOption(MutableComponent result, int count, boolean enabled, String key) {
        if (!enabled) return count;
        if (count > 0) result.append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY));
        result.append(Component.translatable("config.liymod.loli." + key).withStyle(ChatFormatting.LIGHT_PURPLE));
        return count + 1;
    }

    private static Component state(boolean enabled) {
        return Component.translatable(enabled
                        ? "liymod.loli_pickaxe.tooltip.enabled"
                        : "liymod.loli_pickaxe.tooltip.disabled")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static Component value(Number value) {
        return Component.literal(value.toString()).withStyle(ChatFormatting.AQUA);
    }
}
