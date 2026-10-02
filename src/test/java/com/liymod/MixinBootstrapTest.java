package com.liymod;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** Loads every common-side mixin target through Fabric's real transforming loader. */
public final class MixinBootstrapTest {
    @Test
    void minecraftRegistriesAndMixinTargetsLoad() {
        assertDoesNotThrow(() -> {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            for (String target : new String[] {
                    "net.minecraft.world.entity.Entity",
                    "net.minecraft.world.entity.LivingEntity",
                    "net.minecraft.world.entity.item.PrimedTnt",
                    "net.minecraft.world.entity.player.Player",
                    "net.minecraft.server.players.PlayerList",
                    "net.minecraft.server.level.ServerPlayer",
                    "net.minecraft.server.level.ServerPlayerGameMode",
                    "net.minecraft.server.network.ServerGamePacketListenerImpl",
                    "net.minecraft.world.entity.projectile.ProjectileUtil",
                    "net.minecraft.world.entity.ai.targeting.TargetingConditions"}) {
                Class.forName(target);
            }
        });
    }
}
