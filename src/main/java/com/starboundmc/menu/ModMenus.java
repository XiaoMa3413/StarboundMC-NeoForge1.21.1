package com.starboundmc.menu;

import com.starboundmc.StarboundMC;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus
{
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, StarboundMC.MODID);

    public static final RegistryObject<MenuType<UpgradeMenu>> UPGRADE_MENU = MENUS.register("upgrade_menu",
            () -> IForgeMenuType.create((id, inventory, buf) -> new UpgradeMenu(id, inventory)));

    public static final RegistryObject<MenuType<ShipConsoleMenu>> SHIP_CONSOLE_MENU = MENUS.register("ship_console_menu",
            () -> IForgeMenuType.create((id, inventory, buf) -> new ShipConsoleMenu(id, inventory)));

    public static final RegistryObject<MenuType<ShipCrateMenu>> SHIP_CRATE_MENU = MENUS.register("ship_crate_menu",
            () -> IForgeMenuType.create((id, inventory, buf) -> new ShipCrateMenu(id, inventory)));

    public static final RegistryObject<MenuType<TeleporterMenu>> TELEPORTER_MENU = MENUS.register("teleporter_menu",
            () -> IForgeMenuType.create((id, inventory, buf) -> new TeleporterMenu(id, inventory)));

    public static final RegistryObject<MenuType<AlloyFurnaceMenu>> ALLOY_FURNACE_MENU = MENUS.register("alloy_furnace_menu",
            () -> IForgeMenuType.create((id, inventory, buf) -> new AlloyFurnaceMenu(id, inventory)));

    public static final RegistryObject<MenuType<FuelControllerMenu>> FUEL_CONTROLLER_MENU = MENUS.register("fuel_controller_menu",
            () -> IForgeMenuType.create((id, inventory, buf) -> new FuelControllerMenu(id, inventory)));

    public static void register(IEventBus modEventBus)
    {
        MENUS.register(modEventBus);
    }
}
