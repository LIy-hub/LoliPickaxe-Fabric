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

public final class LoliCardItem extends Item {
    private final boolean album;
    public LoliCardItem(boolean album, Properties properties) { super(properties); this.album = album; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            String value = "album";
            if (!album) {
                value = LoliCardData.art(stack);
                if (LoliCardCatalog.byId(value).isEmpty()) { value = LoliCardCatalog.STANDALONE.get(level.random.nextInt(LoliCardCatalog.STANDALONE.size())).id(); LoliCardData.art(stack, value); }
            }
            ModNetwork.sendCard(serverPlayer, new ModNetwork.CardOpenPacket(album ? ModNetwork.CardMode.ALBUM : ModNetwork.CardMode.CARD, value));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!album) LoliCardCatalog.byId(LoliCardData.art(stack)).ifPresent(art -> tooltip.add(Component.literal(art.id())));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
