package com.liymod.menu;

import com.liymod.LiyMod;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final MenuType<PasswordWorkbenchMenu> PASSWORD_WORKBENCH = Registry.register(
            BuiltInRegistries.MENU,
            new ResourceLocation(LiyMod.MOD_ID, "password_workbench"),
            new ExtendedScreenHandlerType<>(PasswordWorkbenchMenu::new, BlockPos.STREAM_CODEC)
    );
    public static final MenuType<StorageMenu> STORAGE = Registry.register(
            BuiltInRegistries.MENU,
            new ResourceLocation(LiyMod.MOD_ID, "loli_storage"),
            new ExtendedScreenHandlerType<>(StorageMenu::new, ToolMenuData.STREAM_CODEC)
    );
    public static final MenuType<BlacklistMenu> BLACKLIST = Registry.register(
            BuiltInRegistries.MENU,
            new ResourceLocation(LiyMod.MOD_ID, "loli_blacklist"),
            new ExtendedScreenHandlerType<>(BlacklistMenu::new, ToolMenuData.STREAM_CODEC)
    );
    public static final MenuType<FinalConfigMenu> FINAL_CONFIG = Registry.register(
            BuiltInRegistries.MENU,
            new ResourceLocation(LiyMod.MOD_ID, "loli_config"),
            new ExtendedScreenHandlerType<>(FinalConfigMenu::new, ToolMenuData.STREAM_CODEC)
    );
    public static final MenuType<FinalEnchantmentMenu> FINAL_ENCHANTMENT = Registry.register(
            BuiltInRegistries.MENU,
            new ResourceLocation(LiyMod.MOD_ID, "loli_enchantment"),
            new ExtendedScreenHandlerType<>(FinalEnchantmentMenu::new, ToolMenuData.STREAM_CODEC)
    );
    public static final MenuType<FinalEffectMenu> FINAL_EFFECT = Registry.register(
            BuiltInRegistries.MENU,
            new ResourceLocation(LiyMod.MOD_ID, "loli_effect"),
            new ExtendedScreenHandlerType<>(FinalEffectMenu::new, ToolMenuData.STREAM_CODEC)
    );
    public static final MenuType<FinalTeleportMenu> FINAL_TELEPORT = Registry.register(
            BuiltInRegistries.MENU,
            new ResourceLocation(LiyMod.MOD_ID, "loli_teleport"),
            new ExtendedScreenHandlerType<>(FinalTeleportMenu::new, ToolMenuData.STREAM_CODEC)
    );

    private ModMenus() {
    }

    public static void registerMenus() {
        LiyMod.LOGGER.info("Registering menus for {}", LiyMod.MOD_ID);
    }
}
