package com.starboundmc.client;

/** Available space-rendering presets. Custom delegates to the user's feature toggles. */
public enum SpaceVisualQuality
{
    PERFORMANCE,
    BALANCED,
    HIGH,
    ULTRA,
    CUSTOM;

    public int bloomLevels() {
        return switch (this) {
            case PERFORMANCE -> 0;
            case BALANCED -> 3;
            case HIGH, CUSTOM -> 4;
            case ULTRA -> 5;
        };
    }

    public int bloomDownsample() { return this == BALANCED ? 4 : 2; }

    public int atmosphereSamples() {
        return switch (this) {
            case PERFORMANCE -> 6;
            case BALANCED -> 12;
            case HIGH, CUSTOM -> 20;
            case ULTRA -> 28;
        };
    }

    public int atmosphereLightSamples() { return this == ULTRA ? 8 : this == HIGH || this == CUSTOM ? 6 : 4; }

    public int backgroundStarBudget() {
        return switch (this) {
            case PERFORMANCE -> 2000;
            case BALANCED -> 6000;
            case HIGH, CUSTOM -> 20000;
            case ULTRA -> 32000;
        };
    }

    public boolean cloudsEnabled(boolean customValue)
    {
        return switch (this)
        {
            case PERFORMANCE -> false;
            case BALANCED, HIGH, ULTRA -> true;
            case CUSTOM -> customValue;
        };
    }

    public boolean atmosphereEnabled(boolean customValue)
    {
        return switch (this)
        {
            case PERFORMANCE, BALANCED, HIGH, ULTRA -> true;
            case CUSTOM -> customValue;
        };
    }

    public boolean stellarViewStarsEnabled(boolean customValue)
    {
        return switch (this)
        {
            case PERFORMANCE -> false;
            case BALANCED, HIGH, ULTRA -> true;
            case CUSTOM -> customValue;
        };
    }

    public boolean cloudShadowsEnabled(boolean customValue)
    {
        return switch (this)
        {
            case PERFORMANCE, BALANCED -> false;
            case HIGH, ULTRA -> true;
            case CUSTOM -> customValue;
        };
    }
}
