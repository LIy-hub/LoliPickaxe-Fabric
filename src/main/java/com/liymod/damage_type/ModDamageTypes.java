package com.liymod.damage_type;

import com.liymod.LiyMod;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import static com.liymod.LiyMod.MOD_ID;
import static net.minecraft.registry.RegistryKeys.DAMAGE_TYPE;

public final class ModDamageTypes {
    public static final RegistryKey<DamageType> LOLI_DAMAGE = RegistryKey.of(
            DAMAGE_TYPE,
            Identifier.of(MOD_ID, "loli_damage")
    );

    private ModDamageTypes() {
    }

    public static void registerDamageTypes() {
        LiyMod.LOGGER.info("Loading damage types for {}", MOD_ID);
    }
}
