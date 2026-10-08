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
                               int readSituationMask, int tutorialMask, int dismissedHintMask,
                               int flagsMask)
{
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final PlayerStoryState DEFAULT =
            new PlayerStoryState(CURRENT_SCHEMA_VERSION, 0L, false, 0, 0, 0, 0);

    public static final Codec<PlayerStoryState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("schema_version")
                    .forGetter(PlayerStoryState::schemaVersion),
            Codec.LONG.fieldOf("revision")
                    .forGetter(PlayerStoryState::revision),
            Codec.BOOL.fieldOf("identity_confirmed")
                    .forGetter(PlayerStoryState::identityConfirmed),
            Codec.INT.fieldOf("read_situation_mask")
                    .forGetter(PlayerStoryState::readSituationMask),
            Codec.INT.fieldOf("tutorial_mask")
                    .forGetter(PlayerStoryState::tutorialMask),
            Codec.INT.fieldOf("dismissed_hint_mask")
                    .forGetter(PlayerStoryState::dismissedHintMask),
            Codec.INT.fieldOf("flags_mask")
                    .forGetter(PlayerStoryState::flagsMask)
    ).apply(instance, PlayerStoryState::new));

    public PlayerStoryState
    {
        if (schemaVersion < CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException("Unsupported pre-release player story schema: " + schemaVersion);
        if (revision < 0 || readSituationMask < 0 || tutorialMask < 0 || dismissedHintMask < 0 || flagsMask < 0
                || (readSituationMask & ~SituationTopic.REQUIRED_MASK) != 0
                || (tutorialMask & ~TutorialTopic.knownMask()) != 0
                || schemaVersion == CURRENT_SCHEMA_VERSION && (flagsMask & ~PlayerStoryFlag.knownMask()) != 0)
            throw new IllegalArgumentException("Invalid player story masks or revision");
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

    public boolean hasFlag(PlayerStoryFlag flag)
    {
        Objects.requireNonNull(flag, "flag");
        return (flagsMask & flag.mask()) != 0;
    }

    public boolean hasSeenBroadcast(PlayerStoryFlag flag)
    {
        return hasFlag(flag);
    }

    /** Returns whether this attachment may be changed by the current build. */
    public boolean isWritable()
    {
        return schemaVersion <= CURRENT_SCHEMA_VERSION;
    }

    public PlayerStoryState confirmIdentity()
    {
        if (!isWritable() || identityConfirmed)
            return this;
        return changed(true, readSituationMask, tutorialMask, dismissedHintMask);
    }

    public PlayerStoryState withReadTopic(SituationTopic topic)
    {
        Objects.requireNonNull(topic, "topic");
        if (!isWritable())
            return this;
        int updatedMask = readSituationMask | topic.mask();
        if (updatedMask == readSituationMask)
            return this;
        return changed(identityConfirmed, updatedMask, tutorialMask, dismissedHintMask);
    }

    public PlayerStoryState withTutorialSeen(TutorialTopic topic)
    {
        Objects.requireNonNull(topic, "topic");
        if (!isWritable())
            return this;
        int updatedMask = tutorialMask | topic.mask();
        if (updatedMask == tutorialMask)
            return this;
        return changed(identityConfirmed, readSituationMask, updatedMask, dismissedHintMask);
    }

    public PlayerStoryState withDismissedHint(int stableHintBit)
    {
        if (!isWritable() || stableHintBit <= 0 || Integer.bitCount(stableHintBit) != 1)
            return this;
        int updatedMask = dismissedHintMask | stableHintBit;
        if (updatedMask == dismissedHintMask)
            return this;
        return changed(identityConfirmed, readSituationMask, tutorialMask, updatedMask);
    }

    public PlayerStoryState withFlag(PlayerStoryFlag flag)
    {
        Objects.requireNonNull(flag, "flag");
        if (!isWritable())
            return this;
        int updatedMask = flagsMask | flag.mask();
        if (updatedMask == flagsMask)
            return this;
        return changed(identityConfirmed, readSituationMask, tutorialMask,
                dismissedHintMask, updatedMask);
    }

    public PlayerStoryState withBroadcastSeen(PlayerStoryFlag flag)
    {
        return withFlag(flag);
    }

    /** Marks the personal prologue as seen without granting any later task evidence. */
    public PlayerStoryState debugCompletePrologue()
    {
        if (!isWritable())
            return this;

        int prologueFlags = PlayerStoryFlag.INITIAL_WAKE_BROADCAST.mask()
                | PlayerStoryFlag.TERMINAL_REMINDER_BROADCAST.mask()
                | PlayerStoryFlag.TERMINAL_CONTACTED.mask()
                | PlayerStoryFlag.CORE_ONLINE_BROADCAST.mask()
                | PlayerStoryFlag.HUD_CORE_LINK_PRESENTED.mask()
                | PlayerStoryFlag.WOOD_ACQUIRED_BROADCAST.mask()
                | PlayerStoryFlag.SURFACE_ARRIVAL_BROADCAST.mask();
        int nextTutorialMask = tutorialMask | TutorialTopic.MATTER_MANIPULATOR.mask();
        int nextFlagsMask = flagsMask | prologueFlags;
        if (identityConfirmed
                && readSituationMask == SituationTopic.REQUIRED_MASK
                && tutorialMask == nextTutorialMask
                && (flagsMask & prologueFlags) == prologueFlags)
            return this;
        return changed(true, SituationTopic.REQUIRED_MASK, nextTutorialMask,
                dismissedHintMask, nextFlagsMask);
    }

    private PlayerStoryState changed(boolean nextIdentityConfirmed, int nextReadMask,
                                     int nextTutorialMask, int nextDismissedHintMask)
    {
        return changed(nextIdentityConfirmed, nextReadMask, nextTutorialMask,
                nextDismissedHintMask, flagsMask);
    }

    private PlayerStoryState changed(boolean nextIdentityConfirmed, int nextReadMask,
                                     int nextTutorialMask, int nextDismissedHintMask,
                                     int nextFlagsMask)
    {
        return new PlayerStoryState(CURRENT_SCHEMA_VERSION, increment(revision), nextIdentityConfirmed,
                nextReadMask, nextTutorialMask, nextDismissedHintMask, nextFlagsMask);
    }

    private static long increment(long value)
    {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
    }
}
