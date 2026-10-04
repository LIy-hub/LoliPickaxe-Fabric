package com.liymod.storage;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

/** Network-only adaptation: old saves keep their Slot/Stack ListTag without packed markers. */
public final class LoliStorageNetworkCodec {
    private static final int THRESHOLD=1024*1024;
    private static final int MAX_BYTES=4*1024*1024;
    private static final int MAX_PACKED_BYTES=1024*1024;
    private static final String VERSION="NetworkVersion", PACKED="Compressed";
    private LoliStorageNetworkCodec() { }

    public static CompoundTag compact(CompoundTag source) {
        if(source==null || !source.contains(LoliStorageData.STORAGE_KEY,Tag.TAG_LIST)) return source;
        CompoundTag storage=new CompoundTag();storage.put("Value",source.get(LoliStorageData.STORAGE_KEY));
        if(storage.sizeInBytes()<THRESHOLD) return source;
        try {
            ByteArrayOutputStream plain=new ByteArrayOutputStream();
            NbtIo.write(storage,new DataOutputStream(plain));
            if(plain.size()>MAX_BYTES) throw new EncoderException("Loli storage exceeds its 4 MiB budget");

            ByteArrayOutputStream packedBytes=new ByteArrayOutputStream();
            try(GZIPOutputStream gzip=new GZIPOutputStream(packedBytes)) { plain.writeTo(gzip); }
            if(packedBytes.size()>MAX_PACKED_BYTES) throw new EncoderException("Compressed Loli storage exceeds network budget");
            CompoundTag packed=new CompoundTag();packed.putInt(VERSION,1);packed.putByteArray(PACKED,packedBytes.toByteArray());
            CompoundTag root=source.copy();root.put(LoliStorageData.STORAGE_KEY,packed);return root;
        } catch(IOException exception) { throw new EncoderException("Cannot encode Loli storage",exception); }
    }

    public static CompoundTag expand(CompoundTag source) {
        if(source==null || !source.contains(LoliStorageData.STORAGE_KEY,Tag.TAG_COMPOUND)) return source;
        CompoundTag packed=source.getCompound(LoliStorageData.STORAGE_KEY);
        if(!packed.contains(VERSION) && !packed.contains(PACKED)) return source;
        if(packed.getInt(VERSION)!=1 || !packed.contains(PACKED,Tag.TAG_BYTE_ARRAY)) throw new DecoderException("Invalid compressed Loli storage");
        byte[] bytes=packed.getByteArray(PACKED);
        if(bytes.length==0 || bytes.length>MAX_PACKED_BYTES) throw new DecoderException("Invalid compressed Loli storage length");
        try(var gzip=new GZIPInputStream(new ByteArrayInputStream(bytes));var bounded=new LimitedInputStream(gzip);var input=new DataInputStream(bounded)) {
            // 1.20.1's accounter measures heap allocation; serialized and allocation bounds are both enforced.
            CompoundTag storage=NbtIo.read(input,new NbtAccounter(4L*MAX_BYTES));
            if(storage==null || !storage.contains("Value",Tag.TAG_LIST) || input.read()!=-1) throw new DecoderException("Invalid Loli storage payload");
            var list=storage.getList("Value",Tag.TAG_COMPOUND);
            if(list.size()>8100 || storage.get("Value") instanceof net.minecraft.nbt.ListTag raw && !raw.isEmpty() && raw.getElementType()!=Tag.TAG_COMPOUND) throw new DecoderException("Invalid Loli storage entries");
            CompoundTag root=source.copy();root.put(LoliStorageData.STORAGE_KEY,list);return root;
        } catch(IOException | RuntimeException exception) { throw new DecoderException("Cannot decode bounded Loli storage",exception); }
    }

    private static final class LimitedInputStream extends FilterInputStream {
        private int remaining=MAX_BYTES;
        LimitedInputStream(InputStream input) { super(input); }
        @Override public int read() throws IOException {
            int result=in.read();if(result>=0 && --remaining<0) throw new IOException("Inflated storage exceeds 4 MiB");return result;
        }
        @Override public int read(byte[] data,int offset,int length) throws IOException {
            int count=in.read(data,offset,Math.min(length,remaining+1));
            if(count>0 && (remaining-=count)<0) throw new IOException("Inflated storage exceeds 4 MiB");return count;
        }
        @Override public long skip(long count) throws IOException {
            long skipped=in.skip(Math.min(count,remaining));remaining-=skipped;return skipped;
        }
    }
}
