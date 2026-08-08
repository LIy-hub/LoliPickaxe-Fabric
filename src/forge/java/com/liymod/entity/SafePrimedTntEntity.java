package com.liymod.entity;

import com.liymod.registry.ModContent;
import com.liymod.safe.SafeEffect;
import com.liymod.safe.SafeEffectService;
import com.liymod.mixin.PrimedTntAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import javax.annotation.Nullable;

public final class SafePrimedTntEntity extends PrimedTnt {
    private static final EntityDataAccessor<Integer> EFFECT = SynchedEntityData.defineId(
            SafePrimedTntEntity.class, EntityDataSerializers.INT);
    public SafePrimedTntEntity(EntityType<? extends SafePrimedTntEntity> type, Level level) { super(type, level); }
    public SafePrimedTntEntity(Level level, double x, double y, double z, SafeEffect effect) {
        this(level, x, y, z, effect, null);
    }
    public SafePrimedTntEntity(Level level, double x, double y, double z, SafeEffect effect, @Nullable LivingEntity owner) {
        this(ModContent.SAFE_PRIMED_TNT.get(), level);
        setEffect(effect);
        if (owner != null) ((PrimedTntAccessor) (Object) this).liymod$setOwner(owner);
        setPos(x, y, z);
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        setDeltaMovement(-Math.sin(angle) * 0.02D, 0.2D, -Math.cos(angle) * 0.02D);
        setFuse(80); xo = x; yo = y; zo = z;
    }
    @Override public void tick() {
        int before = getFuse();
        super.tick();
        if (before == 1 && !level().isClientSide) for (ServerPlayer player : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(5.0D))) {
            if (distanceToSqr(player) < 25.0D) SafeEffectService.apply(player, effect());
        }
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(EFFECT, SafeEffect.BLUE_SCREEN.ordinal()); }
    public SafeEffect effect() {
        int index = entityData.get(EFFECT);
        SafeEffect[] values = SafeEffect.values();
        return index >= 0 && index < values.length ? values[index] : SafeEffect.BLUE_SCREEN;
    }
    private void setEffect(SafeEffect effect) { entityData.set(EFFECT, effect == null ? 0 : effect.ordinal()); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putString("LoliEffect", effect().name()); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); try { setEffect(SafeEffect.valueOf(tag.getString("LoliEffect"))); } catch (IllegalArgumentException ignored) { setEffect(SafeEffect.BLUE_SCREEN); } }
}
