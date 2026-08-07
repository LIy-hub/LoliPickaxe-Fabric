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

/** Bundled legacy art card and grouped album, with server-owned assignment metadata. */
public final class LoliCardItem extends Item {
    public enum Kind {
        CARD,
        ALBUM
    }

    private final Kind kind;

    public LoliCardItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public ItemStack createArtStack(String id) {
        if (kind != Kind.CARD) {
            throw new IllegalStateException("Only standalone cards carry an art id");
        }
        ItemStack stack = new ItemStack(this);
        LoliCardData.setArt(stack, id);
        return stack;
    }

    public ItemStack createAlbumStack(String group) {
        if (kind != Kind.ALBUM) {
            throw new IllegalStateException("Only albums carry an art group");
        }
        ItemStack stack = new ItemStack(this);
        LoliCardData.setGroup(stack, group);
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.CARD) {
            String id = LoliCardData.art(stack).orElseGet(() -> {
                String selected = LoliCardCatalog.randomStandalone(level.getRandom()).id();
                LoliCardData.setArt(stack, selected);
                return selected;
            });
            ServerPlayNetworking.send(
                    (ServerPlayer) player,
                    new LoliCardOpenPayload(LoliCardOpenPayload.Mode.CARD, id)
            );
        } else {
            String group = LoliCardData.group(stack).orElseGet(() -> {
                String selected = LoliCardCatalog.randomGroup(level.getRandom());
                LoliCardData.setGroup(stack, selected);
                return selected;
            });
            ServerPlayNetworking.send(
                    (ServerPlayer) player,
                    new LoliCardOpenPayload(LoliCardOpenPayload.Mode.ALBUM, group)
            );
        }
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
        if (kind == Kind.CARD) {
            LoliCardData.art(stack)
                    .flatMap(LoliCardCatalog::byId)
                    .ifPresent(art -> tooltip.accept(Component.literal(art.displayName())));
        } else {
            LoliCardData.group(stack).ifPresent(group -> tooltip.accept(
                    Component.translatable("gui.liymod.card.group." + group)
            ));
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
