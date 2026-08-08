package com.liymod.block;

import com.liymod.entity.SafePrimedTntEntity;
import com.liymod.safe.SafeEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class SafeTntBlock extends TntBlock {
    private final SafeEffect effect;
    public SafeTntBlock(SafeEffect effect, BlockBehaviour.Properties properties) { super(properties); this.effect = effect; }
    @Override public void onCaughtFire(BlockState state, Level level, BlockPos pos, Direction face, LivingEntity igniter) {
        if (!level.isClientSide) spawn(level, pos, 80, igniter);
    }
    @Override public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide) spawn(level, pos, level.random.nextInt(20) + 10, explosion.getIndirectSourceEntity());
    }
    private void spawn(Level level, BlockPos pos, int fuse, LivingEntity owner) {
        SafePrimedTntEntity primed = new SafePrimedTntEntity(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, effect, owner);
        primed.setFuse(fuse);
        level.addFreshEntity(primed);
        level.playSound(null, primed.getX(), primed.getY(), primed.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
