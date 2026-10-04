package com.liymod.menu;

import com.liymod.storage.LoliBlacklistContainer;
import com.liymod.storage.LoliStorageData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class BlacklistMenu extends AbstractContainerMenu {
    private final Player player;
    private final InteractionHand hand;
    private final ItemStack ownerStack;
    private final LoliStorageData storage;
    private final LoliBlacklistContainer entries;

    public BlacklistMenu(int containerId, Inventory inventory, ToolMenuData data) {
        super(ModMenus.BLACKLIST, containerId);
        this.player = inventory.player;
        this.hand = data.hand();
        this.ownerStack = player.getItemInHand(hand);
        this.storage = LoliStorageData.open(ownerStack);
        this.entries = new LoliBlacklistContainer(storage);

        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new GhostSlot(entries, row * 9 + column, 8 + column * 18, 8 + row * 18));
            }
        }
        addStandardInventorySlots(inventory, 8, 174);
    }

    public LoliStorageData getStorage() {
        return storage;
    }

    public LoliBlacklistContainer getEntries() {
        return entries;
    }

    public void updateEntry(int slot, boolean clear) {
        if (slot < 0 || slot >= LoliStorageData.BLACKLIST_SIZE) {
            return;
        }
        entries.setItem(slot, clear ? ItemStack.EMPTY : getCarried());
        broadcastFullState();
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand) == ownerStack && LoliStorageData.hasStorage(ownerStack);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < LoliStorageData.BLACKLIST_SIZE) {
            if (!player.level().isClientSide() && input == ContainerInput.PICKUP) {
                updateEntry(slotId, getCarried().isEmpty());
            }
            return;
        }
        int boundMenuSlot = hand == InteractionHand.MAIN_HAND
                ? LoliStorageData.BLACKLIST_SIZE + 27 + player.getInventory().getSelectedSlot()
                : -1;
        if (slotId == boundMenuSlot
                || (input == ContainerInput.SWAP
                && ((hand == InteractionHand.MAIN_HAND
                && button == player.getInventory().getSelectedSlot())
                || (hand == InteractionHand.OFF_HAND && button == Inventory.SLOT_OFFHAND)))) {
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    private static final class GhostSlot extends Slot {
        private GhostSlot(LoliBlacklistContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
