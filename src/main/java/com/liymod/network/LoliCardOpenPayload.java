package com.liymod.network;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authoritative request to show bundled card art or the bounded online-card flow. */
public record LoliCardOpenPayload(Mode mode, String value) implements CustomPacketPayload {
    public static final int MAX_VALUE_LENGTH = 520;

    public enum Mode {
        CARD,
        ALBUM,
        ONLINE_VIEW,
        ONLINE_CONFIG
    }

    public static final Type<LoliCardOpenPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_card_open")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LoliCardOpenPayload> CODEC =
            CustomPacketPayload.codec(LoliCardOpenPayload::write, LoliCardOpenPayload::read);

    public LoliCardOpenPayload {
        mode = mode == null ? Mode.CARD : mode;
        value = limit(value);
    }

    private static LoliCardOpenPayload read(RegistryFriendlyByteBuf buffer) {
        int ordinal = Math.clamp(buffer.readUnsignedByte(), 0, Mode.values().length - 1);
        return new LoliCardOpenPayload(Mode.values()[ordinal], buffer.readUtf(MAX_VALUE_LENGTH));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeByte(mode.ordinal());
        buffer.writeUtf(value, MAX_VALUE_LENGTH);
    }

    private static String limit(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_VALUE_LENGTH ? value : value.substring(0, MAX_VALUE_LENGTH);
    }

    @Override
    public Type<LoliCardOpenPayload> type() {
        return TYPE;
    }
}
