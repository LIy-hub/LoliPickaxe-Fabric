package com.liymod.mixin.client;

import com.liymod.client.gui.LoliChatRainbow;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ChatComponent.class)
public abstract class ChatComponentDivineMixin {
    @ModifyArg(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;<init>(Lnet/minecraft/client/multiplayer/chat/GuiMessage;Lnet/minecraft/util/FormattedCharSequence;Z)V"), index = 1)
    private FormattedCharSequence liymod$animateDivineTitle(GuiMessage message,
                                                           FormattedCharSequence line, boolean endOfEntry) {
        return LoliChatRainbow.wrap(message.content(), line);
    }
}
