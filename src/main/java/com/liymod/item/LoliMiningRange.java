package com.liymod.item;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The same origin-centered cube defines server candidates and the client preview. */
public final class LoliMiningRange {
    private static final List<VoxelShape> OUTLINES = IntStream.rangeClosed(0, 5)
            .mapToObj(radius -> Shapes.box(-radius, -radius, -radius, radius + 1, radius + 1, radius + 1))
            .toList();

    private LoliMiningRange() {
    }

    public static Iterable<BlockPos> positions(BlockPos origin, int radius) {
        int bounded = Mth.clamp(radius, 0, 5);
        return BlockPos.betweenClosed(origin.offset(-bounded, -bounded, -bounded),
                origin.offset(bounded, bounded, bounded));
    }

    public static VoxelShape outline(int radius) {
        return OUTLINES.get(Mth.clamp(radius, 0, 5));
    }
}
