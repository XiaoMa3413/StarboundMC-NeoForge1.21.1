package com.starboundmc.warp;

import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.SyncFuelPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Server-authoritative fuel operations available before the warp controller is restored. */
public final class ShipFuelService {
    public static final int MAX_FUEL = ShipStateData.MAX_FUEL;
    public static final int WARP_FUEL_COST = 20;
    public static final int CROSS_SYSTEM_FUEL_COST = 100;

    private ShipFuelService() {
    }

    public static int getFuel(MinecraftServer server) {
        return ShipStateData.get(server).getFuel();
    }

    public static int acceptedAmount(int currentFuel, int requested) {
        int safeCurrent = Math.max(0, Math.min(MAX_FUEL, currentFuel));
        return Math.max(0, Math.min(requested, MAX_FUEL - safeCurrent));
    }

    public static int addFuel(MinecraftServer server, int requested) {
        ShipStateData state = ShipStateData.get(server);
        int added = acceptedAmount(state.getFuel(), requested);
        if (added > 0) {
            state.setFuel(state.getFuel() + added);
        }
        return added;
    }

    public static void syncToPlayer(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            ModNetwork.sendToPlayer(player, new SyncFuelPacket(getFuel(server), MAX_FUEL));
        }
    }

    public static void syncToAll(MinecraftServer server) {
        SyncFuelPacket payload = new SyncFuelPacket(getFuel(server), MAX_FUEL);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetwork.sendToPlayer(player, payload);
        }
    }

    public static int fuelValue(Item item) {
        if (item == Items.COAL) {
            return 10;
        }
        if (item == Items.CHARCOAL) {
            return 5;
        }
        if (item == Items.BLAZE_POWDER) {
            return 20;
        }
        return 0;
    }
}
