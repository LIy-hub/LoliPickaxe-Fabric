package com.liymod;

import com.liymod.registry.ModContent;
import com.liymod.recipe.ModRecipes;
import com.liymod.menu.ModMenus;
import com.liymod.network.ModNetwork;
import com.liymod.event.ForgeEvents;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;
import com.liymod.config.LoliServerConfig;

@Mod(LiyMod.MOD_ID)
public final class LiyMod {
    public static final String MOD_ID = "liymod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LiyMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModContent.register(modBus);
        ModRecipes.register(modBus);
        ModMenus.register(modBus);
        ModNetwork.register();
        LoliServerConfig.load();
        modBus.addListener(this::attributes);
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> com.liymod.client.ClientBootstrap::init);
        LOGGER.info("Loading LoliPickaxe Forge 1.20.1 port");
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(ModContent.LOLI_ENTITY.get(), Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .build());
    }
}
