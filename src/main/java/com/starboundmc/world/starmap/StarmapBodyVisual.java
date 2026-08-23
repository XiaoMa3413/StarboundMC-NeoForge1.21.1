package com.starboundmc.world.starmap;

import java.util.Objects;

/**
 * Data-only visual description for a body on the star map.
 *
 * <p>The renderer consumes marker geometry, colour/shading values and an
 * optional full-colour sprite. Remaining template values stay data-driven so
 * later body types and optional masks do not require navigation or UI changes.</p>
 */
public final class StarmapBodyVisual
{
    private final StarmapBodyType bodyType;
    private final int markerSize;
    private final int primaryColor;
    private final int secondaryColor;
    private final long textureSeed;
    private final int atmosphereColor;
    private final float atmosphereStrength;
    private final float surfaceDetail;
    private final float bandStrength;
    private final int ringColor;
    private final float ringStrength;
    private final String textureId;
    private final String focusTextureId;
    private final String textureMaskId;

    private StarmapBodyVisual(Builder builder)
    {
        this.bodyType = builder.bodyType;
        this.markerSize = builder.markerSize;
        this.primaryColor = builder.primaryColor;
        this.secondaryColor = builder.secondaryColor;
        this.textureSeed = builder.textureSeed;
        this.atmosphereColor = builder.atmosphereColor;
        this.atmosphereStrength = builder.atmosphereStrength;
        this.surfaceDetail = builder.surfaceDetail;
        this.bandStrength = builder.bandStrength;
        this.ringColor = builder.ringColor;
        this.ringStrength = builder.ringStrength;
        this.textureId = builder.textureId;
        this.focusTextureId = builder.focusTextureId;
        this.textureMaskId = builder.textureMaskId;
    }

    public static Builder builder(StarmapBodyType bodyType, int primaryColor,
                                  int markerSize, long textureSeed)
    {
        return new Builder(bodyType, primaryColor, markerSize, textureSeed);
    }

    /** Compatibility profile matching the former colour/size-only model. */
    public static StarmapBodyVisual basic(int color, int markerSize)
    {
        return builder(StarmapBodyType.GENERIC, color, markerSize, 0L).build();
    }

    public StarmapBodyType getBodyType()
    {
        return bodyType;
    }

    public int getMarkerSize()
    {
        return markerSize;
    }

    public int getPrimaryColor()
    {
        return primaryColor;
    }

    public int getSecondaryColor()
    {
        return secondaryColor;
    }

    public long getTextureSeed()
    {
        return textureSeed;
    }

    public int getAtmosphereColor()
    {
        return atmosphereColor;
    }

    public float getAtmosphereStrength()
    {
        return atmosphereStrength;
    }

    public float getSurfaceDetail()
    {
        return surfaceDetail;
    }

    public float getBandStrength()
    {
        return bandStrength;
    }

    public int getRingColor()
    {
        return ringColor;
    }

    public float getRingStrength()
    {
        return ringStrength;
    }

    public String getTextureMaskId()
    {
        return textureMaskId;
    }

    /** Optional full-colour disc sprite used before the procedural fallback. */
    public String getTextureId()
    {
        return textureId;
    }

    /** Optional higher-detail sprite for the enlarged focus-page target. */
    public String getFocusTextureId()
    {
        return focusTextureId;
    }

    public boolean hasAtmosphere()
    {
        return atmosphereStrength > 0.0F;
    }

    public boolean hasBands()
    {
        return bandStrength > 0.0F;
    }

    public boolean hasRings()
    {
        return ringStrength > 0.0F;
    }

    public static final class Builder
    {
        private final StarmapBodyType bodyType;
        private final int primaryColor;
        private final int markerSize;
        private final long textureSeed;
        private int secondaryColor;
        private int atmosphereColor;
        private float atmosphereStrength;
        private float surfaceDetail;
        private float bandStrength;
        private int ringColor;
        private float ringStrength;
        private String textureId;
        private String focusTextureId;
        private String textureMaskId;

        private Builder(StarmapBodyType bodyType, int primaryColor,
                        int markerSize, long textureSeed)
        {
            this.bodyType = Objects.requireNonNull(bodyType, "bodyType");
            if (markerSize <= 0)
                throw new IllegalArgumentException("markerSize must be positive");
            this.primaryColor = primaryColor;
            this.markerSize = markerSize;
            this.textureSeed = textureSeed;
            this.secondaryColor = primaryColor;
            this.atmosphereColor = primaryColor;
            this.ringColor = primaryColor;
        }

        public Builder secondaryColor(int color)
        {
            this.secondaryColor = color;
            return this;
        }

        public Builder atmosphere(int color, float strength)
        {
            this.atmosphereColor = color;
            this.atmosphereStrength = unitValue("atmosphereStrength", strength);
            return this;
        }

        public Builder surfaceDetail(float strength)
        {
            this.surfaceDetail = unitValue("surfaceDetail", strength);
            return this;
        }

        public Builder bands(float strength)
        {
            this.bandStrength = unitValue("bandStrength", strength);
            return this;
        }

        public Builder rings(int color, float strength)
        {
            this.ringColor = color;
            this.ringStrength = unitValue("ringStrength", strength);
            return this;
        }

        /** Full-colour transparent body sprite, for example a pixel-art globe. */
        public Builder texture(String resourceId)
        {
            if (resourceId == null || resourceId.isBlank())
                throw new IllegalArgumentException("texture resource id must not be blank");
            this.textureId = resourceId;
            return this;
        }

        /** Higher-detail variant of {@link #texture(String)} for the focus page. */
        public Builder focusTexture(String resourceId)
        {
            if (resourceId == null || resourceId.isBlank())
                throw new IllegalArgumentException("focus texture resource id must not be blank");
            this.focusTextureId = resourceId;
            return this;
        }

        /** Optional resource id for a reusable monochrome detail mask. */
        public Builder textureMask(String resourceId)
        {
            if (resourceId == null || resourceId.isBlank())
                throw new IllegalArgumentException("textureMask resource id must not be blank");
            this.textureMaskId = resourceId;
            return this;
        }

        public StarmapBodyVisual build()
        {
            return new StarmapBodyVisual(this);
        }

        private static float unitValue(String name, float value)
        {
            if (!Float.isFinite(value) || value < 0.0F || value > 1.0F)
                throw new IllegalArgumentException(name + " must be finite and within [0, 1]");
            return value;
        }
    }
}
