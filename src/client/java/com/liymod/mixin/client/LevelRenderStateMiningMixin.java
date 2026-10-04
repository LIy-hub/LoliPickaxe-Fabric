package com.liymod.mixin.client;

import com.liymod.client.mining.LoliMiningRenderState;
import java.util.Set;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderState.class)
public abstract class LevelRenderStateMiningMixin implements LoliMiningRenderState {
    @Unique private VoxelShape liymod$miningOutline;
    @Unique private Set<Long> liymod$immediateMiningSections = Set.of();

    public VoxelShape liymod$getMiningOutline() { return liymod$miningOutline; }
    public void liymod$setMiningOutline(VoxelShape outline) { liymod$miningOutline = outline; }
    public Set<Long> liymod$getImmediateMiningSections() { return liymod$immediateMiningSections; }
    public void liymod$setImmediateMiningSections(Set<Long> sections) { liymod$immediateMiningSections = sections; }

    @Inject(method = "reset", at = @At("HEAD"))
    private void liymod$resetMiningState(CallbackInfo ci) {
        liymod$miningOutline = null;
        liymod$immediateMiningSections = Set.of();
    }
}
