package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StarboundMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            ShipWarpManager.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player
                && ShipDimensions.SHIP_LEVEL.equals(event.getTo()))
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
        if (event.getEntity() instanceof ServerPlayer player
                && player.level().dimension().equals(ShipDimensions.SHIP_LEVEL))
        {
            ShipWarpManager.syncToPlayer(player);
        }
    }
}
