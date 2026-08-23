package com.starboundmc;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Minimal NeoForge entry point used only for the stage 0 build and launch baseline.
 */
@Mod(StarboundMC.MODID)
public final class StarboundMC {
    public static final String MODID = "starboundmc";
    private static final Logger LOGGER = LogUtils.getLogger();

    public StarboundMC(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        LOGGER.info("{} NeoForge bootstrap loaded (version {}).", MODID, modContainer.getModInfo().getVersion());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("{} NeoForge bootstrap common setup complete.", MODID);
    }
}
