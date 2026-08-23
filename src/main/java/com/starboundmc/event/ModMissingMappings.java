package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;

/** Converts the old pre-unification teleporter blocks/items into the new unified teleporter. */
@Mod.EventBusSubscriber(modid = StarboundMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModMissingMappings
{
    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event)
    {
        for (MissingMappingsEvent.Mapping<Block> mapping : event.getMappings(Registries.BLOCK, StarboundMC.MODID))
        {
            String path = mapping.getKey().getPath();
            if (path.equals("ship_teleporter_block") || path.equals("ship_return_portal"))
            {
                mapping.remap(ModBlocks.TELEPORTER.get());
            }
        }
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(Registries.ITEM, StarboundMC.MODID))
        {
            String path = mapping.getKey().getPath();
            if (path.equals("ship_teleporter") || path.equals("ship_teleporter_block") || path.equals("ship_return_portal"))
            {
                mapping.remap(ModItems.TELEPORTER_ITEM.get());
            }
        }
    }
}
