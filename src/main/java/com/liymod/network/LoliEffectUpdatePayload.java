package com.liymod.network;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LoliEffectUpdatePayload(String effectId, int level) implements CustomPacketPayload {
    public static final int MAX_ID_LENGTH = 128;
    public static final Type<LoliEffectUpdatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_effect_update")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LoliEffectUpdatePayload> CODEC =
            CustomPacketPayload.codec(LoliEffectUpdatePayload::write, LoliEffectUpdatePayload::read);

    public LoliEffectUpdatePayload {
        effectId = limit(effectId);
    }

    private static LoliEffectUpdatePayload read(RegistryFriendlyByteBuf buffer) {
        return new LoliEffectUpdatePayload(buffer.readUtf(MAX_ID_LENGTH), buffer.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(effectId, MAX_ID_LENGTH);
        buffer.writeVarInt(level);
    }

    private static String limit(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_ID_LENGTH ? value : value.substring(0, MAX_ID_LENGTH);
    }

    @Override
    public Type<LoliEffectUpdatePayload> type() {
        return TYPE;
    }
}
