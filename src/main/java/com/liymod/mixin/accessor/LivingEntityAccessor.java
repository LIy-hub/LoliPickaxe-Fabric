package com.liymod.mixin.accessor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("HEALTH")
    static TrackedData<Float> lolipickaxe$getHealthTrackedData() {
        throw new AssertionError("Mixin accessor was not applied");
    }

    @Accessor("dead")
    void lolipickaxe$setDead(boolean dead);
}
