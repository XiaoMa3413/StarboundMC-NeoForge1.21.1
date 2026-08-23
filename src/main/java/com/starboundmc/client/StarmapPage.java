package com.starboundmc.client;

/** Explicit navigation level of the ship-console star map. */
public enum StarmapPage
{
    GALAXY,
    SYSTEM,
    BODY_FOCUS;

    public boolean showsSystemContext()
    {
        return this == SYSTEM || this == BODY_FOCUS;
    }
}
