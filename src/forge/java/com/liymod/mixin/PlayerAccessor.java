package com.liymod.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface PlayerAccessor {
    @Accessor("attackStrengthTicker")
    int liymod$getAttackStrengthTicker();

    @Accessor("attackStrengthTicker")
    void liymod$setAttackStrengthTicker(int value);
}
