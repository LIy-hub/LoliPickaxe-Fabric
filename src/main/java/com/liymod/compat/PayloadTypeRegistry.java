package com.liymod.compat;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class PayloadTypeRegistry {
    private static final PayloadTypeRegistry S2C=new PayloadTypeRegistry();
    private static final PayloadTypeRegistry C2S=new PayloadTypeRegistry();
    private final Map<ResourceLocation,StreamCodec<RegistryFriendlyByteBuf,?>> codecs=new HashMap<>();
    private PayloadTypeRegistry() { }
    public static PayloadTypeRegistry playS2C() { return S2C; }
    public static PayloadTypeRegistry playC2S() { return C2S; }
    public <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf,T> codec) {
        if (codecs.putIfAbsent(type.id(),codec)!=null) throw new IllegalStateException("Duplicate payload: "+type.id());
    }
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf,T> codec(CustomPacketPayload.Type<T> type) {
        StreamCodec<RegistryFriendlyByteBuf,T> codec=(StreamCodec<RegistryFriendlyByteBuf,T>) codecs.get(type.id());
        if (codec==null) throw new IllegalStateException("Unregistered payload: "+type.id());
        return codec;
    }
}
