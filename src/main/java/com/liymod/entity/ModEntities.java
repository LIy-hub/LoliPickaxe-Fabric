package com.liymod.entity;

import com.liymod.LiyMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ModEntities {
    public static final Identifier LOLI_ID = Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli");
    public static final Identifier LOLI_PRIMED_TNT_ID =
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_buff_attack_tnt");

    public static final EntityType<LoliEntity> LOLI = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            LOLI_ID,
            EntityType.Builder.of(LoliEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.5F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, LOLI_ID))
    );

    public static final EntityType<LoliPrimedTntEntity> LOLI_PRIMED_TNT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            LOLI_PRIMED_TNT_ID,
            EntityType.Builder.<LoliPrimedTntEntity>of(LoliPrimedTntEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(10)
                    .updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, LOLI_PRIMED_TNT_ID))
    );

    private ModEntities() {
    }

    public static void registerEntities() {
        FabricDefaultAttributeRegistry.register(LOLI, Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D));
        LiyMod.LOGGER.info("Registering entities for {}", LiyMod.MOD_ID);
    }
}
