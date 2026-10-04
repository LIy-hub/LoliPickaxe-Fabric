package com.liymod.menu;

import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliItemSettings;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;

public final class FinalConfigMenu extends AbstractFinalToolMenu {
    public static final List<LoliConfigOption> OPTIONS = Arrays.stream(LoliConfigOption.values())
            .filter(LoliConfigOption::itemOverride)
            .toList();

    public FinalConfigMenu(int containerId, Inventory inventory, ToolMenuData data) {
        super(ModMenus.FINAL_CONFIG, containerId, inventory, data);
    }

    public List<LoliConfigOption> getOptions() {
        return OPTIONS;
    }

    public String getEncodedValue(LoliConfigOption option) {
        Object value = switch (option.type()) {
            case BOOLEAN -> LoliItemSettings.getBoolean(getOwnerStack(), option);
            case INTEGER -> LoliItemSettings.getInt(getOwnerStack(), option);
            case DOUBLE -> LoliItemSettings.getDouble(getOwnerStack(), option);
            case STRING -> LoliItemSettings.getString(getOwnerStack(), option);
        };
        return option.encode(value);
    }

    public boolean update(String optionId, String encodedValue) {
        return LoliConfigOption.byId(optionId)
                .filter(LoliConfigOption::itemOverride)
                .map(option -> LoliItemSettings.set(getOwnerStack(), option, encodedValue))
                .orElse(false);
    }
}
