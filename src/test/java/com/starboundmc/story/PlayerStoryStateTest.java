package com.starboundmc.story;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStoryStateTest
{
    @Test
    void allFourTopicsAreRequiredInAnyOrder()
    {
        PlayerStoryState state = PlayerStoryState.DEFAULT
                .withReadTopic(SituationTopic.CURRENT_LOCATION)
                .withReadTopic(SituationTopic.NOVA_IDENTITY)
                .withReadTopic(SituationTopic.FLIGHT_CAPABILITY);

        assertFalse(state.hasReadAllRequiredTopics());
        state = state.withReadTopic(SituationTopic.INCIDENT);
        assertTrue(state.hasReadAllRequiredTopics());
        assertEquals(4L, state.revision());
        assertSame(state, state.withReadTopic(SituationTopic.INCIDENT));
    }

    @Test
    void personalFlagsRoundTripThroughAttachmentCodec()
    {
        PlayerStoryState expected = PlayerStoryState.DEFAULT
                .confirmIdentity()
                .withReadTopic(SituationTopic.NOVA_IDENTITY)
                .withTutorialSeen(TutorialTopic.MATTER_MANIPULATOR)
                .withFlag(PlayerStoryFlag.INITIAL_WAKE_BROADCAST)
                .withFlag(PlayerStoryFlag.TERMINAL_CONTACTED)
                .withFlag(PlayerStoryFlag.SURFACE_ARRIVAL_BROADCAST)
                .withFlag(PlayerStoryFlag.WOOD_ACQUIRED_BROADCAST)
                .withDismissedHint(0x01);

        Tag encoded = PlayerStoryState.CODEC.encodeStart(NbtOps.INSTANCE, expected).getOrThrow();
        PlayerStoryState restored = PlayerStoryState.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

        assertEquals(expected, restored);
        assertTrue(restored.identityConfirmed());
        assertTrue(restored.hasSeenTutorial(TutorialTopic.MATTER_MANIPULATOR));
        assertTrue(restored.hasDismissedHint(0x01));
        assertTrue(restored.hasSeenBroadcast(PlayerStoryFlag.INITIAL_WAKE_BROADCAST));
        assertTrue(restored.hasFlag(PlayerStoryFlag.TERMINAL_CONTACTED));
        assertTrue(restored.hasFlag(PlayerStoryFlag.SURFACE_ARRIVAL_BROADCAST));
        assertTrue(restored.hasFlag(PlayerStoryFlag.WOOD_ACQUIRED_BROADCAST));
        assertEquals(8L, restored.revision());
    }

    @Test
    void malformedNegativeMasksFailClosed()
    {
        PlayerStoryState malformed = new PlayerStoryState(0, -12L, false, -1, -1, -1, -1);

        assertEquals(PlayerStoryState.CURRENT_SCHEMA_VERSION, malformed.schemaVersion());
        assertEquals(0L, malformed.revision());
        assertEquals(0, malformed.readSituationMask());
        assertEquals(0, malformed.tutorialMask());
        assertEquals(0, malformed.dismissedHintMask());
        assertEquals(0, malformed.flagsMask());
        assertFalse(malformed.hasReadAllRequiredTopics());
    }

    @Test
    void futureSchemaCannotBeDowngradedByLocalFlagHelpers()
    {
        PlayerStoryState future = new PlayerStoryState(
                PlayerStoryState.CURRENT_SCHEMA_VERSION + 1, 4L, false, 0, 0, 0,
                PlayerStoryFlag.INITIAL_WAKE_BROADCAST.mask());

        assertSame(future, future.withFlag(PlayerStoryFlag.TERMINAL_CONTACTED));
        assertSame(future, future.withTutorialSeen(TutorialTopic.MATTER_MANIPULATOR));
    }
}
