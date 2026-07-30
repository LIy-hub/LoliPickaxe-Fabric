package com.liymod.combat;

import com.liymod.LiyMod;
import com.liymod.protection.LoliProtection;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LoliAttackResolver {
    private static final double MAX_RANGE = 1024.0D;
    private static final double TARGET_BOX_EXPANSION = 0.35D;
    private static final double MIN_AIM_DOT = Math.cos(Math.toRadians(6.0D));

    private LoliAttackResolver() {
    }

    public static boolean executeFromLook(ServerPlayer attacker) {
        if (!LoliProtection.isProtected(attacker)
                || LoliExecutionManager.isDeadLocked(attacker)) {
            return false;
        }

        ServerPlayer target = findTarget(attacker);
        if (target == null) {
            return false;
        }

        LoliErasureService.Result result = LoliErasureService.executeAbsolute(
                attacker,
                target
        );
        if (result == LoliErasureService.Result.EXECUTED) {
            LiyMod.LOGGER.debug(
                    "Absolute Loli swing from {} resolved to {}",
                    attacker.getGameProfile().name(),
                    target.getGameProfile().name()
            );
            return true;
        }
        return false;
    }

    @Nullable
    private static ServerPlayer findTarget(ServerPlayer attacker) {
        Vec3 origin = attacker.getEyePosition();
        Vec3 look = attacker.getViewVector(1.0F).normalize();
        Vec3 rayEnd = origin.add(look.scale(MAX_RANGE));
        double maximumDistanceSquared = MAX_RANGE * MAX_RANGE;

        ServerPlayer directTarget = null;
        double directDistanceSquared = Double.POSITIVE_INFINITY;
        ServerPlayer assistedTarget = null;
        double assistedDot = MIN_AIM_DOT;
        double assistedDistanceSquared = Double.POSITIVE_INFINITY;

        for (ServerPlayer candidate : attacker.level().players()) {
            if (candidate == attacker
                    || candidate.isRemoved()
                    || LoliExecutionManager.isDeadLocked(candidate)) {
                continue;
            }

            AABB targetBox = candidate.getBoundingBox().inflate(TARGET_BOX_EXPANSION);
            Optional<Vec3> intersection = targetBox.clip(origin, rayEnd);
            if (intersection.isPresent()) {
                double hitDistanceSquared = origin.distanceToSqr(intersection.get());
                if (hitDistanceSquared < directDistanceSquared) {
                    directTarget = candidate;
                    directDistanceSquared = hitDistanceSquared;
                }
                continue;
            }

            Vec3 delta = targetBox.getCenter().subtract(origin);
            double distanceSquared = delta.lengthSqr();
            if (distanceSquared <= 0.0D || distanceSquared > maximumDistanceSquared) {
                continue;
            }

            double dot = delta.dot(look) / Math.sqrt(distanceSquared);
            if (dot < MIN_AIM_DOT) {
                continue;
            }

            if (dot > assistedDot
                    || (Math.abs(dot - assistedDot) < 1.0E-9D
                    && distanceSquared < assistedDistanceSquared)) {
                assistedTarget = candidate;
                assistedDot = dot;
                assistedDistanceSquared = distanceSquared;
            }
        }

        return directTarget != null ? directTarget : assistedTarget;
    }
}
