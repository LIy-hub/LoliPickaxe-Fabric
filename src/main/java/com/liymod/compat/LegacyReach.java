package com.liymod.compat;

import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliItemSettings;
import net.minecraft.world.entity.player.Player;

/** Stack-aware reach for versions before vanilla interaction-range attributes. */
public final class LegacyReach {
    private LegacyReach() { }
    public static double configured(Player player) {
        if(player==null || !LoliItemSettings.isFinalPickaxe(player.getMainHandItem())) return 0.0D;
        return LoliItemSettings.getDouble(player.getMainHandItem(),LoliConfigOption.BLOCK_REACH_DISTANCE);
    }
    public static double squaredLimit(Player player,double vanilla) {
        double reach=configured(player);
        return reach>0.0D ? (reach+1.0D)*(reach+1.0D):vanilla;
    }
}
