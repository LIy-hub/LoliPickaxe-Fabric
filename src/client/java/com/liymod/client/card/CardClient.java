package com.liymod.client.card;

import com.liymod.item.LoliCardCatalog;
import com.liymod.network.LoliCardOpenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** Clientbound card dispatcher. Payload values are revalidated before a screen is created. */
public final class CardClient {
    private static boolean registered;

    private CardClient() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        ClientPlayNetworking.registerGlobalReceiver(
                LoliCardOpenPayload.TYPE,
                (payload, context) -> context.client().execute(() -> open(context.client(), payload))
        );
        registered = true;
    }

    private static void open(Minecraft client, LoliCardOpenPayload payload) {
        switch (payload.mode()) {
            case CARD -> client.gui.setScreen(CardViewerScreen.bundled(
                    false,
                    LoliCardCatalog.byId(payload.value()).stream().toList()));
            case ALBUM -> client.gui.setScreen(CardViewerScreen.bundled(
                    true,
                    LoliCardCatalog.group(payload.value())));
            case ONLINE_VIEW -> client.gui.setScreen(CardViewerScreen.online(payload.value()));
            case ONLINE_CONFIG -> client.gui.setScreen(CardOnlineConfigScreen.fromPayload(payload.value()));
        }
    }
}
