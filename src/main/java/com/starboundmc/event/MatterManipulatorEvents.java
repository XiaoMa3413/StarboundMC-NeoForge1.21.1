package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import com.starboundmc.item.MatterManipulatorItem;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Matter Manipulator is a laser mining tool, not a pickaxe: left-click
 * mining is disabled entirely. The dig speed is zeroed (survival progress can
 * never advance) and the break event is cancelled (creative instant-break
 * included), so the ONLY way to mine with it is the right-click beam.
 */
@Mod.EventBusSubscriber(modid = StarboundMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MatterManipulatorEvents
{
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event)
    {
        if (event.getEntity().getMainHandItem().getItem() instanceof MatterManipulatorItem)
        {
            event.setNewSpeed(0.0F);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event)
    {
        if (event.getPlayer().getMainHandItem().getItem() instanceof MatterManipulatorItem)
        {
            event.setCanceled(true);
        }
    }
}
