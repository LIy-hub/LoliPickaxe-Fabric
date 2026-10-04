package com.liymod.menu;

import com.liymod.config.LoliItemSettings;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Slotless common menu base that locks every mutation to the exact held final pickaxe. */
public abstract class AbstractFinalToolMenu extends AbstractContainerMenu {
    private final Player player;
    private final InteractionHand hand;
    private final ItemStack ownerStack;

    protected AbstractFinalToolMenu(
            MenuType<?> type,
            int containerId,
            Inventory inventory,
            ToolMenuData data
    ) {
        super(type, containerId);
        this.player = inventory.player;
        this.hand = data.hand();
        this.ownerStack = player.getItemInHand(hand);
    }

    public final Player getPlayer() {
        return player;
    }

    public final InteractionHand getHand() {
        return hand;
    }

    public final ItemStack getOwnerStack() {
        return ownerStack;
    }

    @Override
    public final boolean stillValid(Player player) {
        return player == this.player
                && player.getItemInHand(hand) == ownerStack
                && LoliItemSettings.isFinalPickaxe(ownerStack);
    }

    @Override
    public final ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
