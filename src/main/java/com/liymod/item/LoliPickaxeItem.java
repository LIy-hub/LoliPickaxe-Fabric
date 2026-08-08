package com.liymod.item;

import com.liymod.combat.LoliErasureService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import com.liymod.config.LoliItemSettings;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public final class LoliPickaxeItem extends Item {
    public static final int FORTUNE_LEVEL = 32;
    private static final double ABILITY_RANGE = 1024.0;

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
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        makeUnbreakable(stack);
        refreshEnchantments(stack, world);
        if (entity instanceof Player player) {
            LoliItemSettings.ensureDefaults(stack);
            LoliItemSettings.bindOwnerIfAbsent(stack, player);
            LoliFinalEffects.ensureDefaults(stack);
        }
        super.inventoryTick(stack, world, entity, slot);
    }

    public static void refreshEnchantments(ItemStack stack, ServerLevel level) {
        if (!(stack.getItem() instanceof LoliPickaxeItem)) {
            return;
        }
        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> fortune = registry.getOrThrow(Enchantments.FORTUNE);
        if (EnchantmentHelper.getItemEnchantmentLevel(fortune, stack) < FORTUNE_LEVEL) {
            EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(fortune, FORTUNE_LEVEL));
        }
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide()) {
            return InteractionResult.PASS;
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

        for (Entity target : targets) {
            double x = target.getX();
            double y = target.getY();
            double z = target.getZ();
            if (LoliErasureService.executeAbsolute(user, target)
                    == LoliErasureService.Result.EXECUTED) {
                spawnLightning(world, x, y, z);
            }
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    private static void makeUnbreakable(ItemStack stack) {
        stack.setDamageValue(0);
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    private static void spawnLightning(Level world, double x, double y, double z) {
        LightningBolt lightning = new LightningBolt(EntityTypes.LIGHTNING_BOLT, world);
        lightning.setPos(x, y, z);
        world.addFreshEntity(lightning);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.accept(Component.translatable("liymod.loli_pickaxe.tip").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
