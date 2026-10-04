package com.liymod.mixin;

import com.liymod.storage.LoliStorageNetworkCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FriendlyByteBuf.class)
public abstract class StorageNetworkMixin {
    @ModifyVariable(method="writeNbt",at=@At("HEAD"),argsOnly=true)
    private CompoundTag liymod$compact(CompoundTag source) { return LoliStorageNetworkCodec.compact(source); }
    @Inject(method="readNbt(Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/CompoundTag;",at=@At("RETURN"),cancellable=true)
    private void liymod$expand(NbtAccounter accounter,CallbackInfoReturnable<CompoundTag> cir) { cir.setReturnValue(LoliStorageNetworkCodec.expand(cir.getReturnValue())); }
}
