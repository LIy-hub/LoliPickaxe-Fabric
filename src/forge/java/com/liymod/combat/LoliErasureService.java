package com.liymod.combat;

import com.liymod.LiyMod;
import com.liymod.compat.StrengthConfrontation;
import com.liymod.protection.LoliProtection;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class LoliErasureService {
    public enum Result { EXECUTED, IMMUNE, IGNORED }
    private LoliErasureService() { }

    public static Result execute(@Nullable Entity attacker, Entity target) {
        return execute(attacker, target, ExecutionAuthority.STANDARD);
    }

    public static Result executeAbsolute(@Nullable Entity attacker, Entity target) {
        return execute(attacker, target, ExecutionAuthority.ABSOLUTE_EXECUTION);
    }

    private static Result execute(@Nullable Entity attacker, Entity target, ExecutionAuthority authority) {
        if (!(target.level() instanceof ServerLevel level) || target == attacker) return Result.IGNORED;
        if (LoliProtection.isExecutionImmune(target) && LoliProtection.isProtected(attacker)) return immune(level, attacker, target);
        if (LoliProtection.isExecutionImmune(target) && !authority.piercesExecutionDefense()) return immune(level, attacker, target);

        var damageTypes = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        var loliType = damageTypes.getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,
                new ResourceLocation(LiyMod.MOD_ID, "loli_damage")));
        DamageSource source = attacker == null ? new DamageSource(loliType) : new DamageSource(loliType, attacker);
        LoliExecutionTicket ticket = LoliExecutionManager.begin(target, attacker, source, authority);
        if (ticket == null) return immune(level, attacker, target);
        if (ticket.state() == LoliExecutionTicket.State.DEAD_LOCK) {
            LoliExecutionManager.lock(target);
            return Result.EXECUTED;
        }
        if (authority == ExecutionAuthority.ABSOLUTE_EXECUTION) StrengthConfrontation.prepareAbsoluteExecution(target);

        try (LoliLegacyExecutionPolicy.PreparedExecution legacy = LoliLegacyExecutionPolicy.prepare(
                authority == ExecutionAuthority.ABSOLUTE_EXECUTION ? attacker : null, target)) {
            try {
                target.setInvulnerable(false);
                target.hurt(source, Float.MAX_VALUE);
            } catch (RuntimeException exception) {
                LiyMod.LOGGER.warn("Normal execution path was blocked for {}", target.getUUID(), exception);
            }
            if (LoliExecutionManager.abortForDefense(target)) return immune(level, attacker, target);
            LoliExecutionManager.beginCommit(target);
            if (target instanceof LivingEntity living) {
                if (!living.isDeadOrDying() && living.getHealth() > 0.0F) {
                    try { living.setHealth(0.0F); living.die(source); }
                    catch (RuntimeException exception) { LiyMod.LOGGER.warn("Fallback death was blocked for {}", target.getUUID(), exception); }
                }
                if (living.isDeadOrDying() || living.getHealth() <= 0.0F) LoliExecutionManager.markDeathCommitted(target);
            } else {
                LoliExecutionManager.markDeathCommitted(target);
            }
            if (LoliExecutionManager.abortForDefense(target)) return immune(level, attacker, target);
            LoliExecutionManager.lock(target);
            if (!LoliExecutionManager.isDeadLocked(target)) return Result.IGNORED;
            legacy.commit();
            StrengthConfrontation.armSuppression(target, authority == ExecutionAuthority.ABSOLUTE_EXECUTION);
            return Result.EXECUTED;
        }
    }

    private static Result immune(ServerLevel level, @Nullable Entity attacker, Entity target) {
        LoliImmunityFeedback.play(level, attacker, target);
        return Result.IMMUNE;
    }
}
