package com.liymod.compat;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;

public final class RegistryFriendlyByteBuf extends FriendlyByteBuf {
    private final RegistryAccess registries;
    public RegistryFriendlyByteBuf(ByteBuf buffer, RegistryAccess registries) {
        super(buffer); this.registries=registries;
    }
    public RegistryAccess registryAccess() { return registries; }
}
