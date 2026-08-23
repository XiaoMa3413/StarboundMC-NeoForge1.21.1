package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Stops the warp loop when the player is no longer aboard the ship. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public class ClientWarpSoundEvents
{
    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event)
    {
        ClientPlanetState.resetConnectionState();
        WarpSounds.reset();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ClientPlanetState.resetConnectionState();
        WarpSounds.reset();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL))
        {
            WarpSounds.stopLoop();
            return;
        }
        if (ClientPlanetState.isWarping())
        {
            WarpSounds.onWarpTick(ClientPlanetState.warpProgress());
        }
    }
}
