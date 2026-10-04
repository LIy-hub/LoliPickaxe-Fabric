package com.liymod.entity;

import com.liymod.block.ModBlocks;
import com.liymod.mixin.accessor.PrimedTntAccessor;
import com.liymod.safe.SafeTntEffect;
import com.liymod.safe.SafeTntEffectService;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class LoliPrimedTntEntity extends PrimedTnt {
    private static final net.minecraft.network.syncher.EntityDataAccessor<BlockState> LOLI_BLOCK_STATE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(LoliPrimedTntEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BLOCK_STATE);
    @Override protected void defineSynchedData() {
        super.defineSynchedData();entityData.define(LOLI_BLOCK_STATE,net.minecraft.world.level.block.Blocks.TNT.defaultBlockState());
    }
    public void setBlockState(BlockState state) { entityData.set(LOLI_BLOCK_STATE,state); }
    public BlockState getBlockState() { return entityData.get(LOLI_BLOCK_STATE); }
    @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);tag.put("block_state",net.minecraft.nbt.NbtUtils.writeBlockState(getBlockState()));
    }
    @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("block_state",net.minecraft.nbt.Tag.TAG_COMPOUND)) setBlockState(net.minecraft.nbt.NbtUtils.readBlockState(
                level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK).asLookup(),tag.getCompound("block_state")));
    }

    public LoliPrimedTntEntity(EntityType<? extends PrimedTnt> type, Level level) {
        super(type, level);
    }

    public LoliPrimedTntEntity(
            Level level,
            double x,
            double y,
            double z,
            LivingEntity owner,
            BlockState blockState
    ) {
        this(ModEntities.LOLI_PRIMED_TNT, level);
        setPos(x, y, z);
        double direction = level.getRandom().nextDouble() * (Math.PI * 2.0D);
        setDeltaMovement(-Math.sin(direction) * 0.02D, 0.2D, -Math.cos(direction) * 0.02D);
        setFuse(80);
        xo = x;
        yo = y;
        zo = z;
        if (owner != null) {
            ((PrimedTntAccessor) (Object) this).lolipickaxe$setOwner(owner);
        }
        setBlockState(blockState);
    }

    @Override
    public void tick() {
        int fuseBeforeTick = getFuse();
        super.tick();
        if (fuseBeforeTick == 1 && !level().isClientSide()) {
            applySafeEffectToNearbyPlayers(resolveEffect());
        }
    }

    private SafeTntEffect resolveEffect() {
        BlockState state = getBlockState();
        if (state.is(ModBlocks.LOLI_EXIT_TNT)) {
            return SafeTntEffect.EXIT;
        }
        if (state.is(ModBlocks.LOLI_FAIL_RESPOND_TNT)) {
            return SafeTntEffect.FAIL_RESPOND;
        }
        return SafeTntEffect.BLUE_SCREEN;
    }

    private void applySafeEffectToNearbyPlayers(SafeTntEffect effect) {
        AABB searchBox = new AABB(
                getX() - 5.0D,
                getY() - 5.0D,
                getZ() - 5.0D,
                getX() + 5.0D,
                getY() + 5.0D,
                getZ() + 5.0D
        );
        List<ServerPlayer> nearbyPlayers = level().getEntitiesOfClass(ServerPlayer.class, searchBox);
        for (ServerPlayer player : nearbyPlayers) {
            if (distanceToSqr(player) < 25.0D) {
                SafeTntEffectService.apply(player, effect);
            }
        }
    }
}
