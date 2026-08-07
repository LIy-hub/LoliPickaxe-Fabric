package com.liymod.item;

import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliServerConfig;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Validates bounded relative teleports without loading destination chunks. */
public final class LoliTeleportService {
    private LoliTeleportService() {
    }

    public static boolean teleportRelative(
            ServerPlayer player,
            String encodedDimension,
            double requestedX,
            double requestedY,
            double requestedZ
    ) {
        if (!LoliServerConfig.getBoolean(LoliConfigOption.SPACE_FOLDING)
                || encodedDimension == null
                || encodedDimension.length() > 128
                || !finite(requestedX, requestedY, requestedZ)) {
            return false;
        }
        Identifier dimensionId = Identifier.tryParse(encodedDimension);
        if (dimensionId == null || isBlacklisted(dimensionId)) {
            return false;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel destination = player.level().getServer().getLevel(dimension);
        if (destination == null) {
            return false;
        }

        Vec3 offset = new Vec3(requestedX, requestedY, requestedZ);
        double maximum = LoliServerConfig.getDouble(LoliConfigOption.MAX_TELEPORT_DISTANCE);
        if (offset.lengthSqr() > maximum * maximum) {
            offset = offset.normalize().scale(maximum);
        }
        Vec3 target = player.position().add(offset);
        if (!finite(target.x, target.y, target.z)
                || target.y < destination.getMinY() + 1.0D
                || target.y > destination.getMaxY() - 2.0D
                || !destination.getWorldBorder().isWithinBounds(target)) {
            return false;
        }

        BlockPos feet = BlockPos.containing(target);
        if (!destination.hasChunkAt(feet)
                || !destination.getFluidState(feet).isEmpty()
                || destination.getBlockState(feet.below())
                .getCollisionShape(destination, feet.below())
                .isEmpty()) {
            return false;
        }
        AABB landingBox = player.getBoundingBox().move(target.subtract(player.position()));
        if (!destination.noCollision(player, landingBox)) {
            return false;
        }

        player.stopRiding();
        boolean teleported = player.teleportTo(
                destination,
                target.x,
                target.y,
                target.z,
                Set.<Relative>of(),
                player.getYRot(),
                player.getXRot(),
                false
        );
        if (teleported) {
            player.fallDistance = 0.0F;
        }
        return teleported;
    }

    private static boolean isBlacklisted(Identifier dimension) {
        String encoded = LoliServerConfig.getString(LoliConfigOption.DIMENSION_BLACKLIST);
        for (String element : encoded.split(",")) {
            if (dimension.toString().equals(element.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean finite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }
}
