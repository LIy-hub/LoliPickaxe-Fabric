package com.liymod.safe;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Fixed-size S2C presentation cue. Durations are defined by {@link SafeTntEffect}. */
public record SafeTntEffectPayload(SafeTntEffect effect) implements CustomPacketPayload {
    public static final Type<SafeTntEffectPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "safe_tnt_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SafeTntEffectPayload> CODEC =
            CustomPacketPayload.codec(SafeTntEffectPayload::write, SafeTntEffectPayload::read);

    public SafeTntEffectPayload {
        if (effect == null) {
            throw new IllegalArgumentException("Safe TNT effect cannot be null");
        }
    }

    private static SafeTntEffectPayload read(RegistryFriendlyByteBuf buffer) {
        int networkId = buffer.readUnsignedByte();
        SafeTntEffect effect = SafeTntEffect.fromNetworkId(networkId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown safe TNT effect id: " + networkId));
        return new SafeTntEffectPayload(effect);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeByte(effect.networkId());
    }

    @Override
    public Type<SafeTntEffectPayload> type() {
        return TYPE;
    }
}
