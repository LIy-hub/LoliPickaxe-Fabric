package com.liymod.network;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StorageOpenPayload(Mode mode) implements CustomPacketPayload {
    public enum Mode {
        STORAGE,
        BLACKLIST
    }

    public static final Type<StorageOpenPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "storage_open")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageOpenPayload> CODEC =
            CustomPacketPayload.codec(StorageOpenPayload::write, StorageOpenPayload::read);

    public StorageOpenPayload {
        mode = mode == null ? Mode.STORAGE : mode;
    }

    private static StorageOpenPayload read(RegistryFriendlyByteBuf buffer) {
        int id = buffer.readUnsignedByte();
        return new StorageOpenPayload(id == 1 ? Mode.BLACKLIST : Mode.STORAGE);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeByte(mode == Mode.BLACKLIST ? 1 : 0);
    }

    @Override
    public Type<StorageOpenPayload> type() {
        return TYPE;
    }
}
