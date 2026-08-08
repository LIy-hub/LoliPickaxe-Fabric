package com.liymod.safe;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class SafeEffectService {
    private static final Map<SafeEffect, Boolean> ENABLED = new EnumMap<>(SafeEffect.class);
    static { for (SafeEffect effect : SafeEffect.values()) ENABLED.put(effect, false); }
    private SafeEffectService() { }
    public static boolean enabled(SafeEffect effect) { return ENABLED.get(effect); }
    public static void setEnabled(SafeEffect effect, boolean enabled) { ENABLED.put(effect, enabled); }
    public static boolean apply(ServerPlayer player, SafeEffect effect) {
        if (!enabled(effect)) return false;
        if (effect == SafeEffect.EXIT) player.connection.disconnect(Component.literal("LoliPickaxe safe EXIT simulation"));
        else if (effect == SafeEffect.BLUE_SCREEN) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
            player.sendSystemMessage(Component.literal("LoliPickaxe safe blue-screen simulation"));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 3));
        }
        return true;
    }
}
