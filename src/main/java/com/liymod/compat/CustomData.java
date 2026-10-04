package com.liymod.compat;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Snapshots legacy item NBT, preserving vanilla attributes and enchantments on mutation. */
public record CustomData(CompoundTag tag) {
    public static final CustomData EMPTY=new CustomData(new CompoundTag());
    public static CustomData of(CompoundTag tag) { return new CustomData(tag.copy()); }
    public CompoundTag copyTag() { return tag.copy(); }
    public boolean isEmpty() { return tag.isEmpty(); }
    public static CustomData get(ItemStack stack) { return new CustomData(stack.hasTag()?stack.getTag():new CompoundTag()); }
    public static void update(Object ignored,ItemStack stack,Consumer<CompoundTag> update) {
        CompoundTag tag=get(stack).copyTag();update.accept(tag);stack.setTag(tag);
    }
}
