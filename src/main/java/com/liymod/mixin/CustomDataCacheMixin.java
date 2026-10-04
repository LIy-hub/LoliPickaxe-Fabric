package com.liymod.mixin;

import com.liymod.nbt.LoliCustomDataView;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CustomData.class)
public abstract class CustomDataCacheMixin implements LoliCustomDataView {
    @Shadow @Final private CompoundTag tag;
    @Unique private volatile CustomData liymod$networkSnapshot;

    @Override public CompoundTag liymod$readOnlyTag() { return tag; }
    @Override public CustomData liymod$getNetworkSnapshot() { return liymod$networkSnapshot; }
    @Override public void liymod$setNetworkSnapshot(CustomData snapshot) { liymod$networkSnapshot = snapshot; }
}
