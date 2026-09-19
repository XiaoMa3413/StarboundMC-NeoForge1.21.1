package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.TeleportToShipPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Handles key presses on the client. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public class ModKeyEvents
{
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        if (ModKeyBindings.equipment != null && ModKeyBindings.equipment.consumeClick()
                && net.minecraft.client.Minecraft.getInstance().player != null
                && net.minecraft.client.Minecraft.getInstance().screen == null)
            ModNetwork.sendToServer(new com.starboundmc.network.OpenEppPacket());
        if (ModKeyBindings.returnToShip != null && ModKeyBindings.returnToShip.consumeClick())
        {
            ModNetwork.sendToServer(new TeleportToShipPacket());
        }
    }
}
