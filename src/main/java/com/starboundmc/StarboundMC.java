package com.starboundmc;

import com.mojang.logging.LogUtils;
import com.starboundmc.block.ModBlockEntities;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.entity.ModEntities;
import com.starboundmc.item.ModItems;
import com.starboundmc.item.ModDataComponents;
import com.starboundmc.menu.ModMenus;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.Stage7ServerPayloadActions;
import com.starboundmc.story.ModAttachments;
import com.starboundmc.world.ShipDimensions;
import com.starboundmc.world.WorldgenRotationWarmup;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(StarboundMC.MODID)
public final class StarboundMC {
    public static final String MODID = "starboundmc";
    private static final Logger LOGGER = LogUtils.getLogger();

    public StarboundMC(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModNetwork::register);
        modEventBus.addListener(ShipDimensions::registerDatagen);

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);

        LOGGER.info("{} NeoForge registry layer loaded (version {}).", MODID,
                modContainer.getModInfo().getVersion());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        WorldgenRotationWarmup.initialize();
        ModNetwork.installServerActions(new Stage7ServerPayloadActions());
        LOGGER.info("{} NeoForge common setup complete.", MODID);
    }
}
