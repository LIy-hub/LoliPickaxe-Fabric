package com.liymod.menu;

import com.liymod.storage.LoliStorageData;
import com.liymod.network.StoragePageSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class StorageMenu extends AbstractContainerMenu {
    private static final int STORAGE_END = LoliStorageData.SLOTS_PER_PAGE;
    private static final int INVENTORY_START = STORAGE_END;
    private static final int INVENTORY_END = INVENTORY_START + 27;
    private static final int HOTBAR_START = INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Player player;
    private final InteractionHand hand;
    private final ItemStack ownerStack;
    private final LoliStorageData storage;
    private int syncedPage;
    private int syncedPageCount;

    public StorageMenu(int containerId, Inventory inventory, ToolMenuData data) {
        super(ModMenus.STORAGE, containerId);
        this.player = inventory.player;
        this.hand = data.hand();
        this.ownerStack = player.getItemInHand(hand);
        this.storage = LoliStorageData.open(ownerStack);
        this.syncedPage = storage.getCurrentPage();
        this.syncedPageCount = storage.getPageCount();

        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(storage, row * 9 + column, 8 + column * 18, 8 + row * 18));
            }
        }
        addStandardInventorySlots(inventory, 8, 174);
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return storage.getCurrentPage();
            }

            @Override
            public void set(int value) {
                syncedPage = Math.clamp(value, 0, Math.max(0, syncedPageCount - 1));
                storage.setCurrentPageFromNetwork(syncedPage);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return storage.getPageCount();
            }

            @Override
            public void set(int value) {
                syncedPageCount = Math.max(1, value);
                syncedPage = Math.clamp(syncedPage, 0, syncedPageCount - 1);
                storage.setCurrentPageFromNetwork(syncedPage);
            }
        });
    }

    public LoliStorageData getStorage() {
        return storage;
    }

    public int getCurrentPage() {
        return syncedPage;
    }

    public int getPageCount() {
        return syncedPageCount;
    }

    public void changePage(ServerPlayer player, int delta) {
        storage.setCurrentPage(storage.getCurrentPage() + Integer.signum(delta));
        syncedPage = storage.getCurrentPage();
        ServerPlayNetworking.send(
                player,
                new StoragePageSyncPayload(syncedPage, storage.getPageCount())
        );
        broadcastFullState();
    }

    /** Applies the ordered S2C page cue before vanilla writes the following slot snapshot. */
    public void applyPageSync(int page, int pageCount) {
        syncedPageCount = Math.max(1, pageCount);
        syncedPage = Math.clamp(page, 0, syncedPageCount - 1);
        storage.setCurrentPageFromNetwork(syncedPage);
    }

    public void dropAll(ServerPlayer player) {
        for (ItemStack stack : storage.removeAllStoredItems()) {
            player.drop(stack, false);
        }
        broadcastFullState();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand) == ownerStack && LoliStorageData.hasStorage(ownerStack);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (isBoundToolInteraction(slotId, button, input)) {
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem() || slot.getItem() == ownerStack) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex < STORAGE_END) {
            if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!LoliStorageData.isStorageItem(stack)
                && storage.canPlaceItem(0, stack)
                && moveItemStackTo(stack, 0, STORAGE_END, false)) {
            // Moved into storage.
        } else if (slotIndex < INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    private boolean isBoundToolInteraction(int slotId, int button, ContainerInput input) {
        int boundMenuSlot = hand == InteractionHand.MAIN_HAND
                ? HOTBAR_START + player.getInventory().getSelectedSlot()
                : -1;
        if (slotId == boundMenuSlot) {
            return true;
        }
        return input == ContainerInput.SWAP
                && ((hand == InteractionHand.MAIN_HAND
                && button == player.getInventory().getSelectedSlot())
                || (hand == InteractionHand.OFF_HAND && button == Inventory.SLOT_OFFHAND));
    }
}
