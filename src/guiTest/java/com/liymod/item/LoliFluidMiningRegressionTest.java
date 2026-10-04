package com.liymod.item;

import com.liymod.config.LoliConfigOption;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Real vanilla block/flowing-fluid shapes in an isolated map, with no user world or game input. */
public final class LoliFluidMiningRegressionTest {
    private static final BlockPos LIQUID = new BlockPos(1, 0, 0);
    private static final BlockPos SOLID = new BlockPos(3, 0, 0);
    private static final Vec3 EYE = new Vec3(0.5D, 0.1D, 0.5D);
    private static final Vec3 END = new Vec3(5.5D, 0.1D, 0.5D);

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        require(LoliConfigOption.byId("stop_on_liquid").orElseThrow() == LoliConfigOption.STOP_ON_LIQUID
                && !((Boolean) LoliConfigOption.STOP_ON_LIQUID.defaultValue()), "Legacy id/default changed");
        for (BlockState liquid : new BlockState[]{
                Blocks.WATER.defaultBlockState(), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 5),
                Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 5)
        }) {
            Fixture level = new Fixture();
            level.blocks.put(LIQUID, liquid);
            level.blocks.put(SOLID, Blocks.STONE.defaultBlockState());
            var ordinary = LoliFluidMining.clip(level, EYE, END, CollisionContext.empty(), false);
            var selected = LoliFluidMining.clip(level, EYE, END, CollisionContext.empty(), true);
            require(ordinary.getBlockPos().equals(SOLID), "Disabled selection must see the solid behind liquid");
            require(selected.getBlockPos().equals(LIQUID), "Source/flowing fluid must be selectable");
            require(LoliFluidMining.preferFluidHit(level, EYE, ordinary, selected) == selected,
                    "Closer fluid must replace the ordinary hit");
            var closer = new net.minecraft.world.phys.EntityHitResult(null, new Vec3(0.75D, 0.1D, 0.5D));
            require(LoliFluidMining.preferFluidHit(level, EYE, closer, selected) == closer,
                    "Fluid behind the nearest entity must not replace it");
            require(!LoliFluidMining.canMine(liquid, false) && LoliFluidMining.canMine(liquid, true),
                    "Range mining must follow the fluid option");
            require(!LoliFluidMining.clearFluid(level, LIQUID, liquid, false)
                    && level.getBlockState(LIQUID) == liquid, "Disabled mining changed liquid");
            require(LoliFluidMining.clearFluid(level, LIQUID, liquid, true)
                    && level.getBlockState(LIQUID).isAir(), "Enabled mining must remove liquid instead of restoring it");
            require((level.lastFlags & Block.UPDATE_CLIENTS) != 0
                    && (level.lastFlags & Block.UPDATE_NEIGHBORS) != 0, "Removal must synchronize and update neighbors");
        }
        BlockState waterlogged = Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        require(!LoliFluidMining.isFluidBlock(waterlogged)
                && LoliFluidMining.canMine(waterlogged, false) && LoliFluidMining.canMine(waterlogged, true),
                "Waterlogged solids must mine normally with either option");
        require(!LoliFluidMining.isFluidBlock(Blocks.KELP.defaultBlockState())
                && LoliFluidMining.canMine(Blocks.KELP.defaultBlockState(), false), "Underwater plants are real blocks");
        require(!LoliFluidMining.canMine(Blocks.AIR.defaultBlockState(), true), "Air must never count as a mined fluid");
        Fixture blocked = new Fixture();
        blocked.blocks.put(LIQUID, Blocks.STONE.defaultBlockState());
        blocked.blocks.put(SOLID, Blocks.WATER.defaultBlockState());
        require(LoliFluidMining.clip(blocked, EYE, END, CollisionContext.empty(), true).getBlockPos().equals(LIQUID),
                "Fluid selection must not see through a solid wall");
        var miss = LoliFluidMining.clip(new Fixture(), EYE, END, CollisionContext.empty(), true);
        require(miss.getType() == HitResult.Type.MISS, "Empty ray must remain a miss");
        verifyDirectReplacement();
        System.out.println("FLUID_MINING_OK source/flowing water/lava selection on/off removal neighbor/client flags"
                + " nearest entity solid occlusion waterlogged underwater blocks directReplacement legacy id/default=PASS");
    }

    private static void verifyDirectReplacement() {
        Fixture level = new Fixture();
        for (var state : new BlockState[]{Blocks.STONE.defaultBlockState(), Blocks.BEDROCK.defaultBlockState(),
                Blocks.CHEST.defaultBlockState(), Blocks.COMMAND_BLOCK.defaultBlockState()}) {
            level.blocks.put(SOLID, state);
            require(LoliBlockReplacement.remove(level, SOLID, state, false)
                            && level.getBlockState(SOLID).isAir(),
                    "A solid must become air through a direct world write, including restricted/container blocks");
            require((level.lastFlags & Block.UPDATE_CLIENTS) != 0
                            && (level.lastFlags & Block.UPDATE_NEIGHBORS) != 0,
                    "Direct writes must retain client and neighbor updates");
        }
        for (var state : new BlockState[]{Blocks.WATER.defaultBlockState(), Blocks.LAVA.defaultBlockState(),
                Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 5),
                Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 5)}) {
            level.blocks.put(LIQUID, state);
            require(!LoliBlockReplacement.remove(level, LIQUID, state, false)
                            && level.getBlockState(LIQUID) == state,
                    "Direct replacement must preserve disabled fluid targets");
            require(LoliBlockReplacement.remove(level, LIQUID, state, true)
                            && level.getBlockState(LIQUID).isAir(),
                    "Selected fluid must directly become air rather than restore itself");
        }
        for (var state : new BlockState[]{
                Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                Blocks.KELP.defaultBlockState()}) {
            level.blocks.put(SOLID, state);
            require(LoliBlockReplacement.remove(level, SOLID, state, false)
                            && level.getBlockState(SOLID) == state.getFluidState().createLegacyBlock(),
                    "Waterlogged and underwater solids must keep their established fluid state");
        }
        int previousWrites = level.writes;
        for (var air : new BlockState[]{Blocks.AIR.defaultBlockState(), Blocks.CAVE_AIR.defaultBlockState(),
                Blocks.VOID_AIR.defaultBlockState()}) {
            require(!LoliBlockReplacement.remove(level, SOLID, air, true) && level.writes == previousWrites,
                    "Every air variant must be skipped without any world updates");
        }
        level.acceptWrites = false;
        level.blocks.put(SOLID, Blocks.STONE.defaultBlockState());
        require(!LoliBlockReplacement.remove(level, SOLID, level.getBlockState(SOLID), false)
                        && level.getBlockState(SOLID).is(Blocks.STONE),
                "A failed write must not be reported as a successful replacement");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Fixture implements BlockGetter, LevelWriter {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();
        private int lastFlags;
        private int writes;
        private boolean acceptWrites = true;

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }

        @Override
        public boolean setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft) {
            if (!acceptWrites) return false;
            writes++;
            lastFlags = flags;
            blocks.put(pos.immutable(), state);
            return true;
        }

        @Override
        public boolean removeBlock(BlockPos pos, boolean moving) {
            throw new AssertionError("Fluid removal must not restore its old liquid state");
        }

        @Override
        public boolean destroyBlock(BlockPos pos, boolean drop, Entity entity, int recursionLeft) {
            throw new AssertionError("Vanilla destroyBlock cannot clear a pure fluid");
        }
    }
}

