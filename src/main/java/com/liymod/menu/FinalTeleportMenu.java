package com.liymod.menu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class FinalTeleportMenu extends AbstractFinalToolMenu {
    public FinalTeleportMenu(int containerId, Inventory inventory, ToolMenuData data) {
        super(ModMenus.FINAL_TELEPORT, containerId, inventory, data);
    }

    public ResourceLocation getCurrentDimension() {
        return getPlayer().level().dimension().location();
    }
}
