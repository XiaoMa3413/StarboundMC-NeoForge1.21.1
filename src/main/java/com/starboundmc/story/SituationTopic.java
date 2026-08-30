package com.starboundmc.story;

/** Stable bit assignments for the four mandatory N.O.V.A. situation topics. */
public enum SituationTopic
{
    NOVA_IDENTITY(0, 0x01, "nova_identity"),
    INCIDENT(1, 0x02, "incident"),
    CURRENT_LOCATION(2, 0x04, "current_location"),
    FLIGHT_CAPABILITY(3, 0x08, "flight_capability");

    public static final int REQUIRED_MASK = 0x0F;

    private final int networkId;
    private final int mask;
    private final String translationPath;

    SituationTopic(int networkId, int mask, String translationPath)
    {
        this.networkId = networkId;
        this.mask = mask;
        this.translationPath = translationPath;
    }

    public int networkId()
    {
        return networkId;
    }

    public int mask()
    {
        return mask;
    }

    public String optionTranslationKey()
    {
        return "gui.starboundmc.ship_ai.prologue.topic." + translationPath + ".option";
    }

    public String responseTranslationKey()
    {
        return "gui.starboundmc.ship_ai.prologue.topic." + translationPath + ".response";
    }

    public static SituationTopic fromNetworkId(int networkId)
    {
        for (SituationTopic topic : values())
        {
            if (topic.networkId == networkId)
                return topic;
        }
        throw new IllegalArgumentException("Unknown situation topic id " + networkId);
    }

    public static SituationTopic fromMask(int mask)
    {
        for (SituationTopic topic : values())
        {
            if (topic.mask == mask)
                return topic;
        }
        throw new IllegalArgumentException("Unknown situation topic mask " + mask);
    }
}
