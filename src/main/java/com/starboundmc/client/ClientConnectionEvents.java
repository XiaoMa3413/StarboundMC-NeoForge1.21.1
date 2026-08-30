package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.shipai.ClientShipAiTerminalState;
import com.starboundmc.client.shipai.ClientShipStoryState;
import com.starboundmc.network.ClientNetworkState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Clears revisioned client mirrors at each network-session boundary. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class ClientConnectionEvents {
    private ClientConnectionEvents() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        resetConnectionState();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetConnectionState();
    }

    private static void resetConnectionState() {
        ClientNetworkState.resetConnectionState();
        ClientShipAiTerminalState.resetConnectionState();
        ClientShipStoryState.resetConnectionState();
    }
}
