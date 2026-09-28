package com.starboundmc.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only quality preset and feature toggles for space rendering. */
public final class StarfieldClientConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<SpaceVisualQuality> SPACE_VISUAL_QUALITY = BUILDER
            .comment("Space visual quality preset. Performance disables clouds and Stellar View background stars.",
                    "Balanced disables cloud shadows; High and Ultra enable them. Custom uses the toggles below.")
            .translation("starboundmc.config.space_visual_quality")
            .defineEnum("spaceVisualQuality", SpaceVisualQuality.BALANCED);

    public static final ModConfigSpec.BooleanValue CLOUDS_ENABLED = BUILDER
            .comment("Enable cloud shells when spaceVisualQuality is Custom.")
            .translation("starboundmc.config.clouds_enabled")
            .define("cloudsEnabled", true);

    public static final ModConfigSpec.BooleanValue CLOUD_SHADOWS_ENABLED = BUILDER
            .comment("Enable projected cloud shadows when spaceVisualQuality is Custom.")
            .translation("starboundmc.config.cloud_shadows_enabled")
            .define("cloudShadowsEnabled", true);

    public static final ModConfigSpec.BooleanValue ATMOSPHERE_ENABLED = BUILDER
            .comment("Enable atmosphere shells when spaceVisualQuality is Custom.")
            .translation("starboundmc.config.atmosphere_enabled")
            .define("atmosphereEnabled", true);

    public static final ModConfigSpec.BooleanValue STELLAR_VIEW_BACKGROUND_STARS = BUILDER
            .comment("Use Stellar View for ordinary ship-dimension background stars when the compatible mod is installed.",
                    "This toggle is used when spaceVisualQuality is Custom.")
            .translation("starboundmc.config.stellar_view_background_stars")
            .define("stellarViewBackgroundStars", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean cloudsEnabled()
    {
        return SPACE_VISUAL_QUALITY.get().cloudsEnabled(CLOUDS_ENABLED.get());
    }

    public static boolean cloudShadowsEnabled()
    {
        return SPACE_VISUAL_QUALITY.get().cloudShadowsEnabled(CLOUD_SHADOWS_ENABLED.get());
    }

    public static boolean atmosphereEnabled()
    {
        return SPACE_VISUAL_QUALITY.get().atmosphereEnabled(ATMOSPHERE_ENABLED.get());
    }

    public static boolean stellarViewStarsEnabled()
    {
        return SPACE_VISUAL_QUALITY.get().stellarViewStarsEnabled(STELLAR_VIEW_BACKGROUND_STARS.get());
    }

    private StarfieldClientConfig() {}
}
