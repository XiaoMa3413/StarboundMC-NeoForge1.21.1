package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.entity.ModEntities;
import com.starboundmc.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.resources.ResourceLocation;

/** Client-only registrations for the stage 2 entity and menu layer. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class Stage2ClientRegistrar {
    private Stage2ClientRegistrar() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.UPGRADE_MENU.get(), UpgradeScreen::new);
        event.register(ModMenus.SHIP_CONSOLE_MENU.get(), Stage2ShipConsoleScreen::new);
        event.register(ModMenus.SHIP_CRATE_MENU.get(), ShipCrateScreen::new);
        event.register(ModMenus.TELEPORTER_MENU.get(), TeleporterScreen::new);
        event.register(ModMenus.ALLOY_FURNACE_MENU.get(), AlloyFurnaceScreen::new);
        event.register(ModMenus.FUEL_CONTROLLER_MENU.get(), FuelControllerScreen::new);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEAT.get(), SeatRenderer::new);
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "ship"),
                new ShipDimensionEffects());
        event.register(ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "frozen"),
                new FrozenDimensionEffects());
    }
}
