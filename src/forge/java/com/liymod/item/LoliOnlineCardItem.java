package com.liymod.item;

import com.liymod.network.ModNetwork;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class LoliOnlineCardItem extends Item {
    public LoliOnlineCardItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ModNetwork.CardMode mode = player.isShiftKeyDown() ? ModNetwork.CardMode.ONLINE_CONFIG : ModNetwork.CardMode.ONLINE_VIEW;
            String value = mode == ModNetwork.CardMode.ONLINE_CONFIG ? hand.name() + "\n" + LoliCardData.url(stack) : LoliCardData.url(stack);
            ModNetwork.sendCard(serverPlayer, new ModNetwork.CardOpenPacket(mode, value));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String url = LoliCardData.url(stack); if (!url.isEmpty()) tooltip.add(Component.literal(url));
        tooltip.add(Component.translatable("item.liymod.loli_card_online.hint")); super.appendHoverText(stack, level, tooltip, flag);
    }
}
