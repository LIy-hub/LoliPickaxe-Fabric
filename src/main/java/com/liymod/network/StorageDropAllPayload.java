package com.liymod.network;

import com.liymod.LiyMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StorageDropAllPayload() implements CustomPacketPayload {
    public static final Type<StorageDropAllPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(LiyMod.MOD_ID, "storage_drop_all")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageDropAllPayload> CODEC =
            StreamCodec.unit(new StorageDropAllPayload());

    @Override
    public Type<StorageDropAllPayload> type() {
        return TYPE;
    }
}
