package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.entity.ModEntities;
import com.starboundmc.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = StarboundMC.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModScreenRegistrar
{
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            MenuScreens.register(ModMenus.UPGRADE_MENU.get(), UpgradeScreen::new);
            MenuScreens.register(ModMenus.SHIP_CONSOLE_MENU.get(), ShipConsoleScreen::new);
            MenuScreens.register(ModMenus.SHIP_CRATE_MENU.get(), ShipCrateScreen::new);
            MenuScreens.register(ModMenus.TELEPORTER_MENU.get(), TeleporterScreen::new);
            MenuScreens.register(ModMenus.ALLOY_FURNACE_MENU.get(), AlloyFurnaceScreen::new);
            MenuScreens.register(ModMenus.FUEL_CONTROLLER_MENU.get(), FuelControllerScreen::new);
            EntityRenderers.register(ModEntities.SEAT.get(), SeatRenderer::new);
        });
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event)
    {
        event.register(ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "ship"), new ShipDimensionEffects());
        event.register(ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "frozen"), new FrozenDimensionEffects());
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        // Forge prepends the mod namespace, so only the path goes here.
        event.registerAboveAll("warp_flash", WarpFlashOverlay.FLASH);
    }
}
