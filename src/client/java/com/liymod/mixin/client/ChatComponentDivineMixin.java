package com.liymod.mixin.client;

import com.liymod.client.gui.LoliChatRainbow;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentDivineMixin {
    @Unique private Component liymod$incomingMessage;

    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"))
    private void liymod$captureMessage(GuiMessage message, CallbackInfo ci) {
        liymod$incomingMessage = message.content();
    }

    @ModifyArg(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/GuiMessage$Line;<init>(ILnet/minecraft/util/FormattedCharSequence;Lnet/minecraft/client/GuiMessageTag;Z)V"), index = 1)
    private FormattedCharSequence liymod$animateDivineTitle(FormattedCharSequence line) {
        return liymod$incomingMessage == null ? line : LoliChatRainbow.wrap(liymod$incomingMessage, line);
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At("RETURN"))
    private void liymod$clearMessage(GuiMessage message, CallbackInfo ci) {
        liymod$incomingMessage = null;
    }
}
