package com.liymod.password;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface PasswordRecipe {
    ItemStack assemble(CraftingContainer input, ServerPlayer player, String password);
}
