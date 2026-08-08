package com.liymod.menu;

import com.liymod.password.PasswordRecipeRegistry;
import com.liymod.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

public final class PasswordWorkbenchMenu extends AbstractContainerMenu {
    private final TransientCraftingContainer craft = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    private String password = "";

    public PasswordWorkbenchMenu(int id, Inventory inventory, FriendlyByteBuf data) { this(id, inventory, data.readBlockPos()); }
    public PasswordWorkbenchMenu(int id, Inventory inventory, BlockPos pos) { this(id, inventory, ContainerLevelAccess.create(inventory.player.level(), pos)); }
    public PasswordWorkbenchMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.PASSWORD_WORKBENCH.get(), id);
        this.access = access; this.player = inventory.player;
        addSlot(new Slot(result, 0, 124, 65) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public void onTake(Player player, ItemStack stack) {
                for (int i = 0; i < craft.getContainerSize(); i++) if (!craft.getItem(i).isEmpty()) craft.removeItem(i, 1);
                slotsChanged(craft); super.onTake(player, stack);
            }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) addSlot(new Slot(craft, col + row * 3, 30 + col * 18, 47 + row * 18));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 114 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 172));
    }

    public String password() { return password; }
    public ResultContainer result() { return result; }
    public void setPassword(String value) { password = com.liymod.network.ModNetwork.sanitizePassword(value); slotsChanged(craft); }

    @Override public void slotsChanged(Container container) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        access.execute((level, pos) -> {
            ItemStack output = level.getBlockState(pos).is(ModContent.PASSWORD_WORK_BENCH.get()) ? PasswordRecipeRegistry.find(craft, serverPlayer, password) : ItemStack.EMPTY;
            result.setItem(0, output); setRemoteSlot(0, output);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, output));
        });
    }

    @Override public boolean stillValid(Player player) { return stillValid(access, player, ModContent.PASSWORD_WORK_BENCH.get()); }
    @Override public void removed(Player player) { super.removed(player); if (!player.level().isClientSide) clearContainer(player, craft); result.clearContent(); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); ItemStack source = slot.getItem(); ItemStack copy = source.copy();
        if (index == 0) { if (!moveItemStackTo(source, 10, 46, true)) return ItemStack.EMPTY; slot.onQuickCraft(source, copy); }
        else if (index < 10) { if (!moveItemStackTo(source, 10, 46, false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(source, 1, 10, false)) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (source.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source); return copy;
    }
}
