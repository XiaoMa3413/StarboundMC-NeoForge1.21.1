package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Client keybindings. */
@Mod.EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyBindings
{
    public static KeyMapping returnToShip;

    /** Custom category shown in the Controls screen. */
    public static final String CATEGORY = "key.categories.starboundmc";

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        returnToShip = new KeyMapping("key.starboundmc.return_to_ship",
                GLFW.GLFW_KEY_H, CATEGORY);
        event.register(returnToShip);
    }
}
