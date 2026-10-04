package com.liymod.nbt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class LoliCustomData {
    private LoliCustomData() {}

    /** Read only. Keep writes in CustomData.update/of so existing component snapshots stay immutable. */
    public static CompoundTag view(ItemStack stack) {
        return view(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY));
    }

    public static CompoundTag view(CustomData data) {
        return (Object) data instanceof LoliCustomDataView access
                ? access.liymod$readOnlyTag() : data.copyTag();
    }

    /** A private mutable root sharing immutable children; CustomData.of copies it before publication. */
    public static CompoundTag copyRoot(CompoundTag source) {
        CompoundTag copy = new CompoundTag();
        for (var entry : source.getAllKeys()) copy.put(entry, source.get(entry));
        return copy;
    }
}
