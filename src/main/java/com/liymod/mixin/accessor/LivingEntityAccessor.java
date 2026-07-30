package com.liymod.mixin.accessor;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> lolipickaxe$getHealthTrackedData() {
        throw new AssertionError("Mixin accessor was not applied");
    }

    @Accessor("dead")
    void lolipickaxe$setDead(boolean dead);
}
