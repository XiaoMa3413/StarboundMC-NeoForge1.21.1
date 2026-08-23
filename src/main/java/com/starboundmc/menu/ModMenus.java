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
    // Stage 2 restores the real inventory/menu behavior behind these stable IDs.
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, StarboundMC.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<Stage1UpgradeMenu>> UPGRADE_MENU =
            MENUS.register("upgrade_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new Stage1UpgradeMenu(id)));
    public static final DeferredHolder<MenuType<?>, MenuType<Stage1ShipConsoleMenu>> SHIP_CONSOLE_MENU =
            MENUS.register("ship_console_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new Stage1ShipConsoleMenu(id)));
    public static final DeferredHolder<MenuType<?>, MenuType<Stage1ShipCrateMenu>> SHIP_CRATE_MENU =
            MENUS.register("ship_crate_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new Stage1ShipCrateMenu(id)));
    public static final DeferredHolder<MenuType<?>, MenuType<Stage1TeleporterMenu>> TELEPORTER_MENU =
            MENUS.register("teleporter_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new Stage1TeleporterMenu(id)));
    public static final DeferredHolder<MenuType<?>, MenuType<Stage1AlloyFurnaceMenu>> ALLOY_FURNACE_MENU =
            MENUS.register("alloy_furnace_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new Stage1AlloyFurnaceMenu(id)));
    public static final DeferredHolder<MenuType<?>, MenuType<Stage1FuelControllerMenu>> FUEL_CONTROLLER_MENU =
            MENUS.register("fuel_controller_menu", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new Stage1FuelControllerMenu(id)));

    private ModMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private abstract static class Stage1Menu extends AbstractContainerMenu {
        protected Stage1Menu(MenuType<?> type, int containerId) {
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

    public static final class Stage1UpgradeMenu extends Stage1Menu {
        private Stage1UpgradeMenu(int containerId) {
            super(UPGRADE_MENU.get(), containerId);
        }
    }

    public static final class Stage1ShipConsoleMenu extends Stage1Menu {
        private Stage1ShipConsoleMenu(int containerId) {
            super(SHIP_CONSOLE_MENU.get(), containerId);
        }
    }

    public static final class Stage1ShipCrateMenu extends Stage1Menu {
        private Stage1ShipCrateMenu(int containerId) {
            super(SHIP_CRATE_MENU.get(), containerId);
        }
    }

    public static final class Stage1TeleporterMenu extends Stage1Menu {
        private Stage1TeleporterMenu(int containerId) {
            super(TELEPORTER_MENU.get(), containerId);
        }
    }

    public static final class Stage1AlloyFurnaceMenu extends Stage1Menu {
        private Stage1AlloyFurnaceMenu(int containerId) {
            super(ALLOY_FURNACE_MENU.get(), containerId);
        }
    }

    public static final class Stage1FuelControllerMenu extends Stage1Menu {
        private Stage1FuelControllerMenu(int containerId) {
            super(FUEL_CONTROLLER_MENU.get(), containerId);
        }
    }
}
