package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Vanilla Nether portals are banned everywhere: the Molten Planet replaced the
 * vanilla Nether as the game's hellish destination, so obsidian frames cannot
 * be ignited in any dimension, and no existing portal can drag anyone into the
 * vanilla Nether.
 */
@Mod.EventBusSubscriber(modid = StarboundMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
