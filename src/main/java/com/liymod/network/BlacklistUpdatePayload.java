package com.liymod.network;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BlacklistUpdatePayload(int slot, boolean clear) implements CustomPacketPayload {
    public static final Type<BlacklistUpdatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "blacklist_update")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BlacklistUpdatePayload> CODEC =
            CustomPacketPayload.codec(BlacklistUpdatePayload::write, BlacklistUpdatePayload::read);

    private static BlacklistUpdatePayload read(RegistryFriendlyByteBuf buffer) {
        return new BlacklistUpdatePayload(buffer.readVarInt(), buffer.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(slot);
        buffer.writeBoolean(clear);
    }

    @Override
    public Type<BlacklistUpdatePayload> type() {
        return TYPE;
    }
}
