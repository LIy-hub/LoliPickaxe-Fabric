package com.liymod.network;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Orders the client-side page switch ahead of the vanilla full-slot update.
 *
 * <p>The vanilla container synchronizer sends slot contents before {@code DataSlot} values. A
 * paged container therefore needs this small cue first so the following 81 slots are written into
 * the requested page instead of the previously visible page.</p>
 */
public record StoragePageSyncPayload(int page, int pageCount) implements CustomPacketPayload {
    public static final Type<StoragePageSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "storage_page_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StoragePageSyncPayload> CODEC =
            CustomPacketPayload.codec(StoragePageSyncPayload::write, StoragePageSyncPayload::read);

    public StoragePageSyncPayload {
        pageCount = Math.max(1, pageCount);
        page = Math.clamp(page, 0, pageCount - 1);
    }

    private static StoragePageSyncPayload read(RegistryFriendlyByteBuf buffer) {
        return new StoragePageSyncPayload(buffer.readVarInt(), buffer.readVarInt());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(page);
        buffer.writeVarInt(pageCount);
    }

    @Override
    public Type<StoragePageSyncPayload> type() {
        return TYPE;
    }
}
