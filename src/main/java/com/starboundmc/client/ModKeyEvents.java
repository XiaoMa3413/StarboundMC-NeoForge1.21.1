package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.TeleportToShipPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Handles key presses on the client. */
@Mod.EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModKeyEvents
{
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (ModKeyBindings.returnToShip != null && ModKeyBindings.returnToShip.consumeClick())
        {
            ModNetwork.CHANNEL.sendToServer(new TeleportToShipPacket());
        }
    }
}
