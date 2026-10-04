package com.liymod.mixin;
import com.liymod.storage.LoliStorageNetworkCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(FriendlyByteBuf.class)
public abstract class LegacyStorageNetworkMixin {
 @ModifyArg(method="writeItem",at=@At(value="INVOKE",target="Lnet/minecraft/network/FriendlyByteBuf;writeNbt(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/network/FriendlyByteBuf;"),index=0)
 private CompoundTag liymod$compactStorage(CompoundTag tag) { return LoliStorageNetworkCodec.compact(tag); }
 @Redirect(method="readItem",at=@At(value="INVOKE",target="Lnet/minecraft/network/FriendlyByteBuf;readNbt()Lnet/minecraft/nbt/CompoundTag;"))
 private CompoundTag liymod$expandStorage(FriendlyByteBuf buffer) { return LoliStorageNetworkCodec.expand(buffer.readNbt()); }
}
