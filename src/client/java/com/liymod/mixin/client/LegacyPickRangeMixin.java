package com.liymod.mixin.client;

import com.liymod.compat.LegacyReach;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class LegacyPickRangeMixin {
    @Inject(method="getPickRange",at=@At("RETURN"),cancellable=true)
    private void liymod$blockReach(CallbackInfoReturnable<Float> cir) {
        double reach=LegacyReach.configured(Minecraft.getInstance().player);
        if(reach>0.0D) cir.setReturnValue((float)reach);
    }
    @Inject(method="hasFarPickRange",at=@At("RETURN"),cancellable=true)
    private void liymod$entityReach(CallbackInfoReturnable<Boolean> cir) {
        if(LegacyReach.configured(Minecraft.getInstance().player)>0.0D) cir.setReturnValue(true);
    }
}
