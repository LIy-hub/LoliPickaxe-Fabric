package com.liymod.storage;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LoliBlacklistContainer implements Container {
    private final LoliStorageData storage;

    public LoliBlacklistContainer(LoliStorageData storage) {
        this.storage = storage;
    }

    @Override
    public int getContainerSize() {
        return LoliStorageData.BLACKLIST_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return storage.getBlacklistItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = getItem(slot);
        if (!removed.isEmpty()) {
            storage.setBlacklistItem(slot, ItemStack.EMPTY);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        storage.setBlacklistItem(slot, stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return storage.stillValid(player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            storage.setBlacklistItem(slot, ItemStack.EMPTY);
        }
    }
}
