package com.liymod.block;

import com.liymod.menu.PasswordWorkbenchMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class PasswordWorkbenchBlock extends Block {
    private static final Component TITLE = Component.translatable("container.liymod.password_workbench");

    public PasswordWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide()) {
            player.openMenu(new Provider(level, pos));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private record Provider(Level level, BlockPos pos) implements ExtendedMenuProvider<BlockPos> {
        @Override
        public BlockPos getScreenOpeningData(ServerPlayer player) {
            return pos;
        }

        @Override
        public Component getDisplayName() {
            return TITLE;
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new PasswordWorkbenchMenu(
                    id,
                    inventory,
                    ContainerLevelAccess.create(level, pos)
            );
        }
    }
}
