package com.starboundmc.client.shipai;

/** Semantic presentation state shared by the terminal portrait and remote HUD. */
enum NovaPortraitActivity {
    IDLE,
    WARNING,
    SCANNING,
    CONFIRMATION;

    private static final String EMERGENCY_KEY =
            "message.starboundmc.nova.prologue.emergency";
    private static final String LOCATE_TERMINAL_KEY =
            "message.starboundmc.nova.prologue.locate_terminal";
    private static final String CORE_ONLINE_KEY =
            "message.starboundmc.nova.prologue.core_online";
    private static final String MINERAL_SCAN_STARTED_KEY =
            "message.starboundmc.nova.prologue.mineral_scan_started";
    private static final String MINERAL_SCAN_RESULT_KEY =
            "message.starboundmc.nova.prologue.mineral_scan_result";

    static NovaPortraitActivity fromBroadcastKey(String translationKey) {
        if (EMERGENCY_KEY.equals(translationKey) || LOCATE_TERMINAL_KEY.equals(translationKey))
            return WARNING;
        if (MINERAL_SCAN_STARTED_KEY.equals(translationKey))
            return SCANNING;
        if (CORE_ONLINE_KEY.equals(translationKey) || MINERAL_SCAN_RESULT_KEY.equals(translationKey))
            return CONFIRMATION;
        return IDLE;
    }
}
