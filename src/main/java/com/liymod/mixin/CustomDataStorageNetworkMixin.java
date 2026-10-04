package com.liymod.mixin;
import com.liymod.storage.LoliStorageNetworkCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
/** Wrap only the native custom-data wire codecs; persistent codecs remain unchanged. */
@Mixin(DataComponents.class)
public abstract class CustomDataStorageNetworkMixin {
    @Redirect(method = "method_57923", at = @At(value = "FIELD", target =
            "Lnet/minecraft/world/item/component/CustomData;STREAM_CODEC:Lnet/minecraft/network/codec/StreamCodec;"))
    private static StreamCodec<ByteBuf, CustomData> liymod$compressStorageForNetwork() {
        return LoliStorageNetworkCodec.wrap(CustomData.STREAM_CODEC);
    }
}
