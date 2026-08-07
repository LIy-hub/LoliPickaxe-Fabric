package com.liymod.network;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StoragePagePayload(int delta) implements CustomPacketPayload {
    public static final Type<StoragePagePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "storage_page")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StoragePagePayload> CODEC =
            CustomPacketPayload.codec(StoragePagePayload::write, StoragePagePayload::read);

    public StoragePagePayload {
        delta = Integer.signum(delta);
    }

    private static StoragePagePayload read(RegistryFriendlyByteBuf buffer) {
        return new StoragePagePayload(buffer.readByte());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeByte(delta);
    }

    @Override
    public Type<StoragePagePayload> type() {
        return TYPE;
    }
}
