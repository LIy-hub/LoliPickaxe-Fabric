package com.liymod.mixin;

import com.liymod.storage.LoliStorageNetworkCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataComponents.class)
public abstract class CustomDataStorageNetworkMixin {
    // 26.2 custom_data otherwise derives its network codec from the persistent CODEC.
    @Inject(method = "lambda$static$0", at = @At("RETURN"), cancellable = true)
    private static void liymod$compressStorageForNetwork(
            DataComponentType.Builder<CustomData> builder,
            CallbackInfoReturnable<DataComponentType.Builder<CustomData>> cir
    ) {
        cir.setReturnValue(LoliStorageNetworkCodec.configure(cir.getReturnValue()));
    }
}
