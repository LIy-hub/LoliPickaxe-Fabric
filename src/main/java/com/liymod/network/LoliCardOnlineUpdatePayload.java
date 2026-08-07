package com.liymod.network;

import com.liymod.LiyMod;
import com.liymod.item.LoliCardData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/** Bounded URL update for the exact online-card stack still held by the player. */
public record LoliCardOnlineUpdatePayload(InteractionHand hand, String url)
        implements CustomPacketPayload {
    public static final Type<LoliCardOnlineUpdatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_card_online_update")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LoliCardOnlineUpdatePayload> CODEC =
            CustomPacketPayload.codec(LoliCardOnlineUpdatePayload::write, LoliCardOnlineUpdatePayload::read);

    public LoliCardOnlineUpdatePayload {
        hand = hand == null ? InteractionHand.MAIN_HAND : hand;
        url = limit(url);
    }

    private static LoliCardOnlineUpdatePayload read(RegistryFriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return new LoliCardOnlineUpdatePayload(hand, buffer.readUtf(LoliCardData.MAX_URL_LENGTH));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(hand == InteractionHand.OFF_HAND);
        buffer.writeUtf(url, LoliCardData.MAX_URL_LENGTH);
    }

    private static String limit(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= LoliCardData.MAX_URL_LENGTH
                ? value
                : value.substring(0, LoliCardData.MAX_URL_LENGTH);
    }

    @Override
    public Type<LoliCardOnlineUpdatePayload> type() {
        return TYPE;
    }
}
