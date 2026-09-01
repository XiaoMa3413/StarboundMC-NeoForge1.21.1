package com.starboundmc.client.shipai;

import com.starboundmc.StarboundMC;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Advances N.O.V.A. remote HUD transmissions independently from terminal screens. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class ClientNovaBroadcastEvents
{
    private ClientNovaBroadcastEvents()
    {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        ClientNovaBroadcastState.tick();
    }
}
