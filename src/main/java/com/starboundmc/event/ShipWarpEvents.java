package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.world.Stage6TravelService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = StarboundMC.MODID)
public class ShipWarpEvents
{
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        ShipWarpManager.init(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        ShipWarpManager.reset();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        ShipWarpManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player
                && Stage6TravelService.SHIP_LEVEL.equals(event.getTo()))
        {
            ShipWarpManager.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        // Players who spawn directly in the ship dimension never fire a
        // dimension change, so sync the shared ship state (planet/fuel/star
        // map) here — otherwise the client's "current planet" stays at its
        // default and the first warp to the actual docked planet is silently
        // rejected by the server.
        if (event.getEntity() instanceof ServerPlayer player)
            ShipWarpManager.syncToPlayer(player);
    }
}
