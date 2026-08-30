package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import com.starboundmc.story.ShipStoryService;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Keeps server-owned prologue timers progressing even while every UI is closed. */
@EventBusSubscriber(modid = StarboundMC.MODID)
public final class ShipStoryEvents
{
    private ShipStoryEvents()
    {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        ShipStoryService.tick(event.getServer());
    }
}
