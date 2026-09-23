// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud;

import com.starboundmc.story.CoreState;

/** Client-only presentation state for the personal visual-interface bootstrap. */
public final class HudBootController {
    public static final HudBootController INSTANCE = new HudBootController();
    public static final String INITIAL_WAKE_KEY =
            "message.starboundmc.nova.prologue.emergency";
    public static final String TERMINAL_REMINDER_KEY =
            "message.starboundmc.nova.prologue.locate_terminal";

    static final int STARTING_TICKS = 96;
    static final int VISOR_FADE_TICKS = 30;
    static final int NAVIGATION_START_TICK = 32;
    static final int CORE_CHECK_TICK = 58;
    static final int AR_START_TICK = 72;
    static final int SAFE_MODE_HOLD_TICKS = 30;
    static final int SAFE_MODE_FADE_TICKS = 16;
    static final int CORE_LINK_TICKS = 50;
    static final int PERSONAL_LINK_TICKS = 40;
    static final int ONLINE_HOLD_TICKS = 25;
    static final int ONLINE_FADE_TICKS = 20;
    static final int ONLINE_STATUS_TICKS = ONLINE_HOLD_TICKS + ONLINE_FADE_TICKS;

    private State state = State.DORMANT;
    private CoreState serverCore = CoreState.OFFLINE;
    private int stateTicks;
    private int safeModeTicks;
    private int onlineTicks = ONLINE_STATUS_TICKS;
    private boolean terminalGuidance;
    private boolean terminalContacted;
    private boolean personalLink;
    private boolean linkReceiptPending;
    private boolean linkReceiptSent;
    private boolean paused;

    HudBootController() { }

    /** Updates core truth even while presentation is paused; never infers ONLINE from a timer. */
    public void applyServerState(CoreState core, boolean wakePresented,
                                 boolean terminalContacted, boolean coreLinkPresented) {
        serverCore = core;
        this.terminalContacted = terminalContacted;
        terminalGuidance = core == CoreState.OFFLINE && wakePresented && !terminalContacted;
        if (core == CoreState.ONLINE) {
            if (coreLinkPresented) {
                enter(State.ONLINE);
                onlineTicks = ONLINE_STATUS_TICKS;
                linkReceiptSent = true;
                linkReceiptPending = false;
            } else if (state == State.LINKING && !personalLink) {
                finishLink();
            } else if (state != State.ONLINE && state != State.LINKING) {
                beginLink(true);
            }
        } else if (core == CoreState.REBOOTING) {
            if (state != State.LINKING)
                beginLink(false);
        } else if (!wakePresented && !terminalContacted && state != State.STARTING) {
            enter(State.DORMANT);
        } else if ((state != State.STARTING || terminalContacted) && state != State.SAFE_MODE) {
            enter(State.SAFE_MODE);
            safeModeTicks = SAFE_MODE_HOLD_TICKS + SAFE_MODE_FADE_TICKS;
        }
    }

    /** Uses the existing one-shot prologue broadcasts as presentation triggers. */
    public void onNovaBroadcast(String translationKey) {
        if (serverCore != CoreState.OFFLINE || terminalContacted)
            return;
        if (INITIAL_WAKE_KEY.equals(translationKey)) {
            terminalGuidance = true;
            if (state == State.DORMANT)
                enter(State.STARTING);
        } else if (TERMINAL_REMINDER_KEY.equals(translationKey)) {
            terminalGuidance = true;
            if (state == State.DORMANT)
                enter(State.SAFE_MODE);
        }
    }

    /** Advances only while gameplay presentation is active. */
    public void tick(boolean paused) {
        this.paused = paused;
        if (paused)
            return;
        if (state == State.STARTING) {
            stateTicks++;
            if (stateTicks >= STARTING_TICKS) {
                enter(State.SAFE_MODE);
                safeModeTicks = 0;
            }
        } else if (state == State.SAFE_MODE
                && safeModeTicks < SAFE_MODE_HOLD_TICKS + SAFE_MODE_FADE_TICKS) {
            safeModeTicks++;
        } else if (state == State.LINKING) {
            stateTicks = Math.min(stateTicks + 1, CORE_LINK_TICKS);
            if (personalLink && serverCore == CoreState.ONLINE && stateTicks >= PERSONAL_LINK_TICKS)
                finishLink();
        } else if (state == State.ONLINE && onlineTicks < ONLINE_STATUS_TICKS) {
            onlineTicks++;
            if (onlineTicks == ONLINE_STATUS_TICKS && !linkReceiptSent)
                linkReceiptPending = true;
        }
    }

    /** Consumed once by the client network adapter after the entire presentation is visible. */
    public boolean consumeCoreLinkReceipt() {
        if (!linkReceiptPending)
            return false;
        linkReceiptPending = false;
        linkReceiptSent = true;
        return true;
    }

    public boolean defersCommunication() {
        return state == State.STARTING || state == State.LINKING
                || state == State.SAFE_MODE && safeModeTicks < SAFE_MODE_HOLD_TICKS + SAFE_MODE_FADE_TICKS
                || state == State.ONLINE && onlineTicks < ONLINE_STATUS_TICKS;
    }

