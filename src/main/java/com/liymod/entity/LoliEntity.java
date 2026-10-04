package com.liymod.entity;

import com.liymod.combat.LoliErasureService;
import com.liymod.mixin.accessor.LivingEntityAccessor;
import com.liymod.protection.LoliProtection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class LoliEntity extends PathfinderMob {
    private static final float WATER_SPEED_MULTIPLIER = 15.0F;
    private boolean dispersalRemoval;

    public LoliEntity(EntityType<? extends LoliEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                LivingEntity.class,
                10,
                true,
                false,
                this::isValidLoliTarget
        ));
    }

    private boolean isValidLoliTarget(LivingEntity target, ServerLevel level) {
        return target != this
                && !(target instanceof LoliEntity)
                && target.isAlive()
                && !LoliProtection.isProtected(target);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target)
                && !(target instanceof LoliEntity)
                && !LoliProtection.isProtected(target);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof LoliEntity || LoliProtection.isProtected(target)) {
            setTarget(null);
            return false;
        }
        return LoliErasureService.executeAbsolute(this, target)
                == LoliErasureService.Result.EXECUTED;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        // The legacy Loli may only leave the world through the dispersal action.
    }

    @Override
    public void tick() {
        super.tick();
        if (!dispersalRemoval) {
            setHealth(getMaxHealth());
            deathTime = 0;
            ((LivingEntityAccessor) (Object) this).lolipickaxe$setDead(false);
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        LivingEntity target = getTarget();
        if (isEffectiveAi() && isInWater() && target != null && target.isInWater()) {
            moveRelative(0.01F * WATER_SPEED_MULTIPLIER, travelVector);
            move(MoverType.SELF, getDeltaMovement());
            Vec3 dampedMovement = getDeltaMovement().scale(0.9D);
            double maximumWaterSpeed = getAttributeValue(Attributes.MOVEMENT_SPEED)
                    * WATER_SPEED_MULTIPLIER;
            if (dampedMovement.lengthSqr() > maximumWaterSpeed * maximumWaterSpeed) {
                dampedMovement = dampedMovement.normalize().scale(maximumWaterSpeed);
            }
            setDeltaMovement(dampedMovement);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public void checkBelowWorld() {
        if (getY() < level().getMinY() - 64.0D || getY() > level().getMaxY() + 64.0D) {
            stopRiding();
            teleportTo(getX(), 256.0D, getZ());
            return;
        }
        super.checkBelowWorld();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isBaby() {
        return true;
    }

    public void disperse() {
        dispersalRemoval = true;
        discard();
    }

    public boolean isDispersalRemovalAllowed() {
        return dispersalRemoval;
    }

    public boolean blocksRemoval(RemovalReason reason) {
        return !dispersalRemoval
                && reason != RemovalReason.UNLOADED_TO_CHUNK
                && reason != RemovalReason.UNLOADED_WITH_PLAYER
                && reason != RemovalReason.CHANGED_DIMENSION;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!blocksRemoval(reason)) {
            super.remove(reason);
        }
    }
}
