package com.liymod.mixin.accessor;

import net.minecraft.entity.Entity;
import net.minecraft.world.entity.EntityChangeListener;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("removalReason")
    void lolipickaxe$setRemovalReason(@Nullable Entity.RemovalReason reason);

    @Accessor("changeListener")
    EntityChangeListener lolipickaxe$getChangeListener();
}