    private void beginLink(boolean personal) {
        personalLink = personal;
        enter(State.LINKING);
    }

    private void finishLink() {
        enter(State.ONLINE);
        onlineTicks = 0;
    }

    public State state() {
        return state;
    }

    public float visorOpacity() {
        if (state == State.DORMANT)
            return 0F;
        if (state != State.STARTING)
            return 1F;
        return smoothStep(Math.clamp((float) stateTicks / VISOR_FADE_TICKS, 0F, 1F));
    }

    public Presentation presentation(float partialTick) {
        float partial = paused ? 0F : Math.clamp(partialTick, 0F, 1F);
        float tick = stateTicks + partial;
        Cue cue;
        float opacity;
        switch (state) {
            case STARTING -> {
                cue = tick < NAVIGATION_START_TICK ? Cue.VISUAL_RESTORE
                        : tick < CORE_CHECK_TICK ? Cue.LOCAL_NAVIGATION : Cue.CORE_UNAVAILABLE;
                float start = cue == Cue.VISUAL_RESTORE ? 0
                        : cue == Cue.LOCAL_NAVIGATION ? NAVIGATION_START_TICK : CORE_CHECK_TICK;
                float end = cue == Cue.VISUAL_RESTORE ? NAVIGATION_START_TICK
                        : cue == Cue.LOCAL_NAVIGATION ? CORE_CHECK_TICK : STARTING_TICKS;
                // Sequential fades: two different capability messages never overlap.
                opacity = smoothStep(Math.clamp((tick - start) / 6F, 0F, 1F))
                        * smoothStep(Math.clamp((end - tick) / 5F, 0F, 1F));
            }
            case SAFE_MODE -> {
                cue = Cue.SAFE_MODE;
                opacity = 1F - smoothStep(Math.clamp((safeModeTicks + partial - SAFE_MODE_HOLD_TICKS)
                        / SAFE_MODE_FADE_TICKS, 0F, 1F));
            }
            case LINKING -> {
                cue = personalLink ? tick < 16 ? Cue.PERSONAL_INITIALIZATION : Cue.CORE_SYNCHRONIZING
                        : Cue.CORE_CONNECTING;
                opacity = smoothStep(Math.clamp(tick / 6F, 0F, 1F));
                if (personalLink) opacity *= tick < 16
                        ? smoothStep(Math.clamp((16 - tick) / 4F, 0F, 1F))
                        : smoothStep(Math.clamp((tick - 16) / 4F, 0F, 1F));
            }
            case ONLINE -> {
                cue = Cue.ONLINE;
                opacity = 1F - smoothStep(Math.clamp((onlineTicks + partial - ONLINE_HOLD_TICKS)
                        / ONLINE_FADE_TICKS, 0F, 1F));
            }
            default -> { cue = Cue.NONE; opacity = 0F; }
        }
        float calibration = state == State.STARTING ? Math.clamp(tick / NAVIGATION_START_TICK, 0F, 1F) : 1F;
        return new Presentation(state, visorOpacity(), opacity, cue, calibration, personalLink);
    }

    public boolean terminalGuidanceActive() {
        return terminalGuidance && serverCore == CoreState.OFFLINE
                && state != State.DORMANT && state != State.ONLINE;
    }

    public boolean localNavigationActive() {
        return state == State.LINKING || state == State.ONLINE && onlineTicks < ONLINE_STATUS_TICKS
                || terminalGuidanceActive() && (state == State.SAFE_MODE
                || state == State.STARTING && stateTicks >= NAVIGATION_START_TICK);
    }

    /** Presentation gate only: reference acquisition precedes the first world lock. */
    public boolean worldArActive() {
        return state != State.DORMANT
                && (state != State.STARTING || stateTicks >= AR_START_TICK);
    }

    public void reset() {
        state = State.DORMANT;
        serverCore = CoreState.OFFLINE;
        stateTicks = 0;
        safeModeTicks = 0;
        onlineTicks = ONLINE_STATUS_TICKS;
        terminalGuidance = false;
        terminalContacted = false;
        personalLink = false;
        linkReceiptPending = false;
        linkReceiptSent = false;
        paused = false;
    }

    private void enter(State next) {
        if (state == next)
            return;
        state = next;
        stateTicks = 0;
        safeModeTicks = 0;
    }

    private static float smoothStep(float value) {
        return value * value * (3F - 2F * value);
    }

    public enum State {
        DORMANT,
        STARTING,
        SAFE_MODE,
        LINKING,
        ONLINE
    }

    public enum Cue {
        NONE, VISUAL_RESTORE, LOCAL_NAVIGATION, CORE_UNAVAILABLE, SAFE_MODE,
        PERSONAL_INITIALIZATION, CORE_CONNECTING, CORE_SYNCHRONIZING, ONLINE
    }

    public record Presentation(State state, float visorOpacity, float statusOpacity,
                               Cue cue, float calibrationProgress, boolean personalLink) { }
}
