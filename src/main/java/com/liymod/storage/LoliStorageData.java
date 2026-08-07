package com.liymod.storage;

import com.liymod.item.LoliPickaxeItem;
import com.liymod.item.SmallLoliPickaxeItem;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Server-authoritative, bounded storage persisted inside the owning pickaxe CUSTOM_DATA. */
public final class LoliStorageData implements Container {
    public static final int SLOTS_PER_PAGE = 81;
    public static final int FINAL_PAGE_COUNT = 100;
    public static final int BLACKLIST_SIZE = 81;

    private static final String ROOT_KEY = "LoliStorage";
    private static final String CURRENT_PAGE_KEY = "CurrentPage";
    private static final String ITEMS_KEY = "Items";
    private static final String BLACKLIST_KEY = "Blacklist";
    private static final String SLOT_KEY = "Slot";
    private static final String STACK_KEY = "Stack";
    private static final int MAX_STACK_NBT_BYTES = 32 * 1024;
    private static final int MAX_TOTAL_NBT_BYTES = 4 * 1024 * 1024;

    private final ItemStack ownerStack;
    private final int pageCount;
    private final NonNullList<ItemStack> items;
    private final NonNullList<ItemStack> blacklist;
    private int currentPage;
    private int storedBytes;

    private LoliStorageData(ItemStack ownerStack, int pageCount) {
        this.ownerStack = ownerStack;
        this.pageCount = pageCount;
        this.items = NonNullList.withSize(pageCount * SLOTS_PER_PAGE, ItemStack.EMPTY);
        this.blacklist = NonNullList.withSize(BLACKLIST_SIZE, ItemStack.EMPTY);
        load();
    }

    public static LoliStorageData open(ItemStack stack) {
        int pages = pageCount(stack);
        if (pages <= 0) {
            throw new IllegalArgumentException("Item does not expose Loli storage");
        }
        return new LoliStorageData(stack, pages);
    }

    public static boolean hasStorage(ItemStack stack) {
        return pageCount(stack) > 0;
    }

    public static boolean isStorageItem(ItemStack stack) {
        return stack.getItem() instanceof LoliPickaxeItem
                || stack.getItem() instanceof SmallLoliPickaxeItem;
    }

    public static int pageCount(ItemStack stack) {
        if (stack.getItem() instanceof LoliPickaxeItem) {
            return FINAL_PAGE_COUNT;
        }
        if (stack.getItem() instanceof SmallLoliPickaxeItem) {
            return Math.max(0, SmallLoliPickaxeItem.getStoragePages(stack));
        }
        return 0;
    }

    public int getPageCount() {
        return pageCount;
    }

    public ItemStack getOwnerStack() {
        return ownerStack;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        int clamped = Math.clamp(page, 0, pageCount - 1);
        if (currentPage != clamped) {
            currentPage = clamped;
            persist();
        }
    }

    public void setCurrentPageFromNetwork(int page) {
        currentPage = Math.clamp(page, 0, pageCount - 1);
    }

    public ItemStack getBlacklistItem(int slot) {
        return slot >= 0 && slot < BLACKLIST_SIZE ? blacklist.get(slot) : ItemStack.EMPTY;
    }

    public void setBlacklistItem(int slot, ItemStack requested) {
        if (slot < 0 || slot >= BLACKLIST_SIZE) {
            return;
        }
        ItemStack sanitized = sanitize(requested, true);
        replaceWithBudget(blacklist, slot, sanitized);
    }

    public boolean isBlacklisted(ItemStack candidate) {
        if (candidate.isEmpty()) {
            return false;
        }
        for (ItemStack entry : blacklist) {
            if (!entry.isEmpty() && ItemStack.isSameItemSameComponents(entry, candidate)) {
                return true;
            }
        }
        return false;
    }

    /** Inserts as much as possible and returns the uninserted remainder. */
    public ItemStack insert(ItemStack requested) {
        ItemStack remaining = sanitize(requested, false);
        if (remaining.isEmpty() || isBlacklisted(remaining)) {
            return requested.copy();
        }

        boolean changed = false;
        for (int index = 0; index < items.size() && !remaining.isEmpty(); index++) {
            ItemStack existing = items.get(index);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) {
                continue;
            }
            int room = existing.getMaxStackSize() - existing.getCount();
            if (room <= 0) {
                continue;
            }
            int moved = Math.min(room, remaining.getCount());
            ItemStack enlarged = existing.copyWithCount(existing.getCount() + moved);
            if (!replaceWithinBudget(items, index, enlarged)) {
                continue;
            }
            remaining.shrink(moved);
            changed = true;
        }

        for (int index = 0; index < items.size() && !remaining.isEmpty(); index++) {
            if (!items.get(index).isEmpty()) {
                continue;
            }
            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            ItemStack inserted = remaining.copyWithCount(moved);
            if (!replaceWithinBudget(items, index, inserted)) {
                break;
            }
            remaining.shrink(moved);
            changed = true;
        }

