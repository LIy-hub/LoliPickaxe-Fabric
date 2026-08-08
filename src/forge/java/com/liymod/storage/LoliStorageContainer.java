package com.liymod.storage;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LoliStorageContainer implements Container {
    private final ItemStack tool;
    private final List<ItemStack> items;
    private int page;

    public LoliStorageContainer(ItemStack tool) {
        this.tool = tool;
        this.items = LoliStorageData.load(tool);
    }

    public int pageCount() { return Math.max(1, LoliStorageData.pages(tool)); }
    public int page() { return page; }
    public void setPage(int page) { this.page = Math.max(0, Math.min(pageCount() - 1, page)); }

    @Override public int getContainerSize() { return LoliStorageData.SLOTS_PER_PAGE; }
    private int absolute(int slot) { return page * LoliStorageData.SLOTS_PER_PAGE + slot; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { int index = absolute(slot); return index >= 0 && index < items.size() ? items.get(index) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack current = getItem(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = current.split(amount);
        if (current.isEmpty()) items.set(absolute(slot), ItemStack.EMPTY);
        setChanged(); return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(slot);
        if (!current.isEmpty()) items.set(absolute(slot), ItemStack.EMPTY);
        return current;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        int index = absolute(slot);
        if (index >= 0 && index < items.size() && LoliStorageData.canStoreAt(items, index, stack)) {
            items.set(index, stack);
            setChanged();
        }
    }
    public boolean mayPlace(int slot, ItemStack stack) {
        int index = absolute(slot);
        return index >= 0 && index < items.size() && LoliStorageData.canStoreAt(items, index, stack);
    }
    @Override public void setChanged() { LoliStorageData.save(tool, items); }
    @Override public boolean stillValid(Player player) { return LoliStorageData.supports(tool); }
    @Override public void clearContent() { for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY); setChanged(); }
    public List<ItemStack> allItems() { return items; }
}
