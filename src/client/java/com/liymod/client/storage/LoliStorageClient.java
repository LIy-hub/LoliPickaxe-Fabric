package com.liymod.client.storage;

import com.liymod.menu.StorageMenu;
import com.liymod.network.StoragePageSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Receives the page cue before the vanilla container-content packet on the same connection. */
public final class LoliStorageClient {
    private static boolean registered;

    private LoliStorageClient() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        ClientPlayNetworking.registerGlobalReceiver(
                StoragePageSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().player != null
                            && context.client().player.containerMenu instanceof StorageMenu menu) {
                        menu.applyPageSync(payload.page(), payload.pageCount());
                    }
                })
        );
        registered = true;
    }
}
