package com.liymod.compat;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

public interface CustomPacketPayload {
    record Type<T extends CustomPacketPayload>(ResourceLocation id) { }
    Type<? extends CustomPacketPayload> type();
    static <B,T extends CustomPacketPayload> StreamCodec<B,T> codec(
            BiConsumer<T,B> writer, Function<B,T> reader) {
        return StreamCodec.of((buffer,value)->writer.accept(value,buffer),reader);
    }
}
