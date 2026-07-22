package com.liymod.damage_type;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ModDamageSources {
    private ModDamageSources() {
    }

    public static DamageSource loli(World world) {
        return loli(world, null);
    }

    public static DamageSource loli(World world, @Nullable Entity attacker) {
        return new DamageSource(
                world.getRegistryManager()
                        .get(RegistryKeys.DAMAGE_TYPE)
                        .entryOf(ModDamageTypes.LOLI_DAMAGE),
                attacker
        );
    }
}