        if (changed) {
            persist();
        }
        return remaining;
    }

    public NonNullList<ItemStack> removeAllStoredItems() {
        NonNullList<ItemStack> removed = NonNullList.create();
        for (int index = 0; index < items.size(); index++) {
            ItemStack stack = items.get(index);
            if (!stack.isEmpty()) {
                removed.add(stack);
                items.set(index, ItemStack.EMPTY);
            }
        }
        recalculateStoredBytes();
        persist();
        return removed;
    }

    @Override
    public int getContainerSize() {
        return SLOTS_PER_PAGE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        int index = pageIndex(slot);
        return index >= 0 ? items.get(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        int index = pageIndex(slot);
        if (index < 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = items.get(index);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = existing.split(Math.min(amount, existing.getCount()));
        if (existing.isEmpty()) {
            items.set(index, ItemStack.EMPTY);
        }
        recalculateStoredBytes();
        persist();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        int index = pageIndex(slot);
        if (index < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.get(index);
        items.set(index, ItemStack.EMPTY);
        recalculateStoredBytes();
        persist();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack requested) {
        int index = pageIndex(slot);
        if (index < 0) {
            return;
        }
        ItemStack sanitized = sanitize(requested, false);
        replaceWithBudget(items, index, sanitized);
    }

    @Override
    public void setChanged() {
        persist();
    }

    @Override
    public boolean stillValid(Player player) {
        return !ownerStack.isEmpty() && hasStorage(ownerStack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isSafeStack(stack) && !isBlacklisted(stack);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getMaxStackSize();
    }

    @Override
    public void clearContent() {
        removeAllStoredItems();
    }

    private int pageIndex(int slot) {
        if (slot < 0 || slot >= SLOTS_PER_PAGE) {
            return -1;
        }
        return currentPage * SLOTS_PER_PAGE + slot;
    }

    private void replaceWithBudget(NonNullList<ItemStack> list, int slot, ItemStack replacement) {
        if (replaceWithinBudget(list, slot, replacement)) {
            persist();
        }
    }

    private boolean replaceWithinBudget(NonNullList<ItemStack> list, int slot, ItemStack replacement) {
        ItemStack previous = list.get(slot);
        int previousBytes = serializedSize(previous);
        int replacementBytes = serializedSize(replacement);
        long projected = (long) storedBytes - previousBytes + replacementBytes;
        if (projected > MAX_TOTAL_NBT_BYTES) {
            return false;
        }
        list.set(slot, replacement);
        storedBytes = (int) projected;
        return true;
    }

    private static ItemStack sanitize(ItemStack requested, boolean blacklistEntry) {
        if (requested == null || requested.isEmpty() || !isSafeStack(requested)) {
            return ItemStack.EMPTY;
        }
        int maximum = blacklistEntry ? 1 : requested.getMaxStackSize();
        return requested.copyWithCount(Math.clamp(requested.getCount(), 1, maximum));
    }

    private static boolean isSafeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || isStorageItem(stack)) {
            return false;
        }
        int size = serializedSize(stack.copyWithCount(1));
        return size > 0 && size <= MAX_STACK_NBT_BYTES;
    }

    private static int serializedSize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        try {
            CompoundTag probe = new CompoundTag();
            probe.store(STACK_KEY, ItemStack.CODEC, stack);
            return probe.sizeInBytes();
        } catch (RuntimeException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private void load() {
        CompoundTag root = ownerStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag storage = root.getCompoundOrEmpty(ROOT_KEY);
        if (storage.sizeInBytes() > MAX_TOTAL_NBT_BYTES) {
            return;
        }
        currentPage = Math.clamp(storage.getIntOr(CURRENT_PAGE_KEY, 0), 0, pageCount - 1);
        loadEntries(storage.getListOrEmpty(ITEMS_KEY), items, items.size(), false);
        loadEntries(storage.getListOrEmpty(BLACKLIST_KEY), blacklist, BLACKLIST_SIZE, true);
        recalculateStoredBytes();
    }

    private void loadEntries(
            ListTag encoded,
            NonNullList<ItemStack> destination,
            int limit,
            boolean blacklistEntries
    ) {
        Set<Integer> occupied = new HashSet<>();
        for (int index = 0; index < encoded.size() && index < limit; index++) {
            CompoundTag entry = encoded.getCompoundOrEmpty(index);
            int slot = entry.getIntOr(SLOT_KEY, -1);
            if (slot < 0 || slot >= limit || !occupied.add(slot)) {
                continue;
            }
            ItemStack decoded = entry.read(STACK_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            ItemStack sanitized = sanitize(decoded, blacklistEntries);
            int size = serializedSize(sanitized);
            if (!sanitized.isEmpty() && (long) storedBytes + size <= MAX_TOTAL_NBT_BYTES) {
                destination.set(slot, sanitized);
                storedBytes += size;
            }
        }
    }

    private void recalculateStoredBytes() {
        storedBytes = 0;
        for (ItemStack stack : items) {
            storedBytes += serializedSize(stack);
        }
        for (ItemStack stack : blacklist) {
            storedBytes += serializedSize(stack);
        }
    }

    private void persist() {
        CompoundTag storage = new CompoundTag();
        storage.putInt(CURRENT_PAGE_KEY, currentPage);
        storage.put(ITEMS_KEY, saveEntries(items));
        storage.put(BLACKLIST_KEY, saveEntries(blacklist));
        if (storage.sizeInBytes() > MAX_TOTAL_NBT_BYTES) {
            return;
        }
        CustomData.update(
                DataComponents.CUSTOM_DATA,
                ownerStack,
                root -> root.put(ROOT_KEY, storage)
        );
    }

    private static ListTag saveEntries(NonNullList<ItemStack> source) {
        ListTag encoded = new ListTag();
        for (int slot = 0; slot < source.size(); slot++) {
            ItemStack stack = source.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt(SLOT_KEY, slot);
            entry.store(STACK_KEY, ItemStack.CODEC, stack);
            encoded.add(entry);
        }
        return encoded;
    }
}
