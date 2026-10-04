package com.liymod.mixin.client;

import com.liymod.client.LoliMiningClient;
import com.liymod.config.FinalToolSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerMiningMixin {
    @Inject(method={"startDestroyBlock","continueDestroyBlock"},at=@At("HEAD"),cancellable=true)
    private void liymod$mine(BlockPos pos,Direction direction,CallbackInfoReturnable<Boolean> cir) {
        Minecraft client=Minecraft.getInstance();
        if(client.player!=null && !client.player.isSpectator() && FinalToolSettings.isFinal(client.player.getMainHandItem())) {
            LoliMiningClient.attack(pos);cir.setReturnValue(false);
        }
    }
}
