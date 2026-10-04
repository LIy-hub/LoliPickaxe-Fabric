package com.liymod.item;

import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliServerConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;

/** Independent legacy living-drop chances, including creeper-only music discs. */
public final class LoliAuxiliaryDropEvents {
    private LoliAuxiliaryDropEvents() {
    }

    public static void registerEvents() {
        ServerLivingEntityEvents.AFTER_DEATH.register(LoliAuxiliaryDropEvents::afterDeath);
    }

    private static void afterDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (roll(level, LoliConfigOption.LOLI_CARD_DROP_CHANCE)) {
            LoliCardItem card = (LoliCardItem) ModItems.LOLI_CARD;
            String art = LoliCardCatalog.randomStandalone(level.getRandom()).id();
            entity.spawnAtLocation(level, card.createArtStack(art));
        }
        if (roll(level, LoliConfigOption.LOLI_CARD_ALBUM_DROP_CHANCE)) {
            LoliCardItem album = (LoliCardItem) ModItems.LOLI_CARD_ALBUM;
            entity.spawnAtLocation(level, album.createAlbumStack(
                    LoliCardCatalog.randomGroup(level.getRandom())
            ));
        }
        if (entity instanceof Creeper && roll(level, LoliConfigOption.LOLI_RECORD_DROP_CHANCE)) {
            entity.spawnAtLocation(level, new ItemStack(ModItems.LOLI_RECORD));
        }
        if (roll(level, LoliConfigOption.ENTITY_SOUL_DROP_CHANCE)) {
            entity.spawnAtLocation(level, ModItems.LOLI_ENTITY_SOUL_ADDON.getDefaultInstance());
        }
    }

    private static boolean roll(ServerLevel level, LoliConfigOption option) {
        return level.getRandom().nextDouble() < LoliServerConfig.getDouble(option);
    }
}
