package com.liymod.menu;

import com.liymod.storage.LoliStorageContainer;
import com.liymod.storage.LoliStorageData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class StorageMenu extends AbstractContainerMenu {
    public static final int BUTTON_PREVIOUS = 1;
    public static final int BUTTON_NEXT = 2;
    public static final int BUTTON_AUTO_ACCEPT = 3;
    public static final int BUTTON_DROP_ALL = 4;
    private final Player player;
    private final InteractionHand hand;
    private final ItemStack ownerStack;
    private final LoliStorageContainer storage;

    public StorageMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readEnum(InteractionHand.class));
    }

    public StorageMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.STORAGE.get(), id);
        this.player = inventory.player;
        this.hand = hand;
        this.ownerStack = inventory.player.getItemInHand(hand);
        this.storage = new LoliStorageContainer(ownerStack);
        for (int row = 0; row < 9; row++) for (int col = 0; col < 9; col++) {
            addSlot(new Slot(storage, row * 9 + col, 8 + col * 18, 18 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return storage.mayPlace(getSlotIndex(), stack); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 184 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 242));
        addDataSlot(new DataSlot() {
            @Override public int get() { return storage.page(); }
            @Override public void set(int value) { storage.setPage(value); }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return LoliStorageData.autoAccept(ownerStack) ? 1 : 0; }
            @Override public void set(int value) { LoliStorageData.setAutoAccept(ownerStack, value != 0); }
        });
    }

    public int page() { return storage.page(); }
    public int pageCount() { return storage.pageCount(); }
    public boolean autoAccept() { return LoliStorageData.autoAccept(ownerStack); }

    @Override public boolean stillValid(Player player) {
        return player == this.player && player.getItemInHand(hand) == ownerStack && LoliStorageData.supports(ownerStack);
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player)) return false;
        if (id == BUTTON_PREVIOUS) storage.setPage(storage.page() - 1);
        else if (id == BUTTON_NEXT) storage.setPage(storage.page() + 1);
        else if (id == BUTTON_AUTO_ACCEPT) LoliStorageData.setAutoAccept(ownerStack, !LoliStorageData.autoAccept(ownerStack));
        else if (id == BUTTON_DROP_ALL) dropAll();
        else return false;
        broadcastChanges();
        return true;
    }

    private void dropAll() {
        if (player.level().isClientSide) return;
        for (ItemStack stack : storage.allItems()) {
            if (stack.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5D, player.getZ(), stack.copy());
            entity.setTarget(player.getUUID());
            entity.setPickUpDelay(20);
            LoliStorageData.markEjected(entity, player.level().getGameTime() + 200L);
            player.level().addFreshEntity(entity);
            stack.setCount(0);
        }
        storage.setChanged();
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        if (index < 81) {
            if (!moveItemStackTo(source, 81, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (LoliStorageData.isStorageTool(source) || !moveItemStackTo(source, 0, 81, false)) return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override public void removed(Player player) { storage.setChanged(); super.removed(player); }
}
