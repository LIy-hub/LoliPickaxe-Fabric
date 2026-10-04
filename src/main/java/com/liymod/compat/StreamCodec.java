package com.liymod.compat;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Bounded payload codecs transported by the legacy Fabric packet API. */
public interface StreamCodec<B, V> {
    void encode(B buffer, V value);
    V decode(B buffer);
    static <B,V> StreamCodec<B,V> of(BiConsumer<B,V> writer, Function<B,V> reader) {
        return new StreamCodec<>() {
            public void encode(B buffer,V value) { writer.accept(buffer,value); }
            public V decode(B buffer) { return reader.apply(buffer); }
        };
    }
    static <B,V> StreamCodec<B,V> unit(V value) {
        return of((buffer,input)-> {
            if (!Objects.equals(value,input)) throw new IllegalArgumentException("Unexpected unit value");
        },buffer->value);
    }
    @SuppressWarnings("unchecked")
    default <C extends B> StreamCodec<C,V> cast() { return (StreamCodec<C,V>) this; }
}
