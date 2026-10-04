package com.liymod.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;

/** Native immutable component access; callers must never mutate the returned tag. */
public interface LoliCustomDataView {
    CompoundTag liymod$readOnlyTag();
    CustomData liymod$getNetworkSnapshot();
    void liymod$setNetworkSnapshot(CustomData snapshot);
}
