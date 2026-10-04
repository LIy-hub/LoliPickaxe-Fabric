package com.liymod.client.mining;

import java.util.Set;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Mining presentation data travels with the native extracted frame. */
public interface LoliMiningRenderState {
    VoxelShape liymod$getMiningOutline();
    void liymod$setMiningOutline(VoxelShape outline);
    Set<Long> liymod$getImmediateMiningSections();
    void liymod$setImmediateMiningSections(Set<Long> sections);
}
