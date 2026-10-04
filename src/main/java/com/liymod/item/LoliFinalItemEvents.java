package com.liymod.item;

import com.liymod.LiyMod;
import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliItemSettings;
import com.liymod.config.LoliServerConfig;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Protects only dropped final-pickaxe item entities; administrators may still discard them. */
public final class LoliFinalItemEvents {
    private static int tick;

    private LoliFinalItemEvents() {
    }

    public static void registerEvents() {
        LiyMod.LOGGER.info("Registering final Loli Pickaxe owner/drop events for {}", LiyMod.MOD_ID);
        ServerTickEvents.END_SERVER_TICK.register(LoliFinalItemEvents::serverTick);
    }

    private static void serverTick(MinecraftServer server) {
        if (++tick % 5 != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity) {
                    protectDroppedPickaxe(server, itemEntity);
                }
            }
        }
    }

    private static void protectDroppedPickaxe(MinecraftServer server, ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (!LoliItemSettings.isFinalPickaxe(stack)
                || !LoliItemSettings.getBoolean(stack, LoliConfigOption.OWNER_PROTECTION)) {
            return;
        }
        UUID ownerId = LoliItemSettings.ownerUuid(stack).orElse(null);
        if (ownerId == null) {
            return;
        }
        entity.setInvulnerable(true);
        entity.setUnlimitedLifetime();
        entity.setTarget(ownerId);

        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner == null || owner.level() != entity.level()) {
            return;
        }
        int range = LoliServerConfig.getInt(LoliConfigOption.FIND_OWNER_RANGE);
        double distanceSquared = entity.distanceToSqr(owner);
        if (distanceSquared > (double) range * range) {
            return;
        }
        if (entity.getAge() >= LoliServerConfig.getInt(LoliConfigOption.DROP_PROTECT_TICKS)) {
            entity.setNoPickUpDelay();
        }
        if (distanceSquared <= 2.25D) {
            entity.playerTouch(owner);
            return;
        }
        Vec3 pull = owner.getEyePosition().subtract(entity.position());
        if (pull.lengthSqr() > 1.0E-6D) {
            entity.setDeltaMovement(pull.normalize().scale(0.45D));
        }
    }
}
