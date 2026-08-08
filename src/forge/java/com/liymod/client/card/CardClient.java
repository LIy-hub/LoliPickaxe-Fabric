package com.liymod.client.card;

import com.liymod.item.LoliCardCatalog;
import com.liymod.network.ModNetwork;
import net.minecraft.client.Minecraft;

public final class CardClient {
    private CardClient() { }
    public static void open(ModNetwork.CardOpenPacket packet) {
        Minecraft client = Minecraft.getInstance();
        switch (packet.mode()) {
            case CARD -> client.setScreen(CardViewerScreen.bundled(LoliCardCatalog.byId(packet.value()).stream().toList(), false));
            case ALBUM -> client.setScreen(CardViewerScreen.bundled(LoliCardCatalog.ALBUM, true));
            case ONLINE_VIEW -> client.setScreen(CardViewerScreen.online(packet.value()));
            case ONLINE_CONFIG -> client.setScreen(CardOnlineConfigScreen.from(packet.value()));
        }
    }
}
