package com.liymod.compat;

import com.liymod.menu.ToolMenuData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public interface ExtendedScreenHandlerFactory<D> extends net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory {
    D getScreenOpeningData(ServerPlayer player);
    @Override default void writeScreenOpeningData(ServerPlayer player,FriendlyByteBuf buffer) {
        Object data=getScreenOpeningData(player);
        if (data instanceof ToolMenuData tool) buffer.writeBoolean(tool.mainHand());
        else if (data instanceof BlockPos pos) buffer.writeBlockPos(pos);
        else throw new IllegalStateException("Unsupported menu data: "+data);
    }
}
