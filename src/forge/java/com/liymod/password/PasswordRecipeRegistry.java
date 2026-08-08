package com.liymod.password;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

public final class PasswordRecipeRegistry {
    private static final List<PasswordRecipe> RECIPES = new CopyOnWriteArrayList<>();
    private PasswordRecipeRegistry() { }
    public static void register(PasswordRecipe recipe) { if (recipe != null) RECIPES.add(recipe); }
    public static void clear() { RECIPES.clear(); }
    public static ItemStack find(CraftingContainer input, ServerPlayer player, String password) {
        for (PasswordRecipe recipe : RECIPES) { ItemStack result = recipe.assemble(input, player, password); if (result != null && !result.isEmpty()) return result.copy(); }
        return ItemStack.EMPTY;
    }
}
