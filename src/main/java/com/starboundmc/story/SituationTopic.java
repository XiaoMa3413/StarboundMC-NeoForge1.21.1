package com.starboundmc.story;

/** Stable bit assignments for the four mandatory N.O.V.A. situation topics. */
public enum SituationTopic
{
    NOVA_IDENTITY(0x01),
    INCIDENT(0x02),
    CURRENT_LOCATION(0x04),
    FLIGHT_CAPABILITY(0x08);

    public static final int REQUIRED_MASK = 0x0F;

    private final int mask;

    SituationTopic(int mask)
    {
        this.mask = mask;
    }

    public int mask()
    {
        return mask;
    }
}
