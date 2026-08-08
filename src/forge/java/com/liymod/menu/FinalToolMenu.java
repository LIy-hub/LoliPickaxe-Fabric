package com.liymod.menu;

import com.liymod.config.FinalToolSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class FinalToolMenu extends AbstractContainerMenu {
    public enum Mode { CONFIG, ENCHANTMENT, EFFECT, TELEPORT }
    private final Player player;
    private final InteractionHand hand;
    private final ItemStack ownerStack;
    private final Mode mode;

    public FinalToolMenu(Mode mode, int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(mode, id, inventory, buffer.readEnum(InteractionHand.class));
    }

    public FinalToolMenu(Mode mode, int id, Inventory inventory, InteractionHand hand) {
        super(typeFor(mode), id);
        this.player = inventory.player;
        this.hand = hand;
        this.ownerStack = player.getItemInHand(hand);
        this.mode = mode;
    }

    public Player player() { return player; }
    public InteractionHand hand() { return hand; }
    public ItemStack tool() { return ownerStack; }
    public Mode mode() { return mode; }

    @Override public boolean stillValid(Player player) {
        return player == this.player && player.getItemInHand(hand) == ownerStack && FinalToolSettings.isFinal(ownerStack);
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    private static MenuType<?> typeFor(Mode mode) {
        return switch (mode) {
            case CONFIG -> ModMenus.FINAL_CONFIG.get();
            case ENCHANTMENT -> ModMenus.FINAL_ENCHANTMENT.get();
            case EFFECT -> ModMenus.FINAL_EFFECT.get();
            case TELEPORT -> ModMenus.FINAL_TELEPORT.get();
        };
    }
}
