package com.liymod.menu;

import com.liymod.config.LoliItemSettings;
import com.liymod.network.LoliMenuOpenPayload;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class FinalToolMenus {
    private FinalToolMenus() {
    }

    public static boolean open(ServerPlayer player, InteractionHand hand, LoliMenuOpenPayload.Mode mode) {
        ItemStack stack = player.getItemInHand(hand);
        if (!LoliItemSettings.isFinalPickaxe(stack)) {
            return false;
        }
        LoliItemSettings.ensureDefaults(stack);
        player.openMenu(new Provider(hand, mode));
        return true;
    }

    private record Provider(
            InteractionHand hand,
            LoliMenuOpenPayload.Mode mode
    ) implements ExtendedMenuProvider<ToolMenuData> {
        @Override
        public ToolMenuData getScreenOpeningData(ServerPlayer player) {
            return new ToolMenuData(hand == InteractionHand.MAIN_HAND);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.liymod." + mode.translationSuffix());
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            ToolMenuData data = new ToolMenuData(hand == InteractionHand.MAIN_HAND);
            return switch (mode) {
                case CONFIG -> new FinalConfigMenu(id, inventory, data);
                case ENCHANTMENT -> new FinalEnchantmentMenu(id, inventory, data);
                case EFFECT -> new FinalEffectMenu(id, inventory, data);
                case TELEPORT -> new FinalTeleportMenu(id, inventory, data);
            };
        }
    }
}
