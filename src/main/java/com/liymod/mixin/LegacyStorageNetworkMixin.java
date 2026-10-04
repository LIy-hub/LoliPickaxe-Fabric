package com.liymod.mixin;
import com.liymod.storage.LoliStorageNetworkCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(FriendlyByteBuf.class)
public abstract class LegacyStorageNetworkMixin {
 @ModifyArg(method="writeItem",at=@At(value="INVOKE",target="Lnet/minecraft/network/FriendlyByteBuf;writeNbt(Lnet/minecraft/nbt/Tag;)Lnet/minecraft/network/FriendlyByteBuf;"),index=0)
 private Tag liymod$compactStorage(Tag tag) { return tag instanceof CompoundTag compound ? LoliStorageNetworkCodec.compact(compound) : tag; }
 @Redirect(method="readItem",at=@At(value="INVOKE",target="Lnet/minecraft/network/FriendlyByteBuf;readNbt()Lnet/minecraft/nbt/CompoundTag;"))
 private CompoundTag liymod$expandStorage(FriendlyByteBuf buffer) { return LoliStorageNetworkCodec.expand(buffer.readNbt()); }
}
