package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.shipai.ClientShipAiTerminalState;
import com.starboundmc.client.shipai.ClientShipStoryState;
import com.starboundmc.client.shipai.ClientNovaBroadcastState;
import com.starboundmc.client.shipai.NovaBroadcastHudLayer;
import com.starboundmc.network.ClientNetworkState;
import com.starboundmc.world.universe.ClientUniverseCatalog;
import net.minecraft.client.Minecraft;
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
        adoptServerUniverse();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetConnectionState();
        // One server's universe must not outlive its session: the next world may
        // ship different datapacks, and stale geometry would place docked ships
        // against planets that are no longer there.
        ClientUniverseCatalog.reset();
    }

    /**
     * Takes the universe the server actually loaded. The synced datapack registry
     * arrives during the configuration phase, so it is already available by the
     * time the player joins.
     */
    private static void adoptServerUniverse() {
        var connection = Minecraft.getInstance().getConnection();
        ClientUniverseCatalog.refreshFrom(connection == null ? null : connection.registryAccess());
    }

    private static void resetConnectionState() {
        ClientNetworkState.resetConnectionState();
        com.starboundmc.client.space.RelayClientState.reset();
        com.starboundmc.client.epp.EppClientState.reset();
        ClientShipAiTerminalState.resetConnectionState();
        ClientShipStoryState.resetConnectionState();
        ClientNovaBroadcastState.resetConnectionState();
        NovaBroadcastHudLayer.INSTANCE.resetConnectionState();
        ClientShipEnvironmentState.resetConnectionState();
        ClientVoxelMachineState.reset();
        ClientPrintQueueState.reset();
        ClientVoxelWalletState.reset();
    }
}
