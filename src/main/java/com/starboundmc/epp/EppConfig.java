// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Oxygen units are a resource amount; time estimates depend on the configured consumption rate. */
public final class EppConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue CAPACITY = BUILDER.defineInRange("mk1Capacity", 720, 180, 30000);
    public static final ModConfigSpec.IntValue MK2_CAPACITY = BUILDER.defineInRange("mk2Capacity", 1080, 180, 30000);
    public static final ModConfigSpec.IntValue MK3_CAPACITY = BUILDER.defineInRange("mk3Capacity", 1440, 180, 30000);
    public static final ModConfigSpec.IntValue RELAY_EVA_GAP = BUILDER
            .comment("Preferred free flight gap between ship and relay envelopes. Applied on the next approach.",
                    "Candidates beyond the 160-block local center-distance limit are rejected.")
            .defineInRange("relayEvaGap", 80, 24, 120);
    public static final ModConfigSpec.IntValue COLD_ACCUMULATION = BUILDER.defineInRange("coldExposurePerTierPerSecond", 2, 1, 100);
    public static final ModConfigSpec.IntValue COLD_RECOVERY = BUILDER.defineInRange("coldRecoveryPerSecond", 5, 1, 100);
    public static final ModConfigSpec.IntValue CONSUMPTION = BUILDER.defineInRange("oxygenPerSecond", 1, 1, 100);
    public static final ModConfigSpec.IntValue REFILL = BUILDER.defineInRange("refillPerSecond", 240, 1, 30000);
    public static final ModConfigSpec.IntValue CANISTER = BUILDER.defineInRange("canisterUnits", 180, 1, 30000);
    public static final ModConfigSpec.IntValue GRACE = BUILDER.defineInRange("suffocationGraceSeconds", 10, 1, 120);
    public static final ModConfigSpec SPEC = BUILDER.build();
    public static final int USE_TICKS = 40, FILL_TICKS = 20;
    private EppConfig() { }
}
