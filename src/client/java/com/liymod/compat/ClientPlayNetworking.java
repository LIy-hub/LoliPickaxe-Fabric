package com.liymod.compat;

import io.netty.buffer.Unpooled;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;

public final class ClientPlayNetworking {
    public record Context(Minecraft client) { }
    private ClientPlayNetworking() { }
    public static <T extends CustomPacketPayload> void registerGlobalReceiver(
            CustomPacketPayload.Type<T> type,BiConsumer<T,Context> receiver) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(type.id(),
                (client,handler,buffer,sender)-> {
                    T payload=PayloadTypeRegistry.playS2C().codec(type).decode(new RegistryFriendlyByteBuf(buffer,client.level.registryAccess()));
                    receiver.accept(payload,new Context(client));
                });
    }
    public static <T extends CustomPacketPayload> void send(T payload) {
        Minecraft client=Minecraft.getInstance();
        if (client.level==null) return;
        RegistryFriendlyByteBuf buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),client.level.registryAccess());
        encode(buffer,payload);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload.type().id(),buffer);
    }
    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> void encode(RegistryFriendlyByteBuf buffer,T payload) {
        PayloadTypeRegistry.playC2S().codec((CustomPacketPayload.Type<T>) payload.type()).encode(buffer,payload);
    }
}
