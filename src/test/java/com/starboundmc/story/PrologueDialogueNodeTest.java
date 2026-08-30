package com.starboundmc.story;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrologueDialogueNodeTest
{
    @Test
    void sharedProgressTakesPriorityForLateJoiners()
    {
        assertEquals(PrologueDialogueNode.CURRENT_OBJECTIVE,
                PrologueDialogueNode.derive(true, true, CoreState.ONLINE,
                        SurfaceMissionState.ACTIVE, false));
    }

    @Test
    void offlineRebootAndFirstContactHaveDistinctNodes()
    {
        assertEquals(PrologueDialogueNode.REBOOT_REQUIRED,
                PrologueDialogueNode.derive(true, true, CoreState.OFFLINE,
                        SurfaceMissionState.LOCKED, false));
        assertEquals(PrologueDialogueNode.REBOOTING,
                PrologueDialogueNode.derive(true, true, CoreState.REBOOTING,
                        SurfaceMissionState.LOCKED, false));
        assertEquals(PrologueDialogueNode.FIRST_CONTACT,
                PrologueDialogueNode.derive(true, true, CoreState.ONLINE,
                        SurfaceMissionState.LOCKED, false));
        assertEquals(PrologueDialogueNode.SITUATION_HUB,
                PrologueDialogueNode.derive(true, true, CoreState.ONLINE,
                        SurfaceMissionState.LOCKED, true));
    }

    @Test
    void unsupportedSchemaGetsAnExplicitIncompatibleNode()
    {
        assertEquals(PrologueDialogueNode.INCOMPATIBLE,
                PrologueDialogueNode.derive(false, true, CoreState.OFFLINE,
                        SurfaceMissionState.LOCKED, false));
    }
}
