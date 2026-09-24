package com.starboundmc.client;

/** Shared travel limits for the sealed, short-travel Blockbench doors. */
public record LockerDoorPose(float slide, float retreat, float hologramAlpha) {
    public static LockerDoorPose at(float opening) {
        float progress = Math.clamp(opening, 0.0F, 1.0F);
        float slideProgress = Math.clamp((progress - 0.225F) / 0.775F, 0.0F, 1.0F);
        slideProgress = slideProgress * slideProgress * (3.0F - 2.0F * slideProgress);
        return new LockerDoorPose(1.2F / 16 * slideProgress,
                0.65F / 16 * Math.min(1.0F, progress / 0.225F),
                1.0F - Math.min(1.0F, progress / 0.225F));
    }
}
