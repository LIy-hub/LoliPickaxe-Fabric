package com.liymod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;

/** Native 1.20.1 block/fluid states; no client or world is opened. */
public final class ForgeFluidMiningRegressionTest {
    public static void main(String[] args) {
        var water=Blocks.WATER.defaultBlockState();var lava=Blocks.LAVA.defaultBlockState();
        var slab=Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED,true);
        require(LoliFluidMining.isFluidBlock(water) && LoliFluidMining.isFluidBlock(lava),"Native fluids were not recognized");
        require(!LoliFluidMining.canMine(water,false) && LoliFluidMining.canMine(water,true),"Fluid selection option does not control mining");
        require(!LoliFluidMining.isFluidBlock(slab) && LoliFluidMining.canMine(slab,false),"Waterlogged solid was mistaken for a pure fluid");
        require(!LoliFluidMining.canMine(Blocks.AIR.defaultBlockState(),true),"Air entered a mining action");
        for(int radius=0;radius<=5;radius++) {
            int count=0;for(BlockPos ignored:LoliMiningRange.positions(new BlockPos(-16,64,16),radius)) count++;
            int width=radius*2+1;require(count==width*width*width,"Mining cube lost positions");
            var bounds=LoliMiningRange.outline(radius).bounds();require(bounds.minX==-radius && bounds.maxX==radius+1,"Preview differs from mining range");
        }
        System.out.println("FORGE_FLUID_MINING_OK water lava waterlogged air single range preview=PASS");
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
