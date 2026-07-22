package com.liymod.combat;

import com.liymod.LiyMod;
import com.liymod.damage_type.ModDamageSources;
import com.liymod.protection.LoliProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.world.event.GameEvent;
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
        if (!(target.getWorld() instanceof ServerWorld serverWorld)
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
            return Result.EXECUTED;
        }

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
        return Result.EXECUTED;
    }

    private static Result immune(
            ServerWorld world,
            @Nullable Entity attacker,
            Entity protectedTarget
    ) {
        LoliImmunityFeedback.play(world, attacker, protectedTarget);
        return Result.IMMUNE;
    }

    private static void tryNormalDamage(Entity target, DamageSource source) {
        try {
            target.damage(source, Float.MAX_VALUE);
        } catch (RuntimeException exception) {
            LiyMod.LOGGER.warn(
                    "Normal damage path failed for {}; continuing with forced execution",
                    target.getUuid(),
                    exception
            );
        }
    }

    private static void tryNormalDeath(LivingEntity target, DamageSource source) {
        try {
            target.onDeath(source);
        } catch (RuntimeException exception) {
            LiyMod.LOGGER.warn(
                    "Normal death path failed for {}; continuing with forced execution",
                    target.getUuid(),
                    exception
            );
        }
    }

    private static void forceFallbackDeath(LivingEntity target, DamageSource source) {
        if (target instanceof ServerPlayerEntity player) {
            forcePlayerDeath(player, source);
        } else {
            target.emitGameEvent(GameEvent.ENTITY_DIE);
            target.getWorld().sendEntityStatus(
                    target,
                    EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES
            );
        }
        LoliExecutionManager.markDeathCommitted(target);
    }

    private static int getPlayerDeathCount(Entity target) {
        if (!(target instanceof ServerPlayerEntity player)) {
            return -1;
        }
        return player.getStatHandler().getStat(Stats.CUSTOM, Stats.DEATHS);
    }

    private static void recordObservedPlayerDeath(Entity target, int deathsBefore) {
        if (!(target instanceof ServerPlayerEntity player) || deathsBefore < 0) {
            return;
        }
        if (player.getStatHandler().getStat(Stats.CUSTOM, Stats.DEATHS) > deathsBefore) {
            LoliExecutionManager.markDeathCommitted(player);
        }
    }

    private static void forcePlayerDeath(ServerPlayerEntity player, DamageSource source) {
        Text deathMessage = source.getDeathMessage(player);
        player.networkHandler.sendPacket(new DeathMessageS2CPacket(player.getId(), deathMessage));
        player.networkHandler.sendPacket(
                new HealthUpdateS2CPacket(
                        0.0F,
                        player.getHungerManager().getFoodLevel(),
                        player.getHungerManager().getSaturationLevel()
                )
        );

        player.getScoreboard().forEachScore(
                ScoreboardCriterion.DEATH_COUNT,
                player.getEntityName(),
                ScoreboardPlayerScore::incrementScore
        );
        player.incrementStat(Stats.DEATHS);

        if (source.getAttacker() instanceof ServerPlayerEntity killer && killer != player) {
            killer.updateKilledAdvancementCriterion(player, 0, source);
        }

        player.emitGameEvent(GameEvent.ENTITY_DIE);
        player.getWorld().sendEntityStatus(
                player,
                EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES
        );
        LiyMod.LOGGER.info(
                "Forced fallback death commit for {} after its normal death path was blocked",
                player.getGameProfile().getName()
        );
    }
}
