package com.liymod.mixin;

import com.liymod.combat.LoliExecutionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayerGameMode.class, priority = Integer.MAX_VALUE)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$blockBreakingWhileDead(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (LoliExecutionManager.isDeadLocked(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$blockItemUseWhileDead(
            ServerPlayer interactingPlayer,
            Level world,
            ItemStack stack,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (LoliExecutionManager.isDeadLocked(player)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
