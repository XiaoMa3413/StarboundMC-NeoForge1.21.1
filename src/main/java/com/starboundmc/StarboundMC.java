package com.starboundmc;

import com.mojang.logging.LogUtils;
import com.starboundmc.block.ModBlockEntities;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.entity.ModEntities;
import com.starboundmc.item.ModItems;
import com.starboundmc.menu.ModMenus;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.sound.ModSounds;
import com.starboundmc.world.ShipDimensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(StarboundMC.MODID)
public class StarboundMC
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "starboundmc";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public StarboundMC(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register blocks, items, menus and networking
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModSounds.register(modEventBus);
        ModNetwork.register();

        // Register the ship dimension data generator
        modEventBus.addListener(ShipDimensions::registerDatagen);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("{} loaded.", MODID);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("{} common setup complete.", MODID);
    }
}
