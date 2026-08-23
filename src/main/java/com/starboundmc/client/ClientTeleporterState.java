package com.starboundmc.client;

import java.util.List;

/** Client-side mirror of the teleporter destination list, filled by packets. */
public class ClientTeleporterState
{
    private static List<String[]> destinations = List.of(); // {type, key, label}
    private static String currentName = "";
    private static boolean dirty = false;

    private ClientTeleporterState()
    {
    }

    public static void receive(List<String[]> destinations, String currentName)
    {
        ClientTeleporterState.destinations = destinations;
        ClientTeleporterState.currentName = currentName;
        dirty = true;
    }

    public static List<String[]> getDestinations()
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
