package com.liymod.compat;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class ExtendedScreenHandlerType<T extends AbstractContainerMenu,D>
        extends net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<T> {
    public interface Factory<T,D> { T create(int id,Inventory inventory,D data); }
    public ExtendedScreenHandlerType(Factory<T,D> factory,StreamCodec<RegistryFriendlyByteBuf,D> codec) {
        super((id,inventory,buffer)->factory.create(id,inventory,
                codec.decode(new RegistryFriendlyByteBuf(buffer,inventory.player.level().registryAccess()))));
    }
}
