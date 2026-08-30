package com.starboundmc.story;

import java.util.Objects;

/**
 * View-level dialogue nodes derived from authoritative semantic state. Topic
 * responses and the landing briefing are transient client presentation nodes,
 * never a third persisted progression source.
 */
public enum PrologueDialogueNode
{
    INCOMPATIBLE,
    REBOOT_REQUIRED,
    REBOOTING,
    FIRST_CONTACT,
    SITUATION_HUB,
    TOPIC_RESPONSE,
    LANDING_BRIEFING,
    CURRENT_OBJECTIVE;

    public static PrologueDialogueNode derive(boolean sharedSchemaSupported,
                                              boolean playerSchemaSupported,
                                              CoreState core,
                                              SurfaceMissionState mission,
                                              boolean identityConfirmed)
    {
        Objects.requireNonNull(core, "core");
        Objects.requireNonNull(mission, "mission");
        if (!sharedSchemaSupported || !playerSchemaSupported)
            return INCOMPATIBLE;
        if (core == CoreState.OFFLINE)
            return REBOOT_REQUIRED;
        if (core == CoreState.REBOOTING)
            return REBOOTING;
        // Shared progress always wins: late joiners must never roll the world back.
        if (mission != SurfaceMissionState.LOCKED)
            return CURRENT_OBJECTIVE;
        return identityConfirmed ? SITUATION_HUB : FIRST_CONTACT;
    }
}
