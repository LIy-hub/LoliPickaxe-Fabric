package com.liymod.mixin;

import com.liymod.storage.LoliStorageData;
import com.liymod.storage.LoliStorageHolder;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStack.class)
public abstract class ItemStackStorageMixin implements LoliStorageHolder {
    @Unique private LoliStorageData liymod$storage;
    @Override public LoliStorageData liymod$getStorage() { return liymod$storage; }
    @Override public void liymod$setStorage(LoliStorageData storage) { liymod$storage = storage; }
}
