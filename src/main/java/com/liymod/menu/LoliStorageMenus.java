package com.liymod.menu;

import com.liymod.network.StorageOpenPayload;
import com.liymod.storage.LoliStorageData;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class LoliStorageMenus {
    private LoliStorageMenus() {
    }

    public static boolean open(
            ServerPlayer player,
            InteractionHand hand,
            StorageOpenPayload.Mode mode
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (!LoliStorageData.hasStorage(stack)) {
            return false;
        }
        player.openMenu(new Provider(hand, mode));
        return true;
    }

    public static boolean dropAll(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!LoliStorageData.hasStorage(stack)) {
            return false;
        }
        LoliStorageData storage = LoliStorageData.open(stack);
        for (ItemStack stored : storage.removeAllStoredItems()) {
            player.drop(stored, false);
        }
        return true;
    }

    private record Provider(
            InteractionHand hand,
            StorageOpenPayload.Mode mode
    ) implements ExtendedMenuProvider<ToolMenuData> {
        @Override
        public ToolMenuData getScreenOpeningData(ServerPlayer player) {
            return new ToolMenuData(hand == InteractionHand.MAIN_HAND);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable(mode == StorageOpenPayload.Mode.STORAGE
                    ? "container.liymod.loli_storage"
                    : "container.liymod.loli_blacklist");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            ToolMenuData data = new ToolMenuData(hand == InteractionHand.MAIN_HAND);
            return mode == StorageOpenPayload.Mode.STORAGE
                    ? new StorageMenu(id, inventory, data)
                    : new BlacklistMenu(id, inventory, data);
        }
    }
}
