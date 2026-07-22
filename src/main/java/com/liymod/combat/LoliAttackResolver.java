package com.liymod.combat;

import com.liymod.LiyMod;
import com.liymod.protection.LoliProtection;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class LoliAttackResolver {
    private static final double MAX_RANGE = 1024.0D;
    private static final double TARGET_BOX_EXPANSION = 0.35D;
    private static final double MIN_AIM_DOT = Math.cos(Math.toRadians(6.0D));

    private LoliAttackResolver() {
    }

    public static boolean executeFromLook(ServerPlayerEntity attacker) {
        if (!LoliProtection.isProtected(attacker)
                || LoliExecutionManager.isDeadLocked(attacker)) {
            return false;
        }

        ServerPlayerEntity target = findTarget(attacker);
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
                    attacker.getGameProfile().getName(),
                    target.getGameProfile().getName()
            );
            return true;
        }
        return false;
    }

    @Nullable
    private static ServerPlayerEntity findTarget(ServerPlayerEntity attacker) {
        Vec3d origin = attacker.getEyePos();
        Vec3d look = attacker.getRotationVec(1.0F).normalize();
        Vec3d rayEnd = origin.add(look.multiply(MAX_RANGE));
        double maximumDistanceSquared = MAX_RANGE * MAX_RANGE;

        ServerPlayerEntity directTarget = null;
        double directDistanceSquared = Double.POSITIVE_INFINITY;
        ServerPlayerEntity assistedTarget = null;
        double assistedDot = MIN_AIM_DOT;
        double assistedDistanceSquared = Double.POSITIVE_INFINITY;

        for (ServerPlayerEntity candidate : attacker.getServerWorld().getPlayers()) {
            if (candidate == attacker
                    || candidate.isRemoved()
                    || LoliExecutionManager.isDeadLocked(candidate)) {
                continue;
            }

            Box targetBox = candidate.getBoundingBox().expand(TARGET_BOX_EXPANSION);
            Optional<Vec3d> intersection = targetBox.raycast(origin, rayEnd);
            if (intersection.isPresent()) {
                double hitDistanceSquared = origin.squaredDistanceTo(intersection.get());
                if (hitDistanceSquared < directDistanceSquared) {
                    directTarget = candidate;
                    directDistanceSquared = hitDistanceSquared;
                }
                continue;
            }

            Vec3d delta = targetBox.getCenter().subtract(origin);
            double distanceSquared = delta.lengthSquared();
            if (distanceSquared <= 0.0D || distanceSquared > maximumDistanceSquared) {
                continue;
            }

            double dot = delta.dotProduct(look) / Math.sqrt(distanceSquared);
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
