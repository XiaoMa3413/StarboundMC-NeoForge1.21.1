package com.starboundmc.story;

/** Stable player-local tutorial flags. Add new values with new explicit bits. */
public enum TutorialTopic
{
    MATTER_MANIPULATOR(0x01);

    private final int mask;

    TutorialTopic(int mask)
    {
        this.mask = mask;
    }

    public int mask()
    {
        return mask;
    }

    public static int knownMask()
    {
        int mask = 0;
        for (TutorialTopic topic : values())
            mask |= topic.mask;
        return mask;
    }
}
