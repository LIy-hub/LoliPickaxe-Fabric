package com.liymod.mixin.client;
import com.liymod.item.LoliFluidMining;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
/** Use vanilla block/entity reach and nearest-hit logic with the selected fluid clipping mode. */
@Mixin(Entity.class)
public abstract class LocalPlayerFluidSelectionMixin {
    @ModifyVariable(method = "pick(DFZ)Lnet/minecraft/world/phys/HitResult;", at = @At("HEAD"), argsOnly = true)
    private boolean liymod$selectFluids(boolean includeFluids) {
        return includeFluids || ((Object) this instanceof LocalPlayer player && !player.isSpectator()
                && LoliFluidMining.isEnabled(player.getMainHandItem()));
    }
}
