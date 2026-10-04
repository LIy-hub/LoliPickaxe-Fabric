package com.liymod.storage;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LoliStorageContainer implements Container {
    private static final Map<ItemStack,WeakReference<LoliStorageContainer>> ACTIVE=Collections.synchronizedMap(new WeakHashMap<>());
    private final ItemStack tool;
    private final List<ItemStack> items;
    private final int capacity;
    private int page;
    private int visiblePages=1;
    private boolean pagesDirty=true;
    public LoliStorageContainer(ItemStack tool) {
        this.tool=tool;this.items=LoliStorageData.load(tool);capacity=Math.max(1,LoliStorageData.pages(tool));
        page=Math.max(0,Math.min(capacity-1,tool.getOrCreateTag().getInt("LoliStorageCurrentPage")));
        ACTIVE.put(tool,new WeakReference<>(this));
    }
    public static LoliStorageContainer active(ItemStack tool) {
        WeakReference<LoliStorageContainer> ref=ACTIVE.get(tool);return ref==null?null:ref.get();
    }
    public void close() { if(active(tool)==this) ACTIVE.remove(tool); }
    public void externalMutation() { pagesDirty=true; }
    public int pageCount() {
        if(pagesDirty) { visiblePages=visiblePageCount(items,capacity);pagesDirty=false; }
        return visiblePages;
    }
    public static int visiblePageCount(List<ItemStack> items,int capacity) {
        return StoragePages.visibleCount(items.size(), LoliStorageData.SLOTS_PER_PAGE, capacity,
                slot -> !items.get(slot).isEmpty());
    }

    public int page() { return page; }
    public void setPage(int page) {
        setPageFromNetwork(page);tool.getOrCreateTag().putInt("LoliStorageCurrentPage",this.page);
    }
    public void setPageFromNetwork(int page) { this.page=Math.max(0,Math.min(capacity-1,page)); }
    @Override public int getContainerSize() { return LoliStorageData.SLOTS_PER_PAGE; }
    private int absolute(int slot) { return slot<0 || slot>=getContainerSize()?-1:page*LoliStorageData.SLOTS_PER_PAGE+slot; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { int index=absolute(slot);return index>=0 && index<items.size()?items.get(index):ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot,int amount) {
        ItemStack current=getItem(slot);if(current.isEmpty()) return ItemStack.EMPTY;
        ItemStack result=current.split(amount);if(current.isEmpty()) items.set(absolute(slot),ItemStack.EMPTY);
        setChanged();return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current=getItem(slot);if(!current.isEmpty()) { items.set(absolute(slot),ItemStack.EMPTY);setChanged(); }return current;
    }
    @Override public void setItem(int slot,ItemStack stack) {
        int index=absolute(slot);
        if(index>=0 && index<items.size() && LoliStorageData.canStoreAt(items,index,stack)) {
            ItemStack copy=stack.copy();copy.setCount(Math.min(copy.getCount(),copy.getMaxStackSize()));items.set(index,copy);setChanged();
        }
    }
    public boolean mayPlace(int slot,ItemStack stack) {
        int index=absolute(slot);return index>=0 && index<items.size() && !LoliStorageData.isBlacklisted(tool,stack) && LoliStorageData.canStoreAt(items,index,stack);
    }
    @Override public void setChanged() { pagesDirty=true;LoliStorageData.save(tool,items); }
    @Override public boolean stillValid(Player player) { return LoliStorageData.supports(tool); }
    @Override public void clearContent() { for(int i=0;i<items.size();i++) items.set(i,ItemStack.EMPTY);setChanged(); }
    public List<ItemStack> allItems() { return items; }
}
