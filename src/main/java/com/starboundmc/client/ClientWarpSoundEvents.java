package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Stops the warp loop when the player is no longer aboard the ship. */
@Mod.EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;
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
