package com.liymod.storage;

import com.liymod.nbt.LoliCustomData;
import com.liymod.nbt.LoliCustomDataView;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.CustomData;

/** Keeps large pickaxe storage within vanilla's network NBT quota without changing saved data. */
public final class LoliStorageNetworkCodec {
    private static final String ROOT_KEY = "LoliStorage";
    private static final String VERSION_KEY = "NetworkVersion";
    private static final String PACKED_KEY = "Compressed";
    private static final int COMPRESS_THRESHOLD = 1024 * 1024;
    private static final int MAX_STORAGE_BYTES = 4 * 1024 * 1024;

    private LoliStorageNetworkCodec() {}

    public static DataComponentType.Builder<CustomData> configure(DataComponentType.Builder<CustomData> builder) {
        return builder.networkSynchronized(wrap(builder.build().streamCodec()));
    }

    public static <B> StreamCodec<B, CustomData> wrap(StreamCodec<B, CustomData> vanilla) {
        return vanilla.map(LoliStorageNetworkCodec::expand, LoliStorageNetworkCodec::compact);
    }

    static CustomData compact(CustomData data) {
        LoliCustomDataView cache = (Object) data instanceof LoliCustomDataView access ? access : null;
        if (cache != null && cache.liymod$getNetworkSnapshot() != null) return cache.liymod$getNetworkSnapshot();
        CompoundTag source = LoliCustomData.view(data);
        CompoundTag storage = source.getCompound(ROOT_KEY);
        if (storage.sizeInBytes() < COMPRESS_THRESHOLD) {
            if (cache != null) cache.liymod$setNetworkSnapshot(data);
            return data;
        }
        if (storage.sizeInBytes() > MAX_STORAGE_BYTES) {
            throw new EncoderException("Loli storage exceeds its 4 MiB budget");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(storage, output);
            CompoundTag packed = new CompoundTag();
            packed.putInt(VERSION_KEY, 1);
            packed.putByteArray(PACKED_KEY, output.toByteArray());
            CompoundTag root = LoliCustomData.copyRoot(source);
            root.put(ROOT_KEY, packed);
            CustomData result = CustomData.of(root);
            if (cache != null) cache.liymod$setNetworkSnapshot(result);
            return result;
        } catch (IOException exception) {
            throw new EncoderException("Cannot encode Loli storage", exception);
        }
    }

    static CustomData expand(CustomData data) {
        CompoundTag source = LoliCustomData.view(data);
        CompoundTag packed = source.getCompound(ROOT_KEY);
        if (com.liymod.compat.LegacyNbt.getIntOr(packed, VERSION_KEY, 0) != 1 || !packed.contains(PACKED_KEY)) return data;
        if (!packed.contains(PACKED_KEY, net.minecraft.nbt.Tag.TAG_BYTE_ARRAY))
            throw new DecoderException("Invalid compressed Loli storage");
        byte[] bytes = packed.getByteArray(PACKED_KEY);
        try {
            CompoundTag storage = NbtIo.readCompressed(
                    new ByteArrayInputStream(bytes), NbtAccounter.create(MAX_STORAGE_BYTES));
            CompoundTag root = LoliCustomData.copyRoot(source);
            root.put(ROOT_KEY, storage);
            return CustomData.of(root);
        } catch (IOException | RuntimeException exception) {
            throw new DecoderException("Cannot decode bounded Loli storage", exception);
        }
    }
}
