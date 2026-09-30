package com.starboundmc.client;

/** Available space-rendering presets. Custom delegates to the user's feature toggles. */
public enum SpaceVisualQuality
{
    PERFORMANCE,
    BALANCED,
    HIGH,
    ULTRA,
    CUSTOM;

    public int backgroundStarBudget() {
        return switch (this) {
            case PERFORMANCE -> 2000;
            case BALANCED -> 6000;
            case HIGH, CUSTOM -> 10000;
            case ULTRA -> 16000;
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
