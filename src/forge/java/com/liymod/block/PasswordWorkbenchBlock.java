package com.liymod.block;

import com.liymod.menu.PasswordWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public final class PasswordWorkbenchBlock extends Block {
    public PasswordWorkbenchBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("container.liymod.password_workbench"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player ignored) { return new PasswordWorkbenchMenu(id, inventory, pos); }
        }, buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
