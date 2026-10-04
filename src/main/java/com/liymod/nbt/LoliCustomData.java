package com.liymod.nbt;

import com.liymod.compat.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import com.liymod.compat.CustomData;

public final class LoliCustomData {
    private LoliCustomData() {}

    /** Read only. Keep writes in CustomData.update/of so existing component snapshots stay immutable. */
    public static CompoundTag view(ItemStack stack) {
        return view(com.liymod.compat.LegacyComponents.getOrDefault(stack, DataComponents.CUSTOM_DATA, CustomData.EMPTY));
    }

    public static CompoundTag view(CustomData data) {
        return data.tag();
    }

    /** A private mutable root sharing immutable children; CustomData.of copies it before publication. */
    public static CompoundTag copyRoot(CompoundTag source) {
        CompoundTag copy = new CompoundTag();
        for (String key : source.getAllKeys()) copy.put(key, source.get(key));
        return copy;
    }
}
