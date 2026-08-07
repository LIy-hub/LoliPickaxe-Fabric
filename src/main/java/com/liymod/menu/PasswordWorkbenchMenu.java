package com.liymod.menu;

import com.liymod.block.ModBlocks;
import com.liymod.password.PasswordRecipeRegistry;
import com.liymod.network.PasswordUpdatePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
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
    private static final int RESULT_SLOT = 0;
    private static final int INPUT_START = 1;
    private static final int INPUT_END = 10;
    private static final int INVENTORY_START = 10;
    private static final int INVENTORY_END = 37;
    private static final int HOTBAR_START = 37;
    private static final int HOTBAR_END = 46;

    private final TransientCraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    private String password = "";

    public PasswordWorkbenchMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, ContainerLevelAccess.create(inventory.player.level(), pos));
    }

    public PasswordWorkbenchMenu(
            int containerId,
            Inventory inventory,
            ContainerLevelAccess access
    ) {
        super(ModMenus.PASSWORD_WORKBENCH, containerId);
        this.access = access;
        this.player = inventory.player;

        addSlot(new PasswordResultSlot(player, craftSlots, resultSlots, 0, 124, 65));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(craftSlots, column + row * 3, 30 + column * 18, 47 + row * 18));
            }
        }
        addStandardInventorySlots(inventory, 8, 114);
    }

    public TransientCraftingContainer getCraftSlots() {
        return craftSlots;
    }

    public ResultContainer getResultSlots() {
        return resultSlots;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String requestedPassword) {
        password = PasswordUpdatePayload.sanitize(requestedPassword);
        slotsChanged(craftSlots);
    }

    @Override
    public void slotsChanged(Container container) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel)) {
            return;
        }
        access.execute((level, pos) -> {
            if (!level.getBlockState(pos).is(ModBlocks.PASSWORD_WORK_BENCH)) {
                resultSlots.setItem(0, ItemStack.EMPTY);
                return;
            }
            ItemStack result = PasswordRecipeRegistry.findResult(
                    craftSlots,
                    serverPlayer,
                    password
            );
            resultSlots.setItem(0, result);
            setRemoteSlot(RESULT_SLOT, result);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    containerId,
                    incrementStateId(),
                    RESULT_SLOT,
                    result
            ));
        });
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            clearContainer(player, craftSlots);
        }
        resultSlots.clearContent();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PASSWORD_WORK_BENCH);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack original = slotStack.copy();
        if (slotIndex == RESULT_SLOT) {
            if (!moveItemStackTo(slotStack, INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(slotStack, original);
        } else if (slotIndex >= INPUT_START && slotIndex < INPUT_END) {
            if (!moveItemStackTo(slotStack, INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= INVENTORY_START && slotIndex < HOTBAR_END) {
            if (!moveItemStackTo(slotStack, INPUT_START, INPUT_END, false)) {
                if (slotIndex < INVENTORY_END) {
                    if (!moveItemStackTo(slotStack, HOTBAR_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(slotStack, INVENTORY_START, INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (slotStack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, original);
        return original;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    private final class PasswordResultSlot extends Slot {
        private PasswordResultSlot(
                Player player,
                Container input,
                Container result,
                int slot,
                int x,
                int y
        ) {
            super(result, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            for (int index = 0; index < craftSlots.getContainerSize(); index++) {
                if (!craftSlots.getItem(index).isEmpty()) {
                    craftSlots.removeItem(index, 1);
                }
            }
            slotsChanged(craftSlots);
            super.onTake(player, stack);
        }
    }
}
