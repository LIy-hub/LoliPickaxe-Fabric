package com.liymod.network;

import com.liymod.LiyMod;
import com.liymod.compat.RegistryFriendlyByteBuf;
import com.liymod.compat.StreamCodec;
import com.liymod.compat.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

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
            new ResourceLocation(LiyMod.MOD_ID, "loli_card_open")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LoliCardOpenPayload> CODEC =
            CustomPacketPayload.codec(LoliCardOpenPayload::write, LoliCardOpenPayload::read);

    public LoliCardOpenPayload {
        mode = mode == null ? Mode.CARD : mode;
        value = limit(value);
    }

    private static LoliCardOpenPayload read(RegistryFriendlyByteBuf buffer) {
        int ordinal = com.liymod.compat.LegacyMath.clamp(buffer.readUnsignedByte(), 0, Mode.values().length - 1);
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
