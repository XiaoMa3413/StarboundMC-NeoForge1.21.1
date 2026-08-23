package com.starboundmc.client;

import com.starboundmc.network.TeleporterListPacket;
import java.util.List;

/** Client-side mirror of the teleporter destination list, filled by packets. */
public class ClientTeleporterState
{
    private static List<TeleporterListPacket.Entry> destinations = List.of();
    private static String currentName = "";
    private static boolean dirty = false;

    private ClientTeleporterState()
    {
    }

    public static void receive(List<TeleporterListPacket.Entry> destinations, String currentName)
    {
        ClientTeleporterState.destinations = List.copyOf(destinations);
        ClientTeleporterState.currentName = currentName;
        dirty = true;
    }

    public static List<TeleporterListPacket.Entry> getDestinations()
    {
        return destinations;
    }

    public static String getCurrentName()
    {
        return currentName;
    }

    public static boolean consumeDirty()
    {
        boolean wasDirty = dirty;
        dirty = false;
        return wasDirty;
    }
}
