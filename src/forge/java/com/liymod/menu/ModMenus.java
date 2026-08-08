package com.liymod.menu;

import com.liymod.LiyMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, LiyMod.MOD_ID);
    public static final RegistryObject<MenuType<StorageMenu>> STORAGE = MENUS.register("storage", () -> IForgeMenuType.create(StorageMenu::new));
    public static final RegistryObject<MenuType<PasswordWorkbenchMenu>> PASSWORD_WORKBENCH = MENUS.register("password_workbench", () -> IForgeMenuType.create(PasswordWorkbenchMenu::new));
    public static final RegistryObject<MenuType<FinalToolMenu>> FINAL_CONFIG = MENUS.register("final_config", () -> IForgeMenuType.create((id, inv, buf) -> new FinalToolMenu(FinalToolMenu.Mode.CONFIG, id, inv, buf)));
    public static final RegistryObject<MenuType<FinalToolMenu>> FINAL_ENCHANTMENT = MENUS.register("final_enchantment", () -> IForgeMenuType.create((id, inv, buf) -> new FinalToolMenu(FinalToolMenu.Mode.ENCHANTMENT, id, inv, buf)));
    public static final RegistryObject<MenuType<FinalToolMenu>> FINAL_EFFECT = MENUS.register("final_effect", () -> IForgeMenuType.create((id, inv, buf) -> new FinalToolMenu(FinalToolMenu.Mode.EFFECT, id, inv, buf)));
    public static final RegistryObject<MenuType<FinalToolMenu>> FINAL_TELEPORT = MENUS.register("final_teleport", () -> IForgeMenuType.create((id, inv, buf) -> new FinalToolMenu(FinalToolMenu.Mode.TELEPORT, id, inv, buf)));
    private ModMenus() { }
    public static void register(IEventBus bus) { MENUS.register(bus); }
}
