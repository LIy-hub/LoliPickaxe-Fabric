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
        verifySelectedOutline(water,slab);
        for(int radius=0;radius<=5;radius++) {
            int count=0;for(BlockPos ignored:LoliMiningRange.positions(new BlockPos(-16,64,16),radius)) count++;
            int width=radius*2+1;require(count==width*width*width,"Mining cube lost positions");
            var bounds=LoliMiningRange.outline(radius).bounds();require(bounds.minX==-radius && bounds.maxX==radius+1,"Preview differs from mining range");
        }
        System.out.println("FORGE_FLUID_MINING_OK water lava waterlogged air single range preview selectedSolid selectedFluidShape previewIndependence=PASS");
    }
    private static void verifySelectedOutline(net.minecraft.world.level.block.state.BlockState water,net.minecraft.world.level.block.state.BlockState slab) {
        var origin=new BlockPos(0,64,0);
        var level=(net.minecraft.world.level.BlockGetter)java.lang.reflect.Proxy.newProxyInstance(
                ForgeFluidMiningRegressionTest.class.getClassLoader(),new Class<?>[]{net.minecraft.world.level.BlockGetter.class},(proxy,method,args)->switch(method.getName()) {
                    case "getFluidState" -> origin.equals(args[0])?water.getFluidState():Blocks.AIR.defaultBlockState().getFluidState();
                    case "getBlockState" -> origin.equals(args[0])?water:Blocks.AIR.defaultBlockState();
                    case "getBlockEntity" -> null;
                    case "getHeight" -> 384;
                    case "getMinBuildHeight" -> -64;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        for(boolean preview:new boolean[]{false,true}) for(int radius:new int[]{0,2}) {
            var fluid=com.liymod.client.MiningOutline.plan(level,origin,water,true,preview,radius);
            require(fluid.selectedFluid()!=null && !fluid.selectedFluid().isEmpty() && fluid.selectedFluid().bounds().maxY<1.0D,
                    "Selected fluid lost its actual surface shape when preview/radius changed");
            require((fluid.range()!=null)==(preview && radius>0),"Range must be independent and absent in single mode");
            for(var solid:new net.minecraft.world.level.block.state.BlockState[]{slab,Blocks.OAK_STAIRS.defaultBlockState(),Blocks.STONE.defaultBlockState()}) {
                var selected=com.liymod.client.MiningOutline.plan(level,origin,solid,true,preview,radius);
                require(selected.selectedFluid()==null,"A solid/slab/stair outline replaced vanilla selection");
                require((selected.range()!=null)==(preview && radius>0),"Solid range preview lost independent selection");
            }
        }
        require(com.liymod.client.MiningOutline.plan(level,origin,water,false,true,2).selectedFluid()==null,
                "Disabled fluid selection replaced a native outline");
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
