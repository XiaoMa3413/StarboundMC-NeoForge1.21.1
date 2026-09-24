package com.starboundmc.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only choice of ordinary background star renderer. */
public final class StarfieldClientConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue STELLAR_VIEW_BACKGROUND_STARS = BUILDER
            .comment("Use Stellar View for ordinary ship-dimension background stars when the compatible mod is installed.")
            .define("stellarViewBackgroundStars", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private StarfieldClientConfig() {}
}
