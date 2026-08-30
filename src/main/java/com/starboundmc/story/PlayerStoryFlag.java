package com.starboundmc.story;

/**
 * Persistent per-player milestones used by one-shot N.O.V.A. cues.
 *
 * <p>The flags are deliberately separate from the shared ship state: two
 * players may receive the same introduction at different times without
 * changing the ship's progression for one another.</p>
 */
public enum PlayerStoryFlag
{
    INITIAL_WAKE_BROADCAST(0x01),
    TERMINAL_REMINDER_BROADCAST(0x02),
    TERMINAL_CONTACTED(0x04),
    CORE_ONLINE_BROADCAST(0x08),
    WOOD_ACQUIRED_BROADCAST(0x10),
    SURFACE_ARRIVAL_BROADCAST(0x20);

    private final int mask;

    PlayerStoryFlag(int mask)
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
        for (PlayerStoryFlag flag : values())
            mask |= flag.mask;
        return mask;
    }
}
