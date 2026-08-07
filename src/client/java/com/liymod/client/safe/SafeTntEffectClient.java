package com.liymod.client.safe;

import com.liymod.safe.SafeTntEffect;
import com.liymod.safe.SafeTntEffectPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Client-only receiver for safe, bounded TNT presentation effects. */
public final class SafeTntEffectClient {
    private static boolean registered;

    private SafeTntEffectClient() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        ClientPlayNetworking.registerGlobalReceiver(
                SafeTntEffectPayload.TYPE,
                (payload, context) -> context.client().execute(() -> show(context.client(), payload.effect())));
        registered = true;
    }

    private static void show(Minecraft client, SafeTntEffect effect) {
        if (effect == SafeTntEffect.EXIT) {
            return;
        }

        Screen previous = client.gui.screen();
        if (previous instanceof TimedSafeTntEffectScreen activeEffect) {
            previous = activeEffect.previousScreen();
        }

        Screen effectScreen = switch (effect) {
            case BLUE_SCREEN -> new BlueScreenEffectScreen(previous, effect.durationTicks());
            case FAIL_RESPOND -> new FailRespondEffectScreen(previous, effect.durationTicks());
            case EXIT -> throw new IllegalStateException("EXIT is handled as a server disconnect");
        };
        client.gui.setScreen(effectScreen);
    }
}
