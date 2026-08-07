package com.liymod.network;

import com.liymod.safe.SafeTntEffectPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Common payload registration. Called once from the main mod initializer. */
public final class ModNetworking {
    private static boolean registered;

    private ModNetworking() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        PayloadTypeRegistry.clientboundPlay().register(SafeTntEffectPayload.TYPE, SafeTntEffectPayload.CODEC);
        registered = true;
    }
}
