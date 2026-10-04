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
    private int syncedPage;
    private int syncedPageCount=1;
    private int sentPage=-1,sentPageCount=-1;

    public StorageMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readEnum(InteractionHand.class));
    }

    public StorageMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.STORAGE.get(), id);
        this.player = inventory.player;
        this.hand = hand;
        this.ownerStack = inventory.player.getItemInHand(hand);
        this.storage = new LoliStorageContainer(ownerStack);
        syncedPageCount=storage.pageCount();
        storage.setPage(Math.min(storage.page(),syncedPageCount-1));syncedPage=storage.page();
        for (int row = 0; row < 9; row++) for (int col = 0; col < 9; col++) {
            addSlot(new Slot(storage, row * 9 + col, 8 + col * 18, 8 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return storage.mayPlace(getSlotIndex(), stack); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 174 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 232));
        addDataSlot(new DataSlot() {
            @Override public int get() { return page(); }
            @Override public void set(int value) { syncedPage=Math.max(0,Math.min(syncedPageCount-1,value));storage.setPageFromNetwork(syncedPage); }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return pageCount(); }
            @Override public void set(int value) { syncedPageCount=Math.max(1,value);syncedPage=Math.min(syncedPage,syncedPageCount-1);storage.setPageFromNetwork(syncedPage); }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return LoliStorageData.autoAccept(ownerStack) ? 1 : 0; }
            @Override public void set(int value) { LoliStorageData.setAutoAccept(ownerStack, value != 0); }
        });
    }

    public int page() { return player.level().isClientSide?syncedPage:storage.page(); }
    public int pageCount() { return player.level().isClientSide?syncedPageCount:storage.pageCount(); }
    public boolean autoAccept() { return LoliStorageData.autoAccept(ownerStack); }

    @Override public boolean stillValid(Player player) {
        return player == this.player && player.getItemInHand(hand) == ownerStack && LoliStorageData.supports(ownerStack);
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (!stillValid(player)) return false;
        if (id == BUTTON_PREVIOUS) storage.setPage(Math.max(0,storage.page() - 1));
        else if (id == BUTTON_NEXT) storage.setPage(Math.min(storage.pageCount()-1,storage.page() + 1));
        else if (id == BUTTON_AUTO_ACCEPT) LoliStorageData.setAutoAccept(ownerStack, !LoliStorageData.autoAccept(ownerStack));
        else if (id == BUTTON_DROP_ALL) dropAll();
        else return false;
        broadcastChanges();
        return true;
    }

    public void applyPageSync(int page,int count) {
        syncedPageCount=Math.max(1,Math.min(LoliStorageData.FINAL_PAGES,count));
        syncedPage=Math.max(0,Math.min(syncedPageCount-1,page));storage.setPageFromNetwork(syncedPage);
    }
    private void synchronizePages() {
        if(!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;
        int count=storage.pageCount(),page=Math.min(storage.page(),count-1);
        storage.setPage(page);
        if(page!=sentPage || count!=sentPageCount) {
            com.liymod.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->serverPlayer),
                    new com.liymod.network.ModNetwork.StoragePageSyncPacket(containerId,page,count));
            sentPage=page;sentPageCount=count;
        }
    }
    @Override public void broadcastChanges() { synchronizePages();super.broadcastChanges(); }
    @Override public void broadcastFullState() { synchronizePages();super.broadcastFullState(); }
    @Override public void clicked(int slot,int button,net.minecraft.world.inventory.ClickType type,Player player) {
        int bound=hand==InteractionHand.MAIN_HAND?108+player.getInventory().selected:-1;
        if(slot==bound || type==net.minecraft.world.inventory.ClickType.SWAP &&
                (hand==InteractionHand.MAIN_HAND && button==player.getInventory().selected || hand==InteractionHand.OFF_HAND && button==40)) return;
        super.clicked(slot,button,type,player);
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
        if(index<0 || index>=slots.size()) return ItemStack.EMPTY;
        Slot slot = getSlot(index);
        if (slot.getItem()==ownerStack || !slot.hasItem()) return ItemStack.EMPTY;
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

    @Override public void removed(Player player) { if(!player.level().isClientSide) storage.setChanged(); storage.close(); super.removed(player); }
}
