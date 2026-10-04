package com.liymod.storage;

import com.liymod.item.LoliPickaxeItem;
import com.liymod.item.SmallLoliPickaxeItem;
import com.liymod.nbt.LoliCustomData;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import net.minecraft.core.NonNullList;
import com.liymod.compat.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import com.liymod.compat.CustomData;

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
    // Standalone fixtures do not apply Fabric mixins. Both keys and values stay weak here.
    private static final Map<ItemStack, WeakReference<LoliStorageData>> FALLBACK_CACHE = new WeakHashMap<>();

    private final ItemStack ownerStack;
    private final int pageCount;
    private final boolean clientMenuView;
    private final NonNullList<ItemStack> items;
    private final NonNullList<ItemStack> blacklist;
    private final int[] itemBytes;
    private final int[] blacklistBytes;
    private final CompoundTag[] encodedItems;
    private final CompoundTag[] encodedBlacklist;
    private final Item[] indexedItems;
    private final TreeSet<Integer> emptySlots = new TreeSet<>();
    private final Map<Item, TreeSet<Integer>> partialSlots = new HashMap<>();
    private CustomData savedData;
    private CompoundTag savedStorage;
    private int currentPage;
    private int storedBytes;
    private int visiblePageCount = 1;
    private boolean pagesDirty = true;
    private int batchDepth;
    private boolean pendingPersistence;

    private LoliStorageData(ItemStack ownerStack, int pageCount) {
        this(ownerStack, pageCount, false);
    }

    private LoliStorageData(ItemStack ownerStack, int pageCount, boolean clientMenuView) {
        this.ownerStack = ownerStack;
        this.pageCount = pageCount;
        this.clientMenuView = clientMenuView;
        this.items = NonNullList.withSize(pageCount * SLOTS_PER_PAGE, ItemStack.EMPTY);
        this.blacklist = NonNullList.withSize(BLACKLIST_SIZE, ItemStack.EMPTY);
        this.itemBytes = new int[items.size()];
        this.blacklistBytes = new int[BLACKLIST_SIZE];
        this.encodedItems = new CompoundTag[items.size()];
        this.encodedBlacklist = new CompoundTag[BLACKLIST_SIZE];
        this.indexedItems = new Item[items.size()];
        if (!clientMenuView) load();
    }

    public static LoliStorageData open(ItemStack stack) {
        int pages = pageCount(stack);
        if (pages <= 0) {
            throw new IllegalArgumentException("Item does not expose Loli storage");
        }
        return cached(stack, pages);
    }

    /** Client menus receive only authoritative visible slots; never decode/rewrite the owner's full storage. */
    public static LoliStorageData clientMenu(ItemStack stack) {
        int pages = pageCount(stack);
        if (pages <= 0) throw new IllegalArgumentException("Item does not expose Loli storage");
        return clientMenu(stack, pages);
    }

    static LoliStorageData clientMenu(ItemStack stack, int pages) {
        return new LoliStorageData(stack, pages, true);
    }

    static LoliStorageData cached(ItemStack stack, int pages) {
        if ((Object) stack instanceof LoliStorageHolder holder) {
            LoliStorageData storage = holder.liymod$getStorage();
            if (storage == null || storage.pageCount != pages) {
                storage = new LoliStorageData(stack, pages);
                holder.liymod$setStorage(storage);
            } else {
                storage.refresh();
            }
            return storage;
        }
        synchronized (FALLBACK_CACHE) {
            var reference = FALLBACK_CACHE.get(stack);
            LoliStorageData storage = reference == null ? null : reference.get();
            if (storage == null || storage.pageCount != pages) {
                storage = new LoliStorageData(stack, pages);
                FALLBACK_CACHE.put(stack, new WeakReference<>(storage));
            } else {
                storage.refresh();
            }
            return storage;
        }
    }

    private void refresh() {
        CustomData current = com.liymod.compat.LegacyComponents.getOrDefault(ownerStack, DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (current == savedData) return;
        CompoundTag storage = LoliCustomData.view(current).getCompound(ROOT_KEY);
        if (storage.equals(savedStorage)) {
            savedData = current;
            savedStorage = storage.copy();
            return;
        }
        if (batchDepth != 0) throw new IllegalStateException("Storage changed during an active insertion batch");
        for (int index = 0; index < items.size(); index++) items.set(index, ItemStack.EMPTY);
        for (int index = 0; index < blacklist.size(); index++) blacklist.set(index, ItemStack.EMPTY);
        storedBytes = 0;
        currentPage = 0;
        pagesDirty = true;
        load();
    }

    /** Defers encoding until the complete synchronous operation, including exceptional exits. */
    public Batch beginBatch() {
        return new Batch();
    }

    public final class Batch implements AutoCloseable {
        private boolean closed;

        private Batch() {
            batchDepth++;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (--batchDepth == 0 && pendingPersistence) {
                pendingPersistence = false;
                persist();
            }
        }
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

    /** Retains sparse saved positions; a full last page exposes one extra page for manual insertion. */
    public int getVisiblePageCount() {
        if (pagesDirty) {
            int lastSlot = items.size() - 1;
            while (lastSlot >= 0 && items.get(lastSlot).isEmpty()) {
                lastSlot--;
            }
            visiblePageCount = lastSlot < 0 ? 1 : lastSlot / SLOTS_PER_PAGE + 1;
            if (lastSlot >= 0 && visiblePageCount < pageCount) {
                boolean full = true;
                int pageStart = (visiblePageCount - 1) * SLOTS_PER_PAGE;
                for (int slot = pageStart; slot < pageStart + SLOTS_PER_PAGE; slot++) {
                    if (items.get(slot).isEmpty()) {
                        full = false;
                        break;
                    }
                }
                if (full) {
                    visiblePageCount++;
                }
            }
            pagesDirty = false;
        }
        return visiblePageCount;
    }

    public ItemStack getOwnerStack() {
        return ownerStack;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        int clamped = com.liymod.compat.LegacyMath.clamp(page, 0, pageCount - 1);
        if (currentPage != clamped) {
            currentPage = clamped;
            persist();
        }
    }

    public void setCurrentPageFromNetwork(int page) {
        currentPage = com.liymod.compat.LegacyMath.clamp(page, 0, pageCount - 1);
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
            if (!entry.isEmpty() && ItemStack.isSameItemSameTags(entry, candidate)) {
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
        TreeSet<Integer> candidates = partialSlots.get(remaining.getItem());
        Integer candidate = candidates == null || candidates.isEmpty() ? null : candidates.first();
        while (candidate != null && !remaining.isEmpty()) {
            int index = candidate;
            candidate = candidates.higher(index);
            ItemStack existing = items.get(index);
            if (!ItemStack.isSameItemSameTags(existing, remaining)) {
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

        while (!emptySlots.isEmpty() && !remaining.isEmpty()) {
            int index = emptySlots.first();
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
                replaceWithinBudget(items, index, ItemStack.EMPTY);
            }
        }
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
        int removedCount = Math.min(amount, existing.getCount());
        ItemStack removed = existing.copyWithCount(removedCount);
        replaceWithinBudget(items, index, existing.getCount() == removedCount
                ? ItemStack.EMPTY : existing.copyWithCount(existing.getCount() - removedCount));
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
        replaceWithinBudget(items, index, ItemStack.EMPTY);
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
        // Vanilla menu transfers can mutate stacks in place before notifying the container.
        int start = currentPage * SLOTS_PER_PAGE;
        for (int index = start; index < start + SLOTS_PER_PAGE; index++) {
            int size = serializedSize(items.get(index));
            storedBytes += size - itemBytes[index];
            itemBytes[index] = size;
            encodedItems[index] = null;
            reindex(index);
        }
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
        int[] sizes = list == items ? itemBytes : blacklistBytes;
        int previousBytes = sizes[slot];
        int replacementBytes = serializedSize(replacement);
        long projected = (long) storedBytes - previousBytes + replacementBytes;
        if (projected > MAX_TOTAL_NBT_BYTES) {
            return false;
        }
        list.set(slot, replacement);
        sizes[slot] = replacementBytes;
        (list == items ? encodedItems : encodedBlacklist)[slot] = null;
        if (list == items) reindex(slot);
        storedBytes = (int) projected;
        return true;
    }

    private static ItemStack sanitize(ItemStack requested, boolean blacklistEntry) {
        if (requested == null || requested.isEmpty() || !isSafeStack(requested)) {
            return ItemStack.EMPTY;
        }
        int maximum = blacklistEntry ? 1 : requested.getMaxStackSize();
        return requested.copyWithCount(com.liymod.compat.LegacyMath.clamp(requested.getCount(), 1, maximum));
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
            com.liymod.compat.LegacyNbt.store(probe, STACK_KEY, ItemStack.CODEC, stack);
            return probe.sizeInBytes();
        } catch (RuntimeException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private void load() {
        Arrays.fill(itemBytes, 0);
        Arrays.fill(blacklistBytes, 0);
        Arrays.fill(encodedItems, null);
        Arrays.fill(encodedBlacklist, null);
        savedData = com.liymod.compat.LegacyComponents.getOrDefault(ownerStack, DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag root = LoliCustomData.view(savedData);
        CompoundTag storage = root.getCompound(ROOT_KEY);
        savedStorage = storage.copy();
        if (storage.sizeInBytes() > MAX_TOTAL_NBT_BYTES) {
            rebuildIndexes();
            return;
        }
        currentPage = com.liymod.compat.LegacyMath.clamp(com.liymod.compat.LegacyNbt.getIntOr(storage, CURRENT_PAGE_KEY, 0), 0, pageCount - 1);
        loadEntries(storage.getList(ITEMS_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND), items, items.size(), false);
        loadEntries(storage.getList(BLACKLIST_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND), blacklist, BLACKLIST_SIZE, true);
        rebuildIndexes();
    }

    private void loadEntries(
            ListTag encoded,
            NonNullList<ItemStack> destination,
            int limit,
            boolean blacklistEntries
    ) {
        Set<Integer> occupied = new HashSet<>();
        for (int index = 0; index < encoded.size() && index < limit; index++) {
            CompoundTag entry = encoded.getCompound(index);
            int slot = com.liymod.compat.LegacyNbt.getIntOr(entry, SLOT_KEY, -1);
            if (slot < 0 || slot >= limit || !occupied.add(slot)) {
                continue;
            }
            ItemStack decoded = com.liymod.compat.LegacyNbt.read(entry, STACK_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            ItemStack sanitized = sanitize(decoded, blacklistEntries);
            int size = serializedSize(sanitized);
            if (!sanitized.isEmpty() && (long) storedBytes + size <= MAX_TOTAL_NBT_BYTES) {
                destination.set(slot, sanitized);
                (destination == items ? itemBytes : blacklistBytes)[slot] = size;
                storedBytes += size;
            }
        }
    }

    private void rebuildIndexes() {
        emptySlots.clear();
        partialSlots.clear();
        for (int index = 0; index < items.size(); index++) {
            indexedItems[index] = null;
            reindex(index);
        }
    }

    private void reindex(int slot) {
        emptySlots.remove(slot);
        Item previous = indexedItems[slot];
        if (previous != null) {
            TreeSet<Integer> previousSlots = partialSlots.get(previous);
            if (previousSlots != null) {
                previousSlots.remove(slot);
                if (previousSlots.isEmpty()) partialSlots.remove(previous);
            }
        }
        ItemStack stack = items.get(slot);
        indexedItems[slot] = stack.isEmpty() ? null : stack.getItem();
        if (stack.isEmpty()) emptySlots.add(slot);
        else if (stack.getCount() < stack.getMaxStackSize()) {
            partialSlots.computeIfAbsent(stack.getItem(), ignored -> new TreeSet<>()).add(slot);
        }
    }

    private void persist() {
        pagesDirty = true;
        if (clientMenuView) return;
        if (batchDepth > 0) {
            pendingPersistence = true;
            return;
        }
        CompoundTag storage = new CompoundTag();
        storage.putInt(CURRENT_PAGE_KEY, currentPage);
        storage.put(ITEMS_KEY, saveEntries(items, encodedItems));
        storage.put(BLACKLIST_KEY, saveEntries(blacklist, encodedBlacklist));
        if (storage.sizeInBytes() > MAX_TOTAL_NBT_BYTES) {
            return;
        }
        CompoundTag root = LoliCustomData.copyRoot(LoliCustomData.view(ownerStack));
        root.put(ROOT_KEY, storage);
        CustomData replacement = CustomData.of(root);
        com.liymod.compat.LegacyComponents.set(ownerStack, DataComponents.CUSTOM_DATA, replacement);
        savedData = replacement;
        savedStorage = LoliCustomData.view(replacement).getCompound(ROOT_KEY).copy();
    }

    private static ListTag saveEntries(NonNullList<ItemStack> source, CompoundTag[] cachedEntries) {
        ListTag encoded = new ListTag();
        for (int slot = 0; slot < source.size(); slot++) {
            ItemStack stack = source.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = cachedEntries[slot];
            if (entry == null) {
                entry = new CompoundTag();
                entry.putInt(SLOT_KEY, slot);
                com.liymod.compat.LegacyNbt.store(entry, STACK_KEY, ItemStack.CODEC, stack);
                cachedEntries[slot] = entry;
            }
            encoded.add(entry);
        }
        return encoded;
    }
}
