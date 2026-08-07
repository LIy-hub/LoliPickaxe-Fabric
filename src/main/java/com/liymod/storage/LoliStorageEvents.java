package com.liymod.storage;

import com.liymod.LiyMod;
import com.liymod.menu.BlacklistMenu;
import com.liymod.menu.StorageMenu;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class LoliStorageEvents {
    private static final double COLLECT_RANGE = 4.0D;
    private static final int COLLECT_INTERVAL_TICKS = 5;

    private LoliStorageEvents() {
    }

    public static void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(LoliStorageEvents::collectNearbyItems);
        LiyMod.LOGGER.info("Registering bounded Loli storage collection");
    }

    private static void collectNearbyItems(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (player.tickCount % COLLECT_INTERVAL_TICKS != 0) {
                    continue;
                }
                LoliStorageData storage = findStorage(player);
                if (storage == null) {
                    continue;
                }
                AABB area = player.getBoundingBox().inflate(COLLECT_RANGE);
                List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class, area);
                for (ItemEntity entity : nearby) {
                    Entity owner = entity.getOwner();
                    if (!entity.isAlive()
                            || entity.getItem().isEmpty()
                            || (owner != null && owner != player)) {
                        continue;
                    }
                    ItemStack before = entity.getItem();
                    ItemStack remaining = storage.insert(before);
                    if (remaining.isEmpty()) {
                        entity.discard();
                    } else if (remaining.getCount() != before.getCount()) {
                        entity.setItem(remaining);
                    }
                }
            }
        }
    }

    private static LoliStorageData findStorage(ServerPlayer player) {
        if (player.containerMenu instanceof StorageMenu menu && menu.stillValid(player)) {
            return menu.getStorage();
        }
        if (player.containerMenu instanceof BlacklistMenu menu && menu.stillValid(player)) {
            return menu.getStorage();
        }

        ItemStack mainHand = player.getMainHandItem();
        if (LoliStorageData.hasStorage(mainHand)) {
            return LoliStorageData.open(mainHand);
        }
        ItemStack offHand = player.getOffhandItem();
        if (LoliStorageData.hasStorage(offHand)) {
            return LoliStorageData.open(offHand);
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (LoliStorageData.hasStorage(stack)) {
                return LoliStorageData.open(stack);
            }
        }
        return null;
    }
}
