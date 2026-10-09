package com.starboundmc.story;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrologueDialogueNodeTest
{
    @Test
    void sharedProgressTakesPriorityForLateJoiners()
    {
        assertEquals(PrologueDialogueNode.CURRENT_OBJECTIVE,
                PrologueDialogueNode.derive(CoreState.ONLINE,
                        SurfaceMissionState.ACTIVE, false));
    }

    @Test
    void offlineRebootAndFirstContactHaveDistinctNodes()
    {
        assertEquals(PrologueDialogueNode.REBOOT_REQUIRED,
                PrologueDialogueNode.derive(CoreState.OFFLINE,
                        SurfaceMissionState.LOCKED, false));
        assertEquals(PrologueDialogueNode.REBOOTING,
                PrologueDialogueNode.derive(CoreState.REBOOTING,
                        SurfaceMissionState.LOCKED, false));
        assertEquals(PrologueDialogueNode.FIRST_CONTACT,
                PrologueDialogueNode.derive(CoreState.ONLINE,
                        SurfaceMissionState.LOCKED, false));
        assertEquals(PrologueDialogueNode.SITUATION_HUB,
                PrologueDialogueNode.derive(CoreState.ONLINE,
                        SurfaceMissionState.LOCKED, true));
    }

}
