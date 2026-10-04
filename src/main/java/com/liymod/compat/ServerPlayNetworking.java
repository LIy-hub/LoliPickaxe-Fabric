package com.liymod.compat;

import io.netty.buffer.Unpooled;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;

public final class ServerPlayNetworking {
    public record Context(ServerPlayer player) { }
    private ServerPlayNetworking() { }
    public static <T extends CustomPacketPayload> void registerGlobalReceiver(
            CustomPacketPayload.Type<T> type,BiConsumer<T,Context> receiver) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(type.id(),
                (server,player,handler,buffer,sender)-> {
                    T payload=PayloadTypeRegistry.playC2S().codec(type).decode(new RegistryFriendlyByteBuf(buffer,player.level().registryAccess()));
                    receiver.accept(payload,new Context(player));
                });
    }
    public static <T extends CustomPacketPayload> void send(ServerPlayer player,T payload) {
        RegistryFriendlyByteBuf buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),player.level().registryAccess());
        encode(buffer,payload);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,payload.type().id(),buffer);
    }
    public static boolean canSend(ServerPlayer player,CustomPacketPayload.Type<?> type) {
        return net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,type.id());
    }
    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> void encode(RegistryFriendlyByteBuf buffer,T payload) {
        PayloadTypeRegistry.playS2C().codec((CustomPacketPayload.Type<T>) payload.type()).encode(buffer,payload);
    }
}
