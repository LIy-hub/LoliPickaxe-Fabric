package com.liymod.mixin;

import com.liymod.combat.LoliExecutionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayerInteractionManager.class, priority = Integer.MAX_VALUE)
public abstract class ServerPlayerInteractionManagerMixin {
    @Shadow
    protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$blockBreakingWhileDead(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (LoliExecutionManager.isDeadLocked(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void lolipickaxe$blockItemUseWhileDead(
            ServerPlayerEntity interactingPlayer,
            World world,
            ItemStack stack,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (LoliExecutionManager.isDeadLocked(player)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
