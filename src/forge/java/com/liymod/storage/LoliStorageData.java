package com.liymod.storage;

import com.liymod.item.SmallLoliPickaxeItem;
import com.liymod.registry.ModContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.StringTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashSet;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LoliStorageData {
    public static final int SLOTS_PER_PAGE = 81;
    public static final int FINAL_PAGES = 100;
    public static final String STORAGE_KEY = "LoliStorage";
    public static final String AUTO_ACCEPT_KEY = "LoliAutoAccept";
    public static final String EJECTED_UNTIL_KEY = "LoliStorageEjectedUntil";
    public static final String EJECTED_KEY = "LoliStorageEjected";
    public static final String BLACKLIST_KEY = "LoliStorageBlacklist";
    private static final int MAX_STACK_NBT_BYTES = 32 * 1024;
    private static final int MAX_TOTAL_NBT_BYTES = 4 * 1024 * 1024;

    private LoliStorageData() { }

    public static boolean supports(ItemStack tool) {
        return tool.is(ModContent.LOLI_PICKAXE.get())
                || (tool.is(ModContent.SMALL_LOLI_PICKAXE.get()) && SmallLoliPickaxeItem.storagePages(tool) > 0);
    }

    public static boolean isStorageTool(ItemStack stack) {
        return stack != null && (stack.is(ModContent.LOLI_PICKAXE.get()) || stack.is(ModContent.SMALL_LOLI_PICKAXE.get()));
    }

    public static int pages(ItemStack tool) {
        if (tool.is(ModContent.LOLI_PICKAXE.get())) return FINAL_PAGES;
        return tool.is(ModContent.SMALL_LOLI_PICKAXE.get()) ? SmallLoliPickaxeItem.storagePages(tool) : 0;
    }

    public static boolean autoAccept(ItemStack tool) {
        if (!supports(tool)) return false;
        CompoundTag tag = tool.getOrCreateTag();
        return !tag.contains(AUTO_ACCEPT_KEY) || tag.getBoolean(AUTO_ACCEPT_KEY);
    }

    public static void setAutoAccept(ItemStack tool, boolean value) {
        if (supports(tool)) tool.getOrCreateTag().putBoolean(AUTO_ACCEPT_KEY, value);
    }

    public static List<ItemStack> load(ItemStack tool) {
        int slots = pages(tool) * SLOTS_PER_PAGE;
        ArrayList<ItemStack> result = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) result.add(ItemStack.EMPTY);
        if (!supports(tool)) return result;
        ListTag list = tool.getOrCreateTag().getList(STORAGE_KEY, Tag.TAG_COMPOUND);
        if (encodedSize(list) > MAX_TOTAL_NBT_BYTES) return result;
        Set<Integer> occupied = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= slots || !occupied.add(slot)) continue;
            ItemStack decoded = sanitize(ItemStack.of(entry.getCompound("Stack")));
            if (!decoded.isEmpty()) result.set(slot, decoded);
        }
        return result;
    }

    public static void save(ItemStack tool, List<ItemStack> items) {
        if (!supports(tool)) return;
        int slots = pages(tool) * SLOTS_PER_PAGE;
        ListTag list = new ListTag();
        long storedBytes = encodedSize(new ListTag());
        for (int i = 0; i < Math.min(slots, items.size()); i++) {
            ItemStack stack = sanitize(items.get(i));
            if (stack.isEmpty()) continue;
            CompoundTag entry = storageEntry(i, stack);
            int size = encodedSize(entry);
            if (storedBytes + size > MAX_TOTAL_NBT_BYTES) continue;
            list.add(entry);
            storedBytes += size;
        }
        while (!list.isEmpty() && encodedSize(list) > MAX_TOTAL_NBT_BYTES) list.remove(list.size() - 1);
        tool.getOrCreateTag().put(STORAGE_KEY, list);
    }

    public static ItemStack insert(ItemStack tool, ItemStack incoming) {
        if (!supports(tool) || incoming.isEmpty() || isStorageTool(incoming) || isBlacklisted(tool, incoming)) return incoming;
        if (!isSafeStack(incoming)) return incoming;
        List<ItemStack> items = load(tool);
        ItemStack remainder = incoming.copy();
        long storedBytes = totalSize(items);
        boolean changed = false;
        for (int index = 0; index < items.size(); index++) {
            ItemStack stored = items.get(index);
            if (remainder.isEmpty()) break;
            if (!stored.isEmpty() && ItemStack.isSameItemSameTags(stored, remainder)) {
                int moved = Math.min(remainder.getCount(), stored.getMaxStackSize() - stored.getCount());
                if (moved > 0) {
                    ItemStack enlarged = stored.copy(); enlarged.grow(moved);
                    int oldSize = storageCost(index, stored), newSize = storageCost(index, enlarged);
                    if (storedBytes - oldSize + newSize <= MAX_TOTAL_NBT_BYTES) {
                        items.set(index, enlarged); storedBytes += newSize - oldSize;
                        remainder.shrink(moved); changed = true;
                    }
                }
            }
        }
        for (int i = 0; i < items.size() && !remainder.isEmpty(); i++) {
            if (!items.get(i).isEmpty()) continue;
            int moved = Math.min(remainder.getCount(), remainder.getMaxStackSize());
            ItemStack stored = remainder.copy(); stored.setCount(moved);
            int size = storageCost(i, stored);
            if (storedBytes + size > MAX_TOTAL_NBT_BYTES) break;
            items.set(i, stored); storedBytes += size; remainder.shrink(moved); changed = true;
        }
        if (changed) save(tool, items);
        return remainder;
    }

    public static boolean absorb(Player player, ItemStack tool, ItemEntity entity) {
        if (!autoAccept(tool) || entity.getItem().isEmpty()) return false;
        if (entity.getPersistentData().getBoolean(EJECTED_KEY)
                || player.level().getGameTime() <= entity.getPersistentData().getLong(EJECTED_UNTIL_KEY)) return false;
        Entity owner = entity.getOwner();
        if (owner instanceof Player ownerPlayer && ownerPlayer != player) return false;
        ItemStack old = entity.getItem();
        ItemStack remainder = insert(tool, old);
        if (remainder.getCount() == old.getCount()) return false;
        if (remainder.isEmpty()) entity.discard(); else entity.setItem(remainder);
        return true;
    }

    public static void markEjected(ItemEntity entity, long until) {
        entity.getPersistentData().putBoolean(EJECTED_KEY, true);
        entity.getPersistentData().putLong(EJECTED_UNTIL_KEY, until);
    }

    public static Set<ResourceLocation> blacklist(ItemStack tool) {
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();
        ListTag list = tool.getOrCreateTag().getList(BLACKLIST_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size() && result.size() < 128; i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i)); if (id != null) result.add(id);
        }
        return result;
    }

    public static boolean setBlacklisted(ItemStack tool, ResourceLocation id, boolean value) {
        if (!supports(tool) || id == null || !BuiltInRegistries.ITEM.containsKey(id)) return false;
        Set<ResourceLocation> entries = blacklist(tool);
        if (value && entries.size() >= 128 && !entries.contains(id)) return false;
        if (value) entries.add(id); else entries.remove(id);
        ListTag list = new ListTag(); for (ResourceLocation entry : entries) list.add(StringTag.valueOf(entry.toString()));
        tool.getOrCreateTag().put(BLACKLIST_KEY, list); return true;
    }

    public static boolean isBlacklisted(ItemStack tool, ItemStack incoming) {
        return !incoming.isEmpty() && blacklist(tool).contains(BuiltInRegistries.ITEM.getKey(incoming.getItem()));
    }

    private static ItemStack sanitize(ItemStack requested) {
        if (!isSafeStack(requested)) return ItemStack.EMPTY;
        ItemStack result = requested.copy();
        result.setCount(Math.max(1, Math.min(result.getCount(), result.getMaxStackSize())));
        return result;
    }

    private static boolean isSafeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || isStorageTool(stack)) return false;
        ItemStack probe = stack.copy(); probe.setCount(1);
        int size = encodedSize(probe);
        return size > 0 && size <= MAX_STACK_NBT_BYTES;
    }

    private static long totalSize(List<ItemStack> items) {
        long total = encodedSize(new ListTag());
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) total += storageCost(i, stack);
        }
        return total;
    }

    public static boolean canStoreAt(List<ItemStack> items, int slot, ItemStack requested) {
        if (slot < 0 || slot >= items.size()) return false;
        if (requested == null || requested.isEmpty()) return true;
        ItemStack stack = sanitize(requested);
        if (stack.isEmpty()) return false;
        ItemStack old = items.get(slot);
        long candidate = totalSize(items) - (old.isEmpty() ? 0L : storageCost(slot, old)) + storageCost(slot, stack);
        return candidate <= MAX_TOTAL_NBT_BYTES;
    }

    private static CompoundTag storageEntry(int slot, ItemStack stack) {
        CompoundTag entry = new CompoundTag();
        entry.putInt("Slot", slot);
        entry.put("Stack", stack.save(new CompoundTag()));
        return entry;
    }

    private static int storageCost(int slot, ItemStack stack) {
        return encodedSize(storageEntry(slot, stack));
    }

    private static int encodedSize(Tag value) {
        if (value == null) return 0;
        CompoundTag probe = new CompoundTag();
        probe.put("Value", value.copy());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeNbt(probe);
            return buffer.readableBytes();
        } catch (RuntimeException exception) {
            return Integer.MAX_VALUE;
        } finally {
            buffer.release();
        }
    }

    private static int encodedSize(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0 : encodedSize(stack.save(new CompoundTag()));
    }
}
