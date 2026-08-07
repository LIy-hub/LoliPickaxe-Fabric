package com.liymod.network;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Requests a bounded server-relative teleport, optionally into another allowed dimension. */
public record LoliTeleportPayload(
        String dimensionId,
        double offsetX,
        double offsetY,
        double offsetZ
) implements CustomPacketPayload {
    public static final int MAX_ID_LENGTH = 128;
    public static final Type<LoliTeleportPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_teleport")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, LoliTeleportPayload> CODEC =
            CustomPacketPayload.codec(LoliTeleportPayload::write, LoliTeleportPayload::read);

    public LoliTeleportPayload {
        dimensionId = limit(dimensionId);
    }

    private static LoliTeleportPayload read(RegistryFriendlyByteBuf buffer) {
        return new LoliTeleportPayload(
                buffer.readUtf(MAX_ID_LENGTH),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(dimensionId, MAX_ID_LENGTH);
        buffer.writeDouble(offsetX);
        buffer.writeDouble(offsetY);
        buffer.writeDouble(offsetZ);
    }

    private static String limit(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_ID_LENGTH ? value : value.substring(0, MAX_ID_LENGTH);
    }

    @Override
    public Type<LoliTeleportPayload> type() {
        return TYPE;
    }
}
