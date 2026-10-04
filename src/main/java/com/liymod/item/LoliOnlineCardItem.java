package com.liymod.item;

import com.liymod.network.LoliCardOpenPayload;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Configurable HTTPS image card. Downloading is client-only, asynchronous and bounded. */
public final class LoliOnlineCardItem extends Item {
    public static final java.util.List<String> DEFAULT_URLS = java.util.List.of(
            "https://bigimg.cheerfun.dev/get/https://i.pximg.net/img-original/img/2017/03/18/03/44/39/61965296_p0.png",
            "https://bigimg.cheerfun.dev/get/https://i.pximg.net/img-original/img/2015/10/23/18/05/06/53170539_p0.jpg",
            "https://bigimg.cheerfun.dev/get/https://i.pximg.net/img-original/img/2015/09/27/07/15/20/52735806_p0.jpg"
    );

    public LoliOnlineCardItem(Properties properties) {
        super(properties);
    }

    public ItemStack createUrlStack(String url) {
        ItemStack stack = new ItemStack(this);
        if (!LoliCardData.setUrl(stack, url)) {
            throw new IllegalArgumentException("Unsafe online-card URL");
        }
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        String url = LoliCardData.url(stack);
        LoliCardOpenPayload.Mode mode = player.isShiftKeyDown()
                ? LoliCardOpenPayload.Mode.ONLINE_CONFIG
                : LoliCardOpenPayload.Mode.ONLINE_VIEW;
        String value = mode == LoliCardOpenPayload.Mode.ONLINE_CONFIG
                ? hand.name() + "\n" + url
                : url;
        ServerPlayNetworking.send((ServerPlayer) player, new LoliCardOpenPayload(mode, value));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        String url = LoliCardData.url(stack);
        if (!url.isEmpty()) {
            tooltip.accept(Component.literal(url));
        }
        tooltip.accept(Component.translatable("item.liymod.loli_card_online.hint"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
