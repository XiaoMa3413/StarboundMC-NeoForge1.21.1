package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entry point for client UI extension points. */
@Mod(value = StarboundMC.MODID, dist = Dist.CLIENT)
public final class StarboundMCClient
{
    public StarboundMCClient(ModContainer modContainer)
    {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
