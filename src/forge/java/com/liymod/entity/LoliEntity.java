package com.liymod.entity;

import com.liymod.combat.LoliErasureService;
import com.liymod.config.LoliServerConfig;
import com.liymod.protection.LoliProtection;
import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class LoliEntity extends PathfinderMob {
    private static final double WATER_SPEED_MULTIPLIER = 15.0D;
    private boolean dispersal;

    public LoliEntity(EntityType<? extends LoliEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                target -> target != this && !(target instanceof LoliEntity) && target.isAlive() && !LoliProtection.isProtected(target)));
    }

    @Override public boolean canAttack(LivingEntity target) {
        return LoliServerConfig.bool("loli_attack") && super.canAttack(target) && !(target instanceof LoliEntity) && !LoliProtection.isProtected(target);
    }

    @Override public void setTarget(@Nullable LivingEntity target) {
        LivingEntity previous = getTarget();
        super.setTarget(target);
        if (target == null || target == previous || !LoliServerConfig.bool("loli_attack")
                || !LoliServerConfig.bool("loli_teleport") || !canAttack(target)) return;
        var position = target.blockPosition();
        if (level().getWorldBorder().isWithinBounds(position) && level().hasChunkAt(position)
                && position.getY() >= level().getMinBuildHeight() && position.getY() < level().getMaxBuildHeight()) {
            teleportTo(target.getX(), target.getY(), target.getZ());
        }
    }

    @Override public boolean doHurtTarget(Entity target) {
        if (target instanceof LoliEntity || LoliProtection.isProtected(target)) { setTarget(null); return false; }
        return LoliErasureService.executeAbsolute(this, target) == LoliErasureService.Result.EXECUTED;
    }

    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public void die(DamageSource source) { }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean requiresCustomPersistence() { return true; }
    @Override public boolean canBeLeashed(Player player) { return false; }
    @Override public boolean isBaby() { return true; }

    @Override public void travel(Vec3 travelVector) {
        LivingEntity target = getTarget();
        if (isEffectiveAi() && isInWater() && target != null && target.isInWater()) {
            moveRelative((float) (0.01D * WATER_SPEED_MULTIPLIER), travelVector);
            move(MoverType.SELF, getDeltaMovement());
            Vec3 movement = getDeltaMovement().scale(0.9D);
            double maximum = getAttributeValue(Attributes.MOVEMENT_SPEED) * WATER_SPEED_MULTIPLIER;
            if (movement.lengthSqr() > maximum * maximum) movement = movement.normalize().scale(maximum);
            setDeltaMovement(movement);
            return;
        }
        super.travel(travelVector);
    }

    @Override public void tick() {
        super.tick();
        if (!dispersal) {
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(LoliServerConfig.number("loli_speed"));
            setHealth(getMaxHealth());
            deathTime = 0;
            if (getY() < level().getMinBuildHeight() - 64.0D || getY() > level().getMaxBuildHeight() + 64.0D) teleportTo(getX(), 256.0D, getZ());
        }
    }

    public void disperse() { dispersal = true; super.remove(RemovalReason.DISCARDED); }
    public boolean isDispersal() { return dispersal; }

    @Override public void remove(RemovalReason reason) {
        if (dispersal || reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER || reason == RemovalReason.CHANGED_DIMENSION) super.remove(reason);
    }
}
