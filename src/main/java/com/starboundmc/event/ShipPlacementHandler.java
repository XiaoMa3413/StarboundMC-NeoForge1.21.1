package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import com.starboundmc.world.ShipTemplatePlacer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Places the custom ship structure template (if any) when the server starts. */
@EventBusSubscriber(modid = StarboundMC.MODID)
public class ShipPlacementHandler
{
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        ShipTemplatePlacer.placeOnServerStart(event.getServer());
    }
}
