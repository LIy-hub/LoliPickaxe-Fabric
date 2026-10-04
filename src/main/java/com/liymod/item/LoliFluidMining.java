package com.liymod.item;

import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliItemSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Shared fluid selection and removal rules; the persisted option keeps its legacy id. */
public final class LoliFluidMining {
    private LoliFluidMining() {
    }

    public static boolean isEnabled(ItemStack tool) {
        return LoliItemSettings.isFinalPickaxe(tool)
                && LoliItemSettings.getBoolean(tool, LoliConfigOption.STOP_ON_LIQUID);
    }

    /** Waterlogged/underwater blocks have a different block from their contained fluid. */
    public static boolean isFluidBlock(BlockState state) {
        var fluid = state.getFluidState();
        return !fluid.isEmpty() && state.getBlock() == fluid.createLegacyBlock().getBlock();
    }

    public static boolean canMine(BlockState state, boolean selectFluids) {
        return !state.isAir() && (selectFluids || !isFluidBlock(state));
    }

    /** Vanilla destroyBlock restores the same liquid; pure fluids must explicitly become air. */
    public static boolean clearFluid(LevelWriter level, BlockPos pos, BlockState state, boolean selectFluids) {
        return selectFluids && isFluidBlock(state)
                && level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    public static BlockHitResult clip(
            BlockGetter level, Vec3 from, Vec3 to, CollisionContext context, boolean selectFluids
    ) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE,
                selectFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, context));
    }

    /** Keep the nearest vanilla entity/solid hit, including the item's native attack-range path. */
    public static HitResult preferFluidHit(BlockGetter level, Vec3 eye, HitResult original, BlockHitResult candidate) {
        if (candidate.getType() != HitResult.Type.BLOCK
                || level.getFluidState(candidate.getBlockPos()).isEmpty()) {
            return original;
        }
        if (original.getType() == HitResult.Type.MISS
                || candidate.getLocation().distanceToSqr(eye) < original.getLocation().distanceToSqr(eye)) {
            return candidate;
        }
        return original;
    }
}
