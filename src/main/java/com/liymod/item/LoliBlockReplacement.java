package com.liymod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Direct world writes avoid destroyBlock's per-block break sound/particle packets. */
final class LoliBlockReplacement {
    private LoliBlockReplacement() { }

    static boolean remove(LevelWriter level, BlockPos pos, BlockState state, boolean selectFluids) {
        if (!LoliFluidMining.canMine(state, selectFluids)) return false;
        // Keep the established waterlogged-solid behavior; selected pure liquids become air.
        BlockState replacement = LoliFluidMining.isFluidBlock(state)
                ? Blocks.AIR.defaultBlockState() : state.getFluidState().createLegacyBlock();
        return level.setBlock(pos, replacement, Block.UPDATE_ALL);
    }
}
