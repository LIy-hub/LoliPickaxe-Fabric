package com.liymod.mixin.client;

import com.liymod.compat.LegacyReach;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(GameRenderer.class)
public abstract class LegacyGameRendererMixin {
    @ModifyConstant(method="pick",constant=@Constant(doubleValue=6.0D))
    private double liymod$entityPickDistance(double vanilla) {
        double reach=LegacyReach.configured(Minecraft.getInstance().player);
        return reach>0.0D?reach:vanilla;
    }
}
