package com.starboundmc.story;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NovaTaskServiceTest
{
    @Test
    void storyMilestonesDoNotRepeatAsTaskCompletionBroadcasts()
    {
        assertFalse(NovaTaskService.shouldBroadcastTaskCompletion(NovaTask.SURFACE));
        assertFalse(NovaTaskService.shouldBroadcastTaskCompletion(NovaTask.REPAIR));
        assertTrue(NovaTaskService.shouldBroadcastTaskCompletion(NovaTask.EXPLORATION));
        assertTrue(NovaTaskService.shouldBroadcastTaskCompletion(NovaTask.LIFE_SUPPORT));
        assertTrue(NovaTaskService.shouldBroadcastTaskCompletion(NovaTask.LUNAR_SORTIE));
    }
}
