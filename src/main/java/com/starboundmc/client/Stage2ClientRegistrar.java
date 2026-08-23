package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.entity.ModEntities;
import com.starboundmc.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only registrations for the stage 2 entity and menu layer. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class Stage2ClientRegistrar {
    private Stage2ClientRegistrar() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.UPGRADE_MENU.get(), UpgradeScreen::new);
        event.register(ModMenus.SHIP_CONSOLE_MENU.get(), Stage2ShipConsoleScreen::new);
        event.register(ModMenus.SHIP_CRATE_MENU.get(), Stage2ShipCrateScreen::new);
        event.register(ModMenus.TELEPORTER_MENU.get(), Stage2TeleporterScreen::new);
        event.register(ModMenus.ALLOY_FURNACE_MENU.get(), Stage2AlloyFurnaceScreen::new);
        event.register(ModMenus.FUEL_CONTROLLER_MENU.get(), Stage2FuelControllerScreen::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEAT.get(), SeatRenderer::new);
    }
}
