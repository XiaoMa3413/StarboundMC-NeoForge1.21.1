package com.starboundmc.menu;

import com.starboundmc.StarboundMC;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    // Network/data-dependent menus retain small shells until their dedicated
    // stages; independent menus use their real container implementations.
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, StarboundMC.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<Stage2UpgradeMenu>> UPGRADE_MENU =
            MENUS.register("upgrade_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new Stage2UpgradeMenu(id)));
    public static final DeferredHolder<MenuType<?>, MenuType<ShipConsoleMenu>> SHIP_CONSOLE_MENU =
            MENUS.register("ship_console_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new ShipConsoleMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<ShipCrateMenu>> SHIP_CRATE_MENU =
            MENUS.register("ship_crate_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new ShipCrateMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<TeleporterMenu>> TELEPORTER_MENU =
            MENUS.register("teleporter_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new TeleporterMenu(id, inventory)));
    public static final DeferredHolder<MenuType<?>, MenuType<Stage2AlloyFurnaceMenu>> ALLOY_FURNACE_MENU =
            MENUS.register("alloy_furnace_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new Stage2AlloyFurnaceMenu(id)));
    public static final DeferredHolder<MenuType<?>, MenuType<Stage2FuelControllerMenu>> FUEL_CONTROLLER_MENU =
            MENUS.register("fuel_controller_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new Stage2FuelControllerMenu(id)));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private abstract static class Stage2Menu extends AbstractContainerMenu {
        protected Stage2Menu(MenuType<?> type, int containerId) {
            super(type, containerId);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slotIndex) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final class Stage2UpgradeMenu extends Stage2Menu {
        public Stage2UpgradeMenu(int containerId) {
            super(UPGRADE_MENU.get(), containerId);
        }
    }

    public static final class Stage2AlloyFurnaceMenu extends Stage2Menu {
        public Stage2AlloyFurnaceMenu(int containerId) {
            super(ALLOY_FURNACE_MENU.get(), containerId);
        }
    }

    public static final class Stage2FuelControllerMenu extends Stage2Menu {
        public Stage2FuelControllerMenu(int containerId) {
            super(FUEL_CONTROLLER_MENU.get(), containerId);
        }
    }
}
