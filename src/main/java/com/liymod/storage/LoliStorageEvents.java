package com.liymod.storage;

import com.liymod.LiyMod;
import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliItemSettings;
import com.liymod.item.LoliMiningExperience;
import com.liymod.menu.BlacklistMenu;
import com.liymod.menu.StorageMenu;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public final class LoliStorageEvents {
    private static final double COLLECT_RANGE = 4.0D;
    private static final int COLLECT_INTERVAL_TICKS = 5;
    private static final String MANUAL_EJECTION_TAG = "liymod.storage_manual_ejection";

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
                ItemStack held = findStorageStack(player);
                boolean collectExperience = allowsExperienceCollection(player.getMainHandItem())
                        || allowsExperienceCollection(player.getOffhandItem());
                if (held.isEmpty() && !collectExperience) {
                    continue;
                }
                AABB area = player.getBoundingBox().inflate(COLLECT_RANGE);
                if (collectExperience) {
                    LoliMiningExperience.collectNearby(player, level.getEntitiesOfClass(ExperienceOrb.class, area));
                }
                if (held.isEmpty()) continue;
                List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class, area);
                if (nearby.isEmpty()) continue;
                LoliStorageData storage = null;
                LoliStorageData.Batch batch = null;
                try {
                    for (ItemEntity entity : nearby) {
                        Entity owner = entity.getOwner();
                        if (!entity.isAlive()
                                || entity.getItem().isEmpty()
                                || entity.getTags().contains(MANUAL_EJECTION_TAG)
                                || (owner != null && owner != player)) {
                            continue;
                        }
                        if (storage == null) {
                            storage = LoliStorageData.open(held);
                            batch = storage.beginBatch();
                        }
                        ItemStack before = entity.getItem();
                        ItemStack remaining = storage.insert(before);
                        if (remaining.isEmpty()) {
                            entity.discard();
                        } else if (remaining.getCount() != before.getCount()) {
                            entity.setItem(remaining);
                        }
                    }
                } finally {
                    if (batch != null) batch.close();
                }
            }
        }
    }

    private static ItemStack findStorageStack(ServerPlayer player) {
        if (player.containerMenu instanceof StorageMenu menu && menu.stillValid(player)) {
            LoliStorageData storage = menu.getStorage();
            if (allowsNearbyCollection(storage.getOwnerStack())) {
                return storage.getOwnerStack();
            }
        }
        if (player.containerMenu instanceof BlacklistMenu menu && menu.stillValid(player)) {
            LoliStorageData storage = menu.getStorage();
            if (allowsNearbyCollection(storage.getOwnerStack())) {
                return storage.getOwnerStack();
            }
        }

        ItemStack mainHand = player.getMainHandItem();
        if (allowsNearbyCollection(mainHand)) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        if (allowsNearbyCollection(offHand)) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    /** Marks an intentional player drop so nearby auto-accept cannot undo that action. */
    public static void markManualEjection(ItemEntity entity) {
        if (entity != null) {
            entity.addTag(MANUAL_EJECTION_TAG);
        }
    }

    public static boolean hasHeldStorage(ServerPlayer player) {
        return LoliStorageData.hasStorage(player.getMainHandItem())
                || LoliStorageData.hasStorage(player.getOffhandItem());
    }

    private static boolean allowsNearbyCollection(ItemStack stack) {
        if (!LoliStorageData.hasStorage(stack)) {
            return false;
        }
        return !LoliItemSettings.isFinalPickaxe(stack)
                || LoliItemSettings.getBoolean(stack, LoliConfigOption.AUTO_ACCEPT);
    }

    private static boolean allowsExperienceCollection(ItemStack stack) {
        return LoliItemSettings.isFinalPickaxe(stack)
                && LoliItemSettings.getBoolean(stack, LoliConfigOption.AUTO_ACCEPT);
    }
}
