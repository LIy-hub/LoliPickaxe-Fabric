package com.liymod.item;

import com.liymod.combat.LoliErasureService;
import com.liymod.combat.LoliKillSummary;
import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliItemSettings;
import com.liymod.storage.LoliStorageData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import com.liymod.compat.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public final class LoliPickaxeItem extends Item {
    public static final int FORTUNE_LEVEL = 32;
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
    private static final double ABILITY_RANGE = 1024.0;
    private static final int SWING_RANGE = 1024;
    private static final int SWING_CONE_DEGREES = 6;
    private static final LoliConfigOption[] ADVANCED_TOOLTIP_OPTIONS = {
            LoliConfigOption.TARGET_FRIENDLY_ENTITIES,
            LoliConfigOption.TARGET_ALL_ENTITIES,
            LoliConfigOption.FORCE_REMOVE,
            LoliConfigOption.CLEAR_INVENTORY,
            LoliConfigOption.DROP_EQUIPMENT,
            LoliConfigOption.KICK_PLAYER,
            LoliConfigOption.REINCARNATION,
            LoliConfigOption.SOUL_REDEMPTION
    };

    public LoliPickaxeItem(Properties settings) {
        super(settings);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        makeUnbreakable(stack);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        makeUnbreakable(stack);
        if (world instanceof ServerLevel serverLevel) refreshEnchantments(stack, serverLevel);
        if (entity instanceof Player player) {
            LoliItemSettings.ensureDefaults(stack);
            LoliItemSettings.bindOwnerIfAbsent(stack, player);
            LoliFinalEffects.ensureDefaults(stack);
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    public static void refreshEnchantments(ItemStack stack, ServerLevel level) {
        if (!(stack.getItem() instanceof LoliPickaxeItem)) {
            return;
        }
        Registry<Enchantment> registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Enchantment fortune = Enchantments.BLOCK_FORTUNE;
        if (EnchantmentHelper.getItemEnchantmentLevel(fortune, stack) < FORTUNE_LEVEL) {
            com.liymod.compat.ItemEnchantments.updateEnchantments(stack, mutable -> mutable.set(fortune, FORTUNE_LEVEL));
        }
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide()) {
            return new net.minecraft.world.InteractionResultHolder<>(InteractionResult.PASS, user.getItemInHand(hand));
        }

        ServerLevel serverLevel = (ServerLevel) world;
        double maximumDistanceSquared = ABILITY_RANGE * ABILITY_RANGE;
        List<Entity> targets = new ArrayList<>();
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity != user
                    && !(entity instanceof LightningBolt)
                    && user.distanceToSqr(entity) <= maximumDistanceSquared) {
                targets.add(entity);
            }
        }

        try (var summary = LoliKillSummary.begin(user)) {
            for (Entity target : targets) {
                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();
                if (LoliErasureService.executeAbsolute(user, target)
                        == LoliErasureService.Result.EXECUTED) {
                    spawnLightning(world, x, y, z);
                }
            }
        }

        return new net.minecraft.world.InteractionResultHolder<>(InteractionResult.CONSUME, user.getItemInHand(hand));
    }

    @Override
    public boolean isCorrectToolForDrops(net.minecraft.world.level.block.state.BlockState state) { return true; }

    @Override
    public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) { return Float.MAX_VALUE; }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier>
            getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot) {
        if (slot != net.minecraft.world.entity.EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        return com.google.common.collect.ImmutableMultimap.of(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID,"liymod.attack_damage",Double.MAX_VALUE,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION),
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_UUID,"liymod.attack_speed",Double.MAX_VALUE,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
    }

    private static void makeUnbreakable(ItemStack stack) {
        stack.setDamageValue(0);
        com.liymod.compat.LegacyComponents.set(stack, DataComponents.UNBREAKABLE, new com.liymod.compat.Unbreakable(false));
    }

    private static void spawnLightning(Level world, double x, double y, double z) {
        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
        lightning.setPos(x, y, z);
        world.addFreshEntity(lightning);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            net.minecraft.world.level.Level context,
            java.util.List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(divineDescription());
        tooltip.add(Component.translatable("liymod.loli_pickaxe.tip").withStyle(ChatFormatting.AQUA));
        int miningRadius = LoliItemSettings.getMiningRadius(stack);
        int automaticRange = LoliItemSettings.getInt(stack, LoliConfigOption.AUTO_KILL_RANGE);

        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.combat",
                value((int) ABILITY_RANGE),
                value(SWING_RANGE),
                value(SWING_CONE_DEGREES)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.mining",
                miningModeDescription(miningRadius).copy().withStyle(ChatFormatting.AQUA),
                value(FORTUNE_LEVEL),
                value(LoliItemSettings.getDouble(stack, LoliConfigOption.BLOCK_REACH_DISTANCE))
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.processing",
                state(LoliItemSettings.getBoolean(stack, LoliConfigOption.AUTO_FURNACE)),
                state(LoliItemSettings.getBoolean(stack, LoliConfigOption.AUTO_ACCEPT)),
                value(LoliStorageData.FINAL_PAGE_COUNT),
                value(LoliStorageData.FINAL_PAGE_COUNT * LoliStorageData.SLOTS_PER_PAGE)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.defense",
                state(LoliItemSettings.getBoolean(stack, LoliConfigOption.THORNS)),
                state(LoliItemSettings.getBoolean(stack, LoliConfigOption.OWNER_PROTECTION))
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.automatic_attack",
                state(LoliItemSettings.getBoolean(stack, LoliConfigOption.AUTO_KILL_RANGE_ENTITY)),
                value(automaticRange)
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "liymod.loli_pickaxe.tooltip.mining_rules",
                state(LoliItemSettings.getBoolean(stack, LoliConfigOption.STOP_ON_LIQUID))
        ).withStyle(ChatFormatting.GRAY));
        appendAdvancedStatus(stack, tooltip);
        tooltip.add(Component.translatable("liymod.loli_pickaxe.tooltip.keys.primary")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("liymod.loli_pickaxe.tooltip.keys.secondary")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public static Component miningModeDescription(int radius) {
        return radius <= 0 ? Component.translatable("liymod.loli_pickaxe.mining.single")
                : Component.translatable("liymod.loli_pickaxe.mining.range", radius * 2 + 1);
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

    private static void appendAdvancedStatus(ItemStack stack, java.util.List<Component> tooltip) {
        MutableComponent enabled = Component.empty();
        int enabledCount = 0;
        for (LoliConfigOption option : ADVANCED_TOOLTIP_OPTIONS) {
            if (!LoliItemSettings.getBoolean(stack, option)) {
                continue;
            }
            if (enabledCount++ > 0) {
                enabled.append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY));
            }
            enabled.append(Component.translatable(option.translationKey()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        Component status = enabledCount == 0
                ? Component.translatable("liymod.loli_pickaxe.tooltip.none").withStyle(ChatFormatting.DARK_GRAY)
                : enabled;
        tooltip.add(Component.translatable("liymod.loli_pickaxe.tooltip.advanced", status)
                .withStyle(ChatFormatting.GRAY));
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
