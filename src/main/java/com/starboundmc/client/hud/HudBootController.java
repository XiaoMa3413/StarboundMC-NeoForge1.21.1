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

    static final int STARTING_TICKS = 130;
    static final int VISOR_FADE_TICKS = 30;
    static final int PROMPT_FADE_IN_TICKS = 10;
    static final int PROMPT_HOLD_END_TICK = 52;
    static final int PROMPT_END_TICK = 70;
    static final int STATUS_START_TICK = 24;
    static final int STATUS_FADE_IN_TICKS = 8;
    static final int STATUS_STEP_TICKS = 15;
    static final int STATUS_SCROLL_TICKS = 6;
    static final int STATUS_STEP_COUNT = 6;
    static final int SAFE_MODE_HOLD_TICKS = 30;
    static final int SAFE_MODE_FADE_TICKS = 30;
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
        } else if (state != State.STARTING || terminalContacted) {
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
        if (paused)
            partialTick = 0F;
        float tick = Math.max(0F, stateTicks + Math.clamp(partialTick, 0F, 1F));
        if (state == State.LINKING || state == State.ONLINE) {
            boolean linking = state == State.LINKING;
            int lineCount = personalLink || !linking ? 4 : 3;
            int lines = linking ? Math.min(lineCount, (int) (tick / 8F) + 1) : lineCount;
            float opacity = linking ? smoothStep(Math.clamp(tick / 8F, 0F, 1F))
                    : 1F - smoothStep(Math.clamp(
                            (onlineTicks - ONLINE_HOLD_TICKS + Math.clamp(partialTick, 0F, 1F))
                                    / ONLINE_FADE_TICKS, 0F, 1F));
            float progress = linking ? Math.clamp(tick
                    / (personalLink ? PERSONAL_LINK_TICKS : CORE_LINK_TICKS), 0F, .95F) : 1F;
            return new Presentation(state, visorOpacity(), 0F, opacity, lines, 0F,
                    progress, false, personalLink);
        }
        float statusTick = tick - STATUS_START_TICK;
        int lines = state == State.STARTING && statusTick >= 0F
                ? Math.min(STATUS_STEP_COUNT,
                        (int) (statusTick / STATUS_STEP_TICKS) + 1)
                : state == State.SAFE_MODE ? STATUS_STEP_COUNT : 0;
        float scrollRows = state == State.SAFE_MODE
                ? Math.max(0, lines - 4) : scrollRows(tick, lines);
        float promptOpacity = state == State.STARTING ? promptOpacity(tick) : 0F;
        float opacity;
        if (state == State.STARTING) {
            opacity = smoothStep(Math.clamp(statusTick / STATUS_FADE_IN_TICKS, 0F, 1F));
        } else if (state == State.SAFE_MODE) {
            float fade = (safeModeTicks - SAFE_MODE_HOLD_TICKS + Math.clamp(partialTick, 0F, 1F))
                    / SAFE_MODE_FADE_TICKS;
            opacity = 1F - smoothStep(Math.clamp(fade, 0F, 1F));
        } else {
            opacity = 0F;
        }
        float scan = state == State.STARTING
                ? Math.clamp(statusTick / (STARTING_TICKS - STATUS_START_TICK), 0F, 1F) : 1F;
        return new Presentation(state, visorOpacity(), promptOpacity, opacity, lines,
                scrollRows, scan, terminalGuidanceActive(), false);
    }

    public boolean terminalGuidanceActive() {
        return terminalGuidance && serverCore == CoreState.OFFLINE
                && state != State.DORMANT && state != State.ONLINE;
    }

    public boolean localNavigationActive() {
        return state == State.SAFE_MODE && terminalGuidanceActive();
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

    private static float promptOpacity(float tick) {
        if (tick < PROMPT_FADE_IN_TICKS)
            return smoothStep(Math.clamp(tick / PROMPT_FADE_IN_TICKS, 0F, 1F));
        if (tick <= PROMPT_HOLD_END_TICK)
            return 1F;
        return 1F - smoothStep(Math.clamp(
                (tick - PROMPT_HOLD_END_TICK)
                        / (PROMPT_END_TICK - PROMPT_HOLD_END_TICK), 0F, 1F));
    }

    private static float scrollRows(float tick, int lines) {
        if (lines <= 4)
            return 0F;
        int completedScrolls = lines - 5;
        float revealTick = STATUS_START_TICK + (lines - 1) * STATUS_STEP_TICKS;
        float activeScroll = smoothStep(Math.clamp(
                (tick - revealTick) / STATUS_SCROLL_TICKS, 0F, 1F));
        return completedScrolls + activeScroll;
    }

    public enum State {
        DORMANT,
        STARTING,
        SAFE_MODE,
        LINKING,
        ONLINE
    }

    public record Presentation(State state, float visorOpacity, float promptOpacity,
                               float statusOpacity, int revealedLines, float scrollRows,
                               float scanProgress,
                               boolean terminalGuidance, boolean personalLink) { }
}
