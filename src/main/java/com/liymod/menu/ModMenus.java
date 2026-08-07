package com.liymod.menu;

import com.liymod.LiyMod;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final MenuType<PasswordWorkbenchMenu> PASSWORD_WORKBENCH = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "password_workbench"),
            new ExtendedMenuType<>(PasswordWorkbenchMenu::new, BlockPos.STREAM_CODEC)
    );
    public static final MenuType<StorageMenu> STORAGE = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_storage"),
            new ExtendedMenuType<>(StorageMenu::new, ToolMenuData.STREAM_CODEC)
    );
    public static final MenuType<BlacklistMenu> BLACKLIST = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_blacklist"),
            new ExtendedMenuType<>(BlacklistMenu::new, ToolMenuData.STREAM_CODEC)
    );
    public static final MenuType<FinalConfigMenu> FINAL_CONFIG = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_config"),
            new ExtendedMenuType<>(FinalConfigMenu::new, ToolMenuData.STREAM_CODEC)
    );
    public static final MenuType<FinalEnchantmentMenu> FINAL_ENCHANTMENT = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_enchantment"),
            new ExtendedMenuType<>(FinalEnchantmentMenu::new, ToolMenuData.STREAM_CODEC)
    );
    public static final MenuType<FinalEffectMenu> FINAL_EFFECT = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_effect"),
            new ExtendedMenuType<>(FinalEffectMenu::new, ToolMenuData.STREAM_CODEC)
    );
    public static final MenuType<FinalTeleportMenu> FINAL_TELEPORT = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_teleport"),
            new ExtendedMenuType<>(FinalTeleportMenu::new, ToolMenuData.STREAM_CODEC)
    );

    private ModMenus() {
    }

    public static void registerMenus() {
        LiyMod.LOGGER.info("Registering menus for {}", LiyMod.MOD_ID);
    }
}
