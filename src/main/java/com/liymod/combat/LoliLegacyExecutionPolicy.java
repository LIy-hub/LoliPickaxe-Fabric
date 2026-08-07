package com.liymod.combat;

import com.liymod.LiyMod;
import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliItemSettings;
import com.liymod.config.LoliServerConfig;
import com.liymod.item.ModItems;
import com.liymod.protection.LoliProtection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;

/**
 * Safe, server-authoritative equivalents for the legacy final-pickaxe execution options.
 * The permanent player-data suppression and zero-max-health patches from 1.12.2 are
 * intentionally represented by reversible player lists and bounded gameplay effects.
 */
public final class LoliLegacyExecutionPolicy {
    private static final int SOUL_EFFECT_TICKS = 20 * 60 * 5;
    private static final List<EquipmentSlot> DISARM_SLOTS = List.of(
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );

    private LoliLegacyExecutionPolicy() {
    }

    public static void registerEvents() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                applyPersistentPlayerStates(newPlayer));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> applyPersistentPlayerStates(handler.player)));
    }

    /** Applies only to optional automatic range execution; direct legacy-strength entries stay unchanged. */
    public static boolean permitsAutomaticRangeTarget(ItemStack tool, Entity target) {
        if (target instanceof LightningBolt) {
            return false;
        }

        boolean includeAll = LoliItemSettings.getBoolean(tool, LoliConfigOption.TARGET_ALL_ENTITIES);
        if (!(target instanceof LivingEntity)) {
            return includeAll;
        }

        return LoliItemSettings.getBoolean(tool, LoliConfigOption.TARGET_FRIENDLY_ENTITIES)
                || target instanceof Enemy;
    }

    /** Called only after the existing service has returned a successful ABSOLUTE execution. */
    public static void afterSuccessfulAbsolute(Entity attacker, Entity target) {
        ItemStack tool = findExecutionTool(attacker);
        if (tool.isEmpty()) {
            return;
        }

        if (LoliItemSettings.getBoolean(tool, LoliConfigOption.FORCE_REMOVE)
                && !(target instanceof ServerPlayer)) {
            // Keep removal inside the existing execution manager; this option only accelerates its normal lock.
            LoliExecutionManager.forceRemoval(target);
        }

        if (!(target instanceof ServerPlayer player)
                || LoliProtection.isExecutionImmune(player)) {
            return;
        }

        if (LoliItemSettings.getBoolean(tool, LoliConfigOption.CLEAR_INVENTORY)) {
            safelyDropInventory(player);
        } else if (LoliItemSettings.getBoolean(tool, LoliConfigOption.DROP_EQUIPMENT)) {
            safelyDropEquipment(player);
        }

        if (LoliItemSettings.getBoolean(tool, LoliConfigOption.REINCARNATION)) {
            addPlayer(LoliConfigOption.REINCARNATION_LIST, player);
        }
        if (LoliItemSettings.getBoolean(tool, LoliConfigOption.SOUL_REDEMPTION)
                && !containsPlayer(LoliConfigOption.SOUL_REDEMPTION_WHITELIST, player)) {
            addPlayer(LoliConfigOption.SOUL_REDEMPTION_LIST, player);
        }

        if (LoliItemSettings.getBoolean(tool, LoliConfigOption.KICK_PLAYER)) {
            player.connection.disconnect(Component.literal(safeKickMessage(tool)));
        }
    }

    public static List<String> entries(LoliConfigOption option) {
        requirePlayerList(option);
        String encoded;
        try {
            // Defensive normalization also covers manually edited properties before a reload/save cycle.
            encoded = (String) option.parse(LoliServerConfig.getString(option));
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
        if (encoded.isBlank()) {
            return List.of();
        }
        return List.of(encoded.split(",")).stream().map(String::trim).toList();
    }

    public static boolean addEntry(LoliConfigOption option, String entry) {
        requirePlayerList(option);
        String validated;
        try {
            validated = (String) option.parse(entry);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        if (validated.isEmpty() || validated.contains(",")) {
            return false;
        }

        Map<String, String> values = keyedEntries(option);
        if (!values.containsKey(validated.toLowerCase(Locale.ROOT)) && values.size() >= 24) {
            return false;
        }
        values.putIfAbsent(validated.toLowerCase(Locale.ROOT), validated);
        return LoliServerConfig.set(option, String.join(",", values.values()));
    }

    public static boolean removeEntry(LoliConfigOption option, String entry) {
        requirePlayerList(option);
        Map<String, String> values = keyedEntries(option);
        if (values.remove(entry.trim().toLowerCase(Locale.ROOT)) == null) {
            return false;
        }
        return LoliServerConfig.set(option, String.join(",", values.values()));
    }

    private static void applyPersistentPlayerStates(ServerPlayer player) {
        if (removeMatchingPlayer(LoliConfigOption.REINCARNATION_LIST, player)) {
            player.removeAllEffects();
            player.clearFire();
            player.setTicksFrozen(0);
            player.fallDistance = 0.0F;
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0F);
            player.sendSystemMessage(Component.translatable("message.liymod.reincarnation_safe"));
        }

        if (containsPlayer(LoliConfigOption.SOUL_REDEMPTION_LIST, player)
                && !containsPlayer(LoliConfigOption.SOUL_REDEMPTION_WHITELIST, player)
                && !LoliProtection.isExecutionImmune(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SOUL_EFFECT_TICKS, 1));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SOUL_EFFECT_TICKS, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, SOUL_EFFECT_TICKS, 0));
            player.sendSystemMessage(Component.translatable("message.liymod.soul_redemption_safe"));
        }
    }

    private static void safelyDropInventory(ServerPlayer player) {
        List<ItemStack> removed = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                removed.add(stack);
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty()) {
            removed.add(carried.copy());
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
        removed.forEach(stack -> spawnRecoverableDrop(player, stack));
        if (!removed.isEmpty()) {
            LiyMod.LOGGER.info(
                    "Safely cleared {} inventory stacks from {} as protected recoverable drops at {}, {}, {}",
                    removed.size(),
                    player.getGameProfile().name(),
                    player.blockPosition().getX(),
                    player.blockPosition().getY(),
                    player.blockPosition().getZ()
            );
        }
    }

    private static void safelyDropEquipment(ServerPlayer player) {
        List<ItemStack> removed = new ArrayList<>();
        for (EquipmentSlot slot : DISARM_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            removed.add(stack.copy());
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
        removed.forEach(stack -> spawnRecoverableDrop(player, stack));
        if (!removed.isEmpty()) {
            LiyMod.LOGGER.info(
                    "Safely disarmed {} equipment stacks from {} as protected recoverable drops",
                    removed.size(),
                    player.getGameProfile().name()
            );
        }
    }

    private static void spawnRecoverableDrop(ServerPlayer owner, ItemStack stack) {
        if (!(owner.level() instanceof ServerLevel level) || stack.isEmpty()) {
            return;
        }
        ItemEntity drop = new ItemEntity(level, owner.getX(), owner.getY() + 0.5D, owner.getZ(), stack);
        drop.setTarget(owner.getUUID());
        drop.setUnlimitedLifetime();
        drop.setInvulnerable(true);
        drop.setPickUpDelay(20);
        level.addFreshEntity(drop);
    }

    private static ItemStack findExecutionTool(Entity attacker) {
        if (!(attacker instanceof LivingEntity living)) {
            return ItemStack.EMPTY;
        }
        ItemStack mainHand = living.getMainHandItem();
        if (mainHand.is(ModItems.LOLI_PICKAXE)) {
            return mainHand;
        }
        ItemStack offHand = living.getOffhandItem();
        return offHand.is(ModItems.LOLI_PICKAXE) ? offHand : ItemStack.EMPTY;
    }

    private static String safeKickMessage(ItemStack tool) {
        String configured = LoliItemSettings.getString(tool, LoliConfigOption.KICK_MESSAGE)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace("§", "");
        return configured.isBlank()
                ? String.valueOf(LoliConfigOption.KICK_MESSAGE.defaultValue())
                : configured;
    }

    private static void addPlayer(LoliConfigOption option, ServerPlayer player) {
        if (!addEntry(option, player.getUUID().toString())) {
            LiyMod.LOGGER.warn("Could not add {} to the bounded {}", player.getUUID(), option.id());
        }
    }

    private static boolean containsPlayer(LoliConfigOption option, ServerPlayer player) {
        String uuid = player.getUUID().toString();
        String name = player.getGameProfile().name();
        return entries(option).stream().anyMatch(entry ->
                entry.equalsIgnoreCase(uuid) || entry.equalsIgnoreCase(name));
    }

    private static boolean removeMatchingPlayer(LoliConfigOption option, ServerPlayer player) {
        Map<String, String> values = keyedEntries(option);
        boolean changed = values.remove(player.getUUID().toString().toLowerCase(Locale.ROOT)) != null;
        changed |= values.remove(player.getGameProfile().name().toLowerCase(Locale.ROOT)) != null;
        return changed && LoliServerConfig.set(option, String.join(",", values.values()));
    }

    private static Map<String, String> keyedEntries(LoliConfigOption option) {
        Map<String, String> values = new LinkedHashMap<>();
        entries(option).forEach(entry -> values.put(entry.toLowerCase(Locale.ROOT), entry));
        return values;
    }

    private static void requirePlayerList(LoliConfigOption option) {
        if (option != LoliConfigOption.REINCARNATION_LIST
                && option != LoliConfigOption.SOUL_REDEMPTION_LIST
                && option != LoliConfigOption.SOUL_REDEMPTION_WHITELIST) {
            throw new IllegalArgumentException("Not a Loli player-list option: " + option.id());
        }
    }
}
