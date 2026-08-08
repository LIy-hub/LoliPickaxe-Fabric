package com.liymod.combat;

import com.liymod.LiyMod;
import com.liymod.config.FinalToolSettings;
import com.liymod.config.LoliServerConfig;
import com.liymod.registry.ModContent;
import com.liymod.storage.LoliStorageData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Safe, reversible equivalents for the legacy final-pickaxe execution options. */
public final class LoliLegacyExecutionPolicy {
    private static final int SOUL_EFFECT_TICKS = 20 * 60 * 5;
    private static final List<EquipmentSlot> DISARM_SLOTS = List.of(
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private LoliLegacyExecutionPolicy() { }

    public static boolean permitsAutomaticRangeTarget(ItemStack tool, Entity target) {
        if (target instanceof LightningBolt) return false;
        if (!(target instanceof LivingEntity)) return FinalToolSettings.targetAll(tool);
        return FinalToolSettings.targetFriendly(tool) || target instanceof Enemy;
    }

    public static PreparedExecution prepare(Entity attacker, Entity target) {
        if (!(target instanceof ServerPlayer player)) return new PreparedExecution(null);
        ItemStack tool = executionTool(attacker);
        if (tool.isEmpty()) return new PreparedExecution(null);
        PreparedExecution prepared = new PreparedExecution(player);
        try {
            prepared.prepare(tool);
            return prepared;
        } catch (RuntimeException exception) {
            prepared.close();
            LiyMod.LOGGER.warn("Could not prepare reversible execution options for {}", player.getUUID(), exception);
            return new PreparedExecution(null);
        }
    }

    public static final class PreparedExecution implements AutoCloseable {
        private final ServerPlayer player;
        private final Map<Integer, ItemStack> inventory = new LinkedHashMap<>();
        private final Map<EquipmentSlot, ItemStack> equipment = new LinkedHashMap<>();
        private AbstractContainerMenu menu;
        private ItemStack carried = ItemStack.EMPTY;
        private boolean reincarnation;
        private boolean soulRedemption;
        private boolean kick;
        private String kickMessage = "";
        private boolean committed;

        private PreparedExecution(ServerPlayer player) { this.player = player; }

        private void prepare(ItemStack tool) {
            menu = player.containerMenu;
            reincarnation = FinalToolSettings.reincarnation(tool);
            soulRedemption = FinalToolSettings.soulRedemption(tool);
            kick = FinalToolSettings.kickPlayer(tool);
            kickMessage = FinalToolSettings.kickMessage(tool);
            if (FinalToolSettings.clearInventory(tool)) detachInventory();
            else if (FinalToolSettings.dropEquipment(tool)) detachEquipment();
            synchronize();
        }

        private void detachInventory() {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().removeItemNoUpdate(slot);
                if (!stack.isEmpty()) inventory.put(slot, stack);
            }
            if (menu != null && !menu.getCarried().isEmpty()) {
                carried = menu.getCarried();
                menu.setCarried(ItemStack.EMPTY);
            }
        }

        private void detachEquipment() {
            for (EquipmentSlot slot : DISARM_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    equipment.put(slot, stack);
                    player.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
        }

        public void commit() {
            if (committed || player == null) { committed = true; return; }
            inventory.forEach((slot, stack) -> { if (!spawnRecoverable(stack)) player.getInventory().setItem(slot, stack); });
            equipment.forEach((slot, stack) -> { if (!spawnRecoverable(stack)) player.setItemSlot(slot, stack); });
            if (!carried.isEmpty() && !spawnRecoverable(carried) && menu != null) menu.setCarried(carried);
            inventory.clear(); equipment.clear(); carried = ItemStack.EMPTY;
            if (reincarnation) addPlayer("reincarnation_list", player);
            if (soulRedemption && !containsPlayer("soul_redemption_whitelist", player)) addPlayer("soul_redemption_list", player);
            if (kick) player.connection.disconnect(Component.literal(kickMessage));
            committed = true;
            synchronize();
        }

        @Override public void close() {
            if (committed || player == null) return;
            inventory.forEach((slot, stack) -> player.getInventory().setItem(slot, stack));
            equipment.forEach(player::setItemSlot);
            if (!carried.isEmpty() && menu != null) menu.setCarried(carried);
            inventory.clear(); equipment.clear(); carried = ItemStack.EMPTY;
            synchronize();
        }

        private boolean spawnRecoverable(ItemStack stack) {
            if (!(player.level() instanceof ServerLevel level) || stack.isEmpty()) return false;
            ItemEntity drop = new ItemEntity(level, player.getX(), player.getY() + 0.5D, player.getZ(), stack);
            drop.setTarget(player.getUUID()); drop.setUnlimitedLifetime(); drop.setInvulnerable(true); drop.setPickUpDelay(20);
            LoliStorageData.markEjected(drop, level.getGameTime() + 200L);
            return level.addFreshEntity(drop);
        }

        private void synchronize() {
            if (player == null) return;
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            if (menu != null && menu != player.inventoryMenu) menu.broadcastChanges();
        }
    }

    public static void applyPersistentPlayerStates(ServerPlayer player) {
        if (removeMatchingPlayer("reincarnation_list", player)) {
            player.removeAllEffects(); player.clearFire(); player.setTicksFrozen(0); player.fallDistance = 0.0F;
            player.setHealth(player.getMaxHealth()); player.getFoodData().setFoodLevel(20); player.getFoodData().setSaturation(5.0F);
            player.sendSystemMessage(Component.translatable("message.liymod.reincarnation_safe"));
        }
        if (containsPlayer("soul_redemption_list", player)
                && !containsPlayer("soul_redemption_whitelist", player)) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SOUL_EFFECT_TICKS, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SOUL_EFFECT_TICKS, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, SOUL_EFFECT_TICKS, 0));
            player.sendSystemMessage(Component.translatable("message.liymod.soul_redemption_safe"));
        }
    }

    public static List<String> entries(String key) {
        String value = LoliServerConfig.get(key);
        if (value == null || value.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String entry : value.split(",")) if (!entry.isBlank()) result.add(entry.trim());
        return result;
    }

    public static boolean addEntry(String key, String entry) {
        Map<String, String> values = keyedEntries(key);
        if (!values.containsKey(entry.toLowerCase(Locale.ROOT)) && values.size() >= 24) return false;
        values.putIfAbsent(entry.toLowerCase(Locale.ROOT), entry);
        return LoliServerConfig.set(key, String.join(",", values.values()));
    }

    public static boolean removeEntry(String key, String entry) {
        Map<String, String> values = keyedEntries(key);
        if (values.remove(entry.trim().toLowerCase(Locale.ROOT)) == null) return false;
        return LoliServerConfig.set(key, String.join(",", values.values()));
    }

    private static ItemStack executionTool(Entity attacker) {
        if (!(attacker instanceof LivingEntity living)) return ItemStack.EMPTY;
        if (living.getMainHandItem().is(ModContent.LOLI_PICKAXE.get())) return living.getMainHandItem();
        return living.getOffhandItem().is(ModContent.LOLI_PICKAXE.get()) ? living.getOffhandItem() : ItemStack.EMPTY;
    }

    private static void addPlayer(String key, ServerPlayer player) {
        if (!addEntry(key, player.getUUID().toString())) LiyMod.LOGGER.warn("Could not add {} to {}", player.getUUID(), key);
    }

    private static boolean containsPlayer(String key, ServerPlayer player) {
        return entries(key).stream().anyMatch(entry -> entry.equalsIgnoreCase(player.getUUID().toString())
                || entry.equalsIgnoreCase(player.getGameProfile().getName()));
    }

    private static boolean removeMatchingPlayer(String key, ServerPlayer player) {
        Map<String, String> values = keyedEntries(key);
        boolean changed = values.remove(player.getUUID().toString().toLowerCase(Locale.ROOT)) != null;
        changed |= values.remove(player.getGameProfile().getName().toLowerCase(Locale.ROOT)) != null;
        return changed && LoliServerConfig.set(key, String.join(",", values.values()));
    }

    private static Map<String, String> keyedEntries(String key) {
        Map<String, String> values = new LinkedHashMap<>();
        entries(key).forEach(entry -> values.put(entry.toLowerCase(Locale.ROOT), entry));
        return values;
    }
}
