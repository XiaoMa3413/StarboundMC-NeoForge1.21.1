package com.starboundmc.menu;

import com.starboundmc.StarboundMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    // Stage 6 restores all inventory-bearing menus. The ship console keeps its
    // real authority boundary while its high-density screen waits for stage 9.
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, StarboundMC.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<UpgradeMenu>> UPGRADE_MENU =
            MENUS.register("upgrade_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new UpgradeMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<ShipConsoleMenu>> SHIP_CONSOLE_MENU =
            MENUS.register("ship_console_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new ShipConsoleMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<StarmapTerminalMenu>> STARMAP_TERMINAL_MENU =
            MENUS.register("starmap_terminal_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new StarmapTerminalMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<ShipCrateMenu>> SHIP_CRATE_MENU =
            MENUS.register("ship_crate_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new ShipCrateMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<TeleporterMenu>> TELEPORTER_MENU =
            MENUS.register("teleporter_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new TeleporterMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<AlloyFurnaceMenu>> ALLOY_FURNACE_MENU =
            MENUS.register("alloy_furnace_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new AlloyFurnaceMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<FuelControllerMenu>> FUEL_CONTROLLER_MENU =
            MENUS.register("fuel_controller_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new FuelControllerMenu(id, inventory)));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

}
