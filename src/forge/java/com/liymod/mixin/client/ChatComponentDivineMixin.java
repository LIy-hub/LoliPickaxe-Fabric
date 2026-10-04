package com.liymod.mixin.client;

import com.liymod.client.gui.LoliChatRainbow;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentDivineMixin {
    @Unique private Component liymod$summary;
    @Inject(method="addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",at=@At("HEAD"))
    private void liymod$capture(Component message, MessageSignature signature, int time, GuiMessageTag tag, boolean refresh, CallbackInfo ci) { liymod$summary=message; }
    @ModifyArg(method="addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/GuiMessage$Line;<init>(ILnet/minecraft/util/FormattedCharSequence;Lnet/minecraft/client/GuiMessageTag;Z)V"),index=1)
    private FormattedCharSequence liymod$animate(FormattedCharSequence line) { return LoliChatRainbow.wrap(liymod$summary,line); }
}
