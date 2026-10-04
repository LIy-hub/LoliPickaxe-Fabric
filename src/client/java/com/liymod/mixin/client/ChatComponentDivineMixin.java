package com.liymod.mixin.client;
import com.liymod.client.gui.LoliChatRainbow;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChatComponent.class)
public abstract class ChatComponentDivineMixin {
 @Unique private Component liymod$message;
 @Inject(method="addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",at=@At("HEAD"))
 private void liymod$captureMessage(Component message,MessageSignature signature,int time,GuiMessageTag tag,boolean refresh,CallbackInfo ci) { liymod$message=message; }
 @ModifyArg(method="addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/GuiMessage$Line;<init>(ILnet/minecraft/util/FormattedCharSequence;Lnet/minecraft/client/GuiMessageTag;Z)V"),index=1)
 private FormattedCharSequence liymod$animateTitle(FormattedCharSequence line) { return LoliChatRainbow.wrap(liymod$message,line); }
 @Inject(method="addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",at=@At("RETURN"))
 private void liymod$clearMessage(Component message,MessageSignature signature,int time,GuiMessageTag tag,boolean refresh,CallbackInfo ci) { liymod$message=null; }
}
