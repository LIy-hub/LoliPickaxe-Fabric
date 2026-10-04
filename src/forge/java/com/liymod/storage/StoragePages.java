package com.liymod.storage;

import java.util.function.IntPredicate;

/** Page visibility depends on occupied slots, independently of stack quantity or NBT layout. */
public final class StoragePages {
    private StoragePages() { }
    public static int visibleCount(int slotCount, int pageSize, int capacity, IntPredicate occupied) {
        int last = Math.min(slotCount, capacity * pageSize) - 1;
        while (last >= 0 && !occupied.test(last)) last--;
        int count = last < 0 ? 1 : last / pageSize + 1;
        if (last >= 0 && count < capacity) {
            boolean full = true;
            int start = (count - 1) * pageSize;
            for (int slot = start; slot < start + pageSize; slot++) {
                if (slot >= slotCount || !occupied.test(slot)) { full = false; break; }
            }
            if (full) count++;
        }
        return Math.max(1, Math.min(capacity, count));
    }
}
