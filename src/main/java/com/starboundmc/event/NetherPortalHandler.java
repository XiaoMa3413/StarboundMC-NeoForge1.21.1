package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Vanilla Nether portals are banned everywhere: the Molten Planet replaced the
 * vanilla Nether as the game's hellish destination, so obsidian frames cannot
 * be ignited in any dimension, and no existing portal can drag anyone into the
 * vanilla Nether.
 */
@EventBusSubscriber(modid = StarboundMC.MODID)
public class NetherPortalHandler
{
    @SubscribeEvent
    public static void onPortalSpawn(BlockEvent.PortalSpawnEvent event)
    {
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event)
    {
        if (Level.NETHER.equals(event.getDimension()))
        {
            event.setCanceled(true);
        }
    }
}
