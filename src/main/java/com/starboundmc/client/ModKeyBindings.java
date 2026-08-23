package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Client keybindings. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
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
