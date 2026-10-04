package com.liymod.mixin.client;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Native player-priority dirty sections compile synchronously at the next render pass. */
@Mixin(LevelRenderer.class)
public interface LevelRendererMiningAccess {
    @Invoker("setSectionDirty") void liymod$markSection(int x,int y,int z,boolean playerChanged);
}
