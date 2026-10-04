package com.liymod.mixin;

import com.liymod.item.SmallLoliMiningEvents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies block-drop processing on versions without Fabric's MODIFY_DROPS event. */
@Mixin(LootTable.class)
public abstract class LootTableMixin {
    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", at = @At("RETURN"))
    private void liymod$modifyDrops(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> callback) {
        SmallLoliMiningEvents.modifyDrops(context, callback.getReturnValue());
    }
}
