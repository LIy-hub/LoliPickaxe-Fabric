package com.liymod.combat;

import com.liymod.LiyMod;
import com.liymod.compat.StrengthConfrontation;
import com.liymod.damage_type.ModDamageSources;
import com.liymod.protection.LoliProtection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.Nullable;

public final class LoliErasureService {
    public enum Result {
        EXECUTED,
        IMMUNE,
        IGNORED
    }

    private LoliErasureService() {
    }

    public static Result execute(@Nullable Entity attacker, Entity target) {
        return execute(attacker, target, ExecutionAuthority.STANDARD);
    }

    public static Result executeAbsolute(@Nullable Entity attacker, Entity target) {
        return execute(attacker, target, ExecutionAuthority.ABSOLUTE_EXECUTION);
    }

    private static Result execute(
            @Nullable Entity attacker,
            Entity target,
            ExecutionAuthority authority
    ) {
        if (!(target.level() instanceof ServerLevel serverWorld)
                || target == attacker) {
            return Result.IGNORED;
        }
        if (LoliProtection.isExecutionImmune(target)
                && LoliProtection.isProtected(attacker)) {
            return immune(serverWorld, attacker, target);
        }
        if (LoliProtection.isExecutionImmune(target)
                && !authority.piercesExecutionDefense()) {
            return immune(serverWorld, attacker, target);
        }

        if (authority == ExecutionAuthority.ABSOLUTE_EXECUTION) {
            StrengthConfrontation.prepareAbsoluteExecution(target);
        }

        DamageSource source = ModDamageSources.loli(serverWorld, attacker);
        LoliExecutionTicket ticket = LoliExecutionManager.begin(
                target,
                attacker,
                source,
                authority
        );
        if (ticket == null) {
            return immune(serverWorld, attacker, target);
        }

        if (ticket.state() == LoliExecutionTicket.State.DEAD_LOCK) {
            LoliExecutionManager.lock(target);
            if (authority == ExecutionAuthority.ABSOLUTE_EXECUTION) {
                StrengthConfrontation.onAbsoluteDeadLock(target);
            }
            return Result.EXECUTED;
        }

        try (LoliLegacyExecutionPolicy.PreparedExecution legacyExecution =
                     LoliLegacyExecutionPolicy.prepare(
                             authority == ExecutionAuthority.ABSOLUTE_EXECUTION ? attacker : null,
                             target
                     )) {
            int playerDeathsBefore = getPlayerDeathCount(target);
            tryNormalDamage(target, source);
            recordObservedPlayerDeath(target, playerDeathsBefore);
            if (LoliExecutionManager.abortForDefense(target)) {
                return immune(serverWorld, attacker, target);
            }

            if (target instanceof LivingEntity living) {
                if (!LoliExecutionManager.isDeathCommitted(target)) {
                    LoliExecutionManager.beginCommit(target);
                    if (LoliExecutionManager.abortForDefense(target)) {
                        return immune(serverWorld, attacker, target);
                    }
                    tryNormalDeath(living, source);
                    recordObservedPlayerDeath(target, playerDeathsBefore);
                    if (LoliExecutionManager.abortForDefense(target)) {
                        return immune(serverWorld, attacker, target);
                    }
                }

                if (!LoliExecutionManager.isDeathCommitted(target)) {
                    if (LoliExecutionManager.abortForDefense(target)) {
                        return immune(serverWorld, attacker, target);
                    }
                    forceFallbackDeath(living, source);
                    if (LoliExecutionManager.abortForDefense(target)) {
                        return immune(serverWorld, attacker, target);
                    }
                }
            } else {
                LoliExecutionManager.beginCommit(target);
                if (LoliExecutionManager.abortForDefense(target)) {
                    return immune(serverWorld, attacker, target);
                }
                LoliExecutionManager.markDeathCommitted(target);
            }

            if (LoliExecutionManager.abortForDefense(target)) {
                return immune(serverWorld, attacker, target);
            }
            LoliExecutionManager.lock(target);
            if (LoliExecutionManager.abortForDefense(target)) {
                return immune(serverWorld, attacker, target);
            }
            if (!LoliExecutionManager.isDeadLocked(target)) {
                return Result.IGNORED;
            }
            legacyExecution.commit();
            if (authority == ExecutionAuthority.ABSOLUTE_EXECUTION) {
                StrengthConfrontation.onAbsoluteDeadLock(target);
            }
            return Result.EXECUTED;
        }
    }

    private static Result immune(
            ServerLevel world,
            @Nullable Entity attacker,
            Entity protectedTarget
    ) {
        LoliImmunityFeedback.play(world, attacker, protectedTarget);
        return Result.IMMUNE;
    }

    private static void tryNormalDamage(Entity target, DamageSource source) {
        try {
            target.hurt(source, Float.MAX_VALUE);
        } catch (RuntimeException exception) {
            LiyMod.LOGGER.warn(
                    "Normal damage path failed for {}; continuing with forced execution",
                    target.getUUID(),
                    exception
            );
        }
    }

    private static void tryNormalDeath(LivingEntity target, DamageSource source) {
        try {
            target.die(source);
        } catch (RuntimeException exception) {
            LiyMod.LOGGER.warn(
                    "Normal death path failed for {}; continuing with forced execution",
                    target.getUUID(),
                    exception
            );
        }
    }

    private static void forceFallbackDeath(LivingEntity target, DamageSource source) {
        if (target instanceof ServerPlayer player) {
            forcePlayerDeath(player, source);
        } else {
            target.gameEvent(GameEvent.ENTITY_DIE);
            target.level().broadcastEntityEvent(
                    target,
                    EntityEvent.DEATH
            );
        }
        LoliExecutionManager.markDeathCommitted(target);
    }

    private static int getPlayerDeathCount(Entity target) {
        if (!(target instanceof ServerPlayer player)) {
            return -1;
        }
        return player.getStats().getValue(Stats.CUSTOM, Stats.DEATHS);
    }

    private static void recordObservedPlayerDeath(Entity target, int deathsBefore) {
        if (!(target instanceof ServerPlayer player) || deathsBefore < 0) {
            return;
        }
        if (player.getStats().getValue(Stats.CUSTOM, Stats.DEATHS) > deathsBefore) {
            LoliExecutionManager.markDeathCommitted(player);
        }
    }

    private static void forcePlayerDeath(ServerPlayer player, DamageSource source) {
        Component deathMessage = source.getLocalizedDeathMessage(player);
        player.connection.send(new ClientboundPlayerCombatKillPacket(player.getId(), deathMessage));
        player.connection.send(
                new ClientboundSetHealthPacket(
                        0.0F,
                        player.getFoodData().getFoodLevel(),
                        player.getFoodData().getSaturationLevel()
                )
        );

        player.level().getServer().getScoreboard().forAllObjectives(
                ObjectiveCriteria.DEATH_COUNT,
                player,
                ScoreAccess::increment
        );
        player.awardStat(Stats.DEATHS);

        if (source.getEntity() instanceof ServerPlayer killer && killer != player) {
            killer.awardKillScore(player, source);
        }

        player.gameEvent(GameEvent.ENTITY_DIE);
        player.level().broadcastEntityEvent(
                player,
                EntityEvent.DEATH
        );
        LiyMod.LOGGER.info(
                "Forced fallback death commit for {} after its normal death path was blocked",
                player.getGameProfile().name()
        );
    }
}
