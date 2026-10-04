package com.liymod.damage_type;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ModDamageSources {
    private ModDamageSources() {
    }

    public static DamageSource loli(Level world) {
        return loli(world, null);
    }

    public static DamageSource loli(Level world, @Nullable Entity attacker) {
        return new DamageSource(
                world.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ModDamageTypes.LOLI_DAMAGE),
                attacker
        );
    }
}
