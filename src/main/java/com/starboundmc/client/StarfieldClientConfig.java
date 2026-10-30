package com.starboundmc.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only quality preset and feature toggles for space rendering. */
public final class StarfieldClientConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue SPACE_BLOOM_STRENGTH = BUILDER
            .comment("HDR optical bloom strength. Performance and minimal visual fallback disable bloom; zero disables it in all presets.")
            .translation("starboundmc.config.space_bloom_strength")
            .defineInRange("spaceBloomStrength", .32, 0.0, 1.5);

    public static final ModConfigSpec.DoubleValue SPACE_EXPOSURE = BUILDER
            .comment("Fixed exposure for the native linear HDR space pipeline. Does not affect world blocks or GUIs.")
            .translation("starboundmc.config.space_exposure")
            .defineInRange("spaceExposure", 1.0, .25, 4.0);

    public static final ModConfigSpec.EnumValue<SpaceVisualQuality> SPACE_VISUAL_QUALITY = BUILDER
            .comment("Space visual quality preset. Performance disables clouds and uses a smaller cinematic star budget.",
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

    private StarfieldClientConfig() {}
}
