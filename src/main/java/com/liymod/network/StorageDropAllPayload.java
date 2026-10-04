package com.liymod.network;

import com.liymod.LiyMod;
import com.liymod.compat.RegistryFriendlyByteBuf;
import com.liymod.compat.StreamCodec;
import com.liymod.compat.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StorageDropAllPayload() implements CustomPacketPayload {
    public static final Type<StorageDropAllPayload> TYPE = new Type<>(
            new ResourceLocation(LiyMod.MOD_ID, "storage_drop_all")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageDropAllPayload> CODEC =
            StreamCodec.unit(new StorageDropAllPayload());

    @Override
    public Type<StorageDropAllPayload> type() {
        return TYPE;
    }
}
