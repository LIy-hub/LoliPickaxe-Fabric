package com.liymod.block;

import com.liymod.entity.LoliPrimedTntEntity;
import com.liymod.safe.SafeTntEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

public final class BuffAttackTntBlock extends TntBlock {
    private final SafeTntEffect effect;

    public BuffAttackTntBlock(SafeTntEffect effect, BlockBehaviour.Properties properties) {
        super(properties);
        this.effect = effect;
    }

    public SafeTntEffect effect() {
        return effect;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        if (oldState.is(state.getBlock())) {
            return;
        }
        if (level.hasNeighborSignal(pos) && prime(level, pos, null)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            Orientation orientation,
            boolean movedByPiston
    ) {
        if (level.hasNeighborSignal(pos) && prime(level, pos, null)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && !player.getAbilities().instabuild
                && state.getValue(UNSTABLE)) {
            prime(level, pos, player);
        }
        return super.playerWillDestroy(level, pos, state.setValue(UNSTABLE, false), player);
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        if (!level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return;
        }
        LoliPrimedTntEntity primed = createPrimed(
                level,
                pos,
                explosion.getIndirectSourceEntity()
        );
        primed.setFuse(PrimedTntFuse.randomShortFuse(primed.getFuse(), level));
        level.addFreshEntity(primed);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }

        if (prime(level, pos, player)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            Item usedItem = stack.getItem();
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            } else {
                stack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(usedItem));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onProjectileHit(
            Level level,
            BlockState state,
            BlockHitResult hit,
            Projectile projectile
    ) {
        if (!(level instanceof ServerLevel serverLevel) || !projectile.isOnFire()) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!projectile.mayInteract(serverLevel, pos)) {
            return;
        }
        Entity owner = projectile.getOwner();
        LivingEntity igniter = owner instanceof LivingEntity living ? living : null;
        if (prime(level, pos, igniter)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    private boolean prime(Level level, BlockPos pos, LivingEntity igniter) {
        if (!(level instanceof ServerLevel serverLevel)
                || !serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return false;
        }

        LoliPrimedTntEntity primed = createPrimed(level, pos, igniter);
        level.addFreshEntity(primed);
        level.playSound(
                null,
                primed.getX(),
                primed.getY(),
                primed.getZ(),
                SoundEvents.TNT_PRIMED,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
        return true;
    }

    private LoliPrimedTntEntity createPrimed(Level level, BlockPos pos, LivingEntity igniter) {
        return new LoliPrimedTntEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                igniter,
                defaultBlockState()
        );
    }

    private static final class PrimedTntFuse {
        private PrimedTntFuse() {
        }

        private static int randomShortFuse(int fuse, ServerLevel level) {
            return net.minecraft.world.entity.item.PrimedTnt.getRandomShortFuse(fuse, level.getRandom());
        }
    }
}
