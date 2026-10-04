package com.liymod.storage;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.io.*;
import java.util.zip.GZIPInputStream;
import net.minecraft.nbt.*;
/** Packet-only envelope. On-disk legacy NBT is never compressed or restructured. */
public final class LoliStorageNetworkCodec {
 private LoliStorageNetworkCodec() {}
 public static CompoundTag compact(CompoundTag source) {
  if(source == null) return null;
  CompoundTag storage=source.getCompound("LoliStorage");
  if(storage.sizeInBytes()<1024*1024) return source;
  if(storage.sizeInBytes()>4*1024*1024) throw new EncoderException("Loli storage exceeds its 4 MiB budget");
  try { ByteArrayOutputStream output=new ByteArrayOutputStream(); NbtIo.writeCompressed(storage,output);
   CompoundTag packed=new CompoundTag();packed.putInt("NetworkVersion",1);packed.putByteArray("Compressed",output.toByteArray());
   CompoundTag root=source.copy();root.put("LoliStorage",packed);return root;
  } catch(IOException e) { throw new EncoderException("Cannot encode Loli storage",e); }
 }
 public static CompoundTag expand(CompoundTag source) {
  if(source==null) return null;
  CompoundTag packed=source.getCompound("LoliStorage");
  if(packed.getInt("NetworkVersion")!=1 || !packed.contains("Compressed")) return source;
  try(DataInputStream input=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(packed.getByteArray("Compressed"))))) {
   CompoundTag storage=NbtIo.read(input,NbtAccounter.create(4*1024*1024));
   CompoundTag root=source.copy();root.put("LoliStorage",storage);return root;
  } catch(IOException | RuntimeException e) { throw new DecoderException("Cannot decode bounded Loli storage",e); }
 }
}
