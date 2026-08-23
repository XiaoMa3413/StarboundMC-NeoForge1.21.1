package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import com.starboundmc.world.ShipTemplatePlacer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Places the custom ship structure template (if any) when the server starts. */
@Mod.EventBusSubscriber(modid = StarboundMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShipPlacementHandler
{
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        ShipTemplatePlacer.placeOnServerStart(event.getServer());
    }
}
