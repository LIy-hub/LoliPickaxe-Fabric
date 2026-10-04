package com.liymod.menu;

import com.liymod.item.LoliFinalEffects;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class FinalEffectMenu extends AbstractFinalToolMenu {
    public FinalEffectMenu(int containerId, Inventory inventory, ToolMenuData data) {
        super(ModMenus.FINAL_EFFECT, containerId, inventory, data);
    }

    public Map<Identifier, Integer> getEffects() {
        return LoliFinalEffects.get(getOwnerStack());
    }
}
