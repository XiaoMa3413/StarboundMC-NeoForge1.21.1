package com.starboundmc.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork
{
    private static final String PROTOCOL_VERSION = "4";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("starboundmc", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register()
    {
        int id = 0;
        CHANNEL.registerMessage(id++, UpgradeMatterManipulatorPacket.class,
                UpgradeMatterManipulatorPacket::encode,
                UpgradeMatterManipulatorPacket::decode,
                UpgradeMatterManipulatorPacket::handle);
        CHANNEL.registerMessage(id++, StartWarpPacket.class,
                StartWarpPacket::encode,
                StartWarpPacket::decode,
                StartWarpPacket::handle);
        CHANNEL.registerMessage(id++, SyncStarStatePacket.class,
                SyncStarStatePacket::encode,
                SyncStarStatePacket::decode,
                SyncStarStatePacket::handle);
        CHANNEL.registerMessage(id++, SyncPlanetPacket.class,
                SyncPlanetPacket::encode,
                SyncPlanetPacket::decode,
                SyncPlanetPacket::handle);
        CHANNEL.registerMessage(id++, WarpStartPacket.class,
                WarpStartPacket::encode,
                WarpStartPacket::decode,
                WarpStartPacket::handle);
        CHANNEL.registerMessage(id++, SyncFuelPacket.class,
                SyncFuelPacket::encode,
                SyncFuelPacket::decode,
                SyncFuelPacket::handle);
        CHANNEL.registerMessage(id++, TeleporterListPacket.class,
                TeleporterListPacket::encode,
                TeleporterListPacket::decode,
                TeleporterListPacket::handle);
        CHANNEL.registerMessage(id++, TeleporterUsePacket.class,
                TeleporterUsePacket::encode,
                TeleporterUsePacket::decode,
                TeleporterUsePacket::handle);
        CHANNEL.registerMessage(id++, TeleporterRenamePacket.class,
                TeleporterRenamePacket::encode,
                TeleporterRenamePacket::decode,
                TeleporterRenamePacket::handle);
        CHANNEL.registerMessage(id++, TeleportToShipPacket.class,
                TeleportToShipPacket::encode,
                TeleportToShipPacket::decode,
                TeleportToShipPacket::handle);
        CHANNEL.registerMessage(id++, AddFuelPacket.class,
                AddFuelPacket::encode,
                AddFuelPacket::decode,
                AddFuelPacket::handle);
        CHANNEL.registerMessage(id++, SyncFlightPacket.class,
                SyncFlightPacket::encode,
                SyncFlightPacket::decode,
                SyncFlightPacket::handle);
    }
}
