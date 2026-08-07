package com.liymod.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;

public record ToolMenuData(boolean mainHand) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolMenuData> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> buffer.writeBoolean(data.mainHand),
            buffer -> new ToolMenuData(buffer.readBoolean())
    );

    public InteractionHand hand() {
        return mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}
