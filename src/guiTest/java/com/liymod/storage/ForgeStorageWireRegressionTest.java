package com.liymod.storage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
public final class ForgeStorageWireRegressionTest {
    public static void main(String[] args) throws Exception {
        CompoundTag root=new CompoundTag();ListTag list=new ListTag();
        for(int slot=0;slot<6000;slot++) { CompoundTag entry=new CompoundTag();entry.putInt("Slot",slot);entry.putString("payload","x".repeat(220));list.add(entry); }
        root.put("LoliStorage",list);root.putString("Owner","unchanged");
        CompoundTag packed=LoliStorageNetworkCodec.compact(root);
        require(packed!=root && packed.getCompound("LoliStorage").contains("Compressed"),"Large storage must compress on wire");
        require(root.get("LoliStorage") instanceof ListTag,"Wire packing changed saved disk layout");
        require(LoliStorageNetworkCodec.expand(packed).equals(root),"Wire roundtrip changed storage/owner");
        CompoundTag small=new CompoundTag();small.put("LoliStorage",new ListTag());
        require(LoliStorageNetworkCodec.compact(small)==small,"Small storage should keep vanilla NBT");
        CompoundTag broken=packed.copy();broken.getCompound("LoliStorage").putInt("NetworkVersion",2);
        reject(()->LoliStorageNetworkCodec.expand(broken),"Unsupported wire version accepted");
        CompoundTag corrupt=packed.copy();corrupt.getCompound("LoliStorage").putByteArray("Compressed",new byte[]{1,2,3});
        reject(()->LoliStorageNetworkCodec.expand(corrupt),"Corrupt compressed storage accepted");
        CompoundTag oversized=new CompoundTag();CompoundTag huge=new CompoundTag();huge.putByteArray("large",new byte[4*1024*1024]);ListTag entries=new ListTag();entries.add(huge);oversized.put("LoliStorage",entries);
        try { LoliStorageNetworkCodec.compact(oversized);throw new AssertionError("Over-budget storage encoded"); }catch(EncoderException expected) { }
        CompoundTag inflated=new CompoundTag();inflated.put("Value",entries);
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
        try(var gzip=new java.util.zip.GZIPOutputStream(bytes);var out=new java.io.DataOutputStream(gzip)) { net.minecraft.nbt.NbtIo.write(inflated,out); }
        CompoundTag bomb=packed.copy();bomb.getCompound("LoliStorage").putByteArray("Compressed",bytes.toByteArray());
        reject(()->LoliStorageNetworkCodec.expand(bomb),"Over-budget inflation accepted");
        System.out.println("FORGE_STORAGE_WIRE_OK bounded roundtrip corrupt oversized diskFormat=PASS");
    }
    private static void reject(Runnable task,String reason) { try { task.run();throw new AssertionError(reason); }catch(DecoderException expected) { } }
    private static void require(boolean condition,String reason) { if(!condition) throw new AssertionError(reason); }
}
