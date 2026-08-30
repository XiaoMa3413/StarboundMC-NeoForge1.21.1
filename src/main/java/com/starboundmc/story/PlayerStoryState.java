package com.starboundmc.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;

/**
 * Immutable player-local knowledge and tutorial state. Shared ship progress is
 * deliberately excluded and remains in {@link com.starboundmc.warp.ShipStateData}.
 * The schema field supports forward migrations; opening newer player data in
 * an older mod build is not a supported, lossless downgrade path.
 */
public record PlayerStoryState(int schemaVersion, long revision, boolean identityConfirmed,
                               int readSituationMask, int tutorialMask, int dismissedHintMask)
{
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final PlayerStoryState DEFAULT =
            new PlayerStoryState(CURRENT_SCHEMA_VERSION, 0L, false, 0, 0, 0);

    public static final Codec<PlayerStoryState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION)
                    .forGetter(PlayerStoryState::schemaVersion),
            Codec.LONG.optionalFieldOf("revision", 0L)
                    .forGetter(PlayerStoryState::revision),
            Codec.BOOL.optionalFieldOf("identity_confirmed", false)
                    .forGetter(PlayerStoryState::identityConfirmed),
            Codec.INT.optionalFieldOf("read_situation_mask", 0)
                    .forGetter(PlayerStoryState::readSituationMask),
            Codec.INT.optionalFieldOf("tutorial_mask", 0)
                    .forGetter(PlayerStoryState::tutorialMask),
            Codec.INT.optionalFieldOf("dismissed_hint_mask", 0)
                    .forGetter(PlayerStoryState::dismissedHintMask)
    ).apply(instance, PlayerStoryState::new));

    public PlayerStoryState
    {
        if (schemaVersion <= 0)
            schemaVersion = CURRENT_SCHEMA_VERSION;
        revision = Math.max(0L, revision);
        readSituationMask = readSituationMask < 0
                ? 0 : readSituationMask & SituationTopic.REQUIRED_MASK;
        tutorialMask = tutorialMask < 0
                ? 0 : tutorialMask & TutorialTopic.knownMask();
        dismissedHintMask = Math.max(0, dismissedHintMask);
    }

    public boolean hasRead(SituationTopic topic)
    {
        Objects.requireNonNull(topic, "topic");
        return (readSituationMask & topic.mask()) != 0;
    }

    public boolean hasReadAllRequiredTopics()
    {
        return (readSituationMask & SituationTopic.REQUIRED_MASK) == SituationTopic.REQUIRED_MASK;
    }

    public boolean hasSeenTutorial(TutorialTopic topic)
    {
        Objects.requireNonNull(topic, "topic");
        return (tutorialMask & topic.mask()) != 0;
    }

    public boolean hasDismissedHint(int stableHintBit)
    {
        return stableHintBit > 0 && (dismissedHintMask & stableHintBit) != 0;
    }

    public PlayerStoryState confirmIdentity()
    {
        if (identityConfirmed)
            return this;
        return changed(true, readSituationMask, tutorialMask, dismissedHintMask);
    }

    public PlayerStoryState withReadTopic(SituationTopic topic)
    {
        Objects.requireNonNull(topic, "topic");
        int updatedMask = readSituationMask | topic.mask();
        if (updatedMask == readSituationMask)
            return this;
        return changed(identityConfirmed, updatedMask, tutorialMask, dismissedHintMask);
    }

    public PlayerStoryState withTutorialSeen(TutorialTopic topic)
    {
        Objects.requireNonNull(topic, "topic");
        int updatedMask = tutorialMask | topic.mask();
        if (updatedMask == tutorialMask)
            return this;
        return changed(identityConfirmed, readSituationMask, updatedMask, dismissedHintMask);
    }

    public PlayerStoryState withDismissedHint(int stableHintBit)
    {
        if (stableHintBit <= 0 || Integer.bitCount(stableHintBit) != 1)
            return this;
        int updatedMask = dismissedHintMask | stableHintBit;
        if (updatedMask == dismissedHintMask)
            return this;
        return changed(identityConfirmed, readSituationMask, tutorialMask, updatedMask);
    }

    private PlayerStoryState changed(boolean nextIdentityConfirmed, int nextReadMask,
                                     int nextTutorialMask, int nextDismissedHintMask)
    {
        return new PlayerStoryState(CURRENT_SCHEMA_VERSION, increment(revision), nextIdentityConfirmed,
                nextReadMask, nextTutorialMask, nextDismissedHintMask);
    }

    private static long increment(long value)
    {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
    }
}
