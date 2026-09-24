package com.starboundmc;

import com.mojang.logging.LogUtils;
import com.starboundmc.block.ModBlockEntities;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.entity.ModEntities;
import com.starboundmc.item.ModItems;
import com.starboundmc.item.ModDataComponents;
import com.starboundmc.loot.VoxelLootModifiers;
import com.starboundmc.menu.ModMenus;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.Stage7ServerPayloadActions;
import com.starboundmc.recipe.ModRecipes;
import com.starboundmc.sound.ModSounds;
import com.starboundmc.story.ModAttachments;
import com.starboundmc.world.WorldgenRotationWarmup;
import com.starboundmc.world.universe.ModUniverseRegistries;
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
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,
                com.starboundmc.epp.EppConfig.SPEC);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,
                com.starboundmc.client.StarfieldClientConfig.SPEC);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModNetwork::register);
        // Ship dimension and universe definitions share one datapack-registry
        // provider, so they are registered together in ModDatagen.
        modEventBus.addListener(ModDatagen::register);
        ModUniverseRegistries.register(modEventBus);

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModSounds.register(modEventBus);
        VoxelLootModifiers.register(modEventBus);
        ModRecipes.register(modEventBus);

        LOGGER.info("{} NeoForge registry layer loaded (version {}).", MODID,
                modContainer.getModInfo().getVersion());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        WorldgenRotationWarmup.initialize();
        ModNetwork.installServerActions(new Stage7ServerPayloadActions());
        LOGGER.info("{} NeoForge common setup complete.", MODID);
    }
}
