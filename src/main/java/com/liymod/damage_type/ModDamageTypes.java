package com.liymod.damage_type;

import com.liymod.LiyMod;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import static com.liymod.LiyMod.MOD_ID;
import static net.minecraft.core.registries.Registries.DAMAGE_TYPE;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> LOLI_DAMAGE = ResourceKey.create(
            DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "loli_damage")
    );

    private ModDamageTypes() {
    }

    public static void registerDamageTypes() {
        LiyMod.LOGGER.info("Loading damage types for {}", MOD_ID);
    }
}
