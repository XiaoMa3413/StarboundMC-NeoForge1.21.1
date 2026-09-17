package com.starboundmc.world.starmap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Objects;
import java.util.Optional;

/**
 * Data-only visual description for a body on the star map.
 *
 * <p>The renderer consumes marker geometry, colour/shading values and an
 * optional full-colour sprite. Remaining template values stay data-driven so
 * later body types and optional masks do not require navigation or UI changes.</p>
 */
public final class StarmapBodyVisual
{
    /**
     * Datapack form.
     *
     * <p>Colours and strengths are written out even when they merely repeat a
     * default, so a definition round-trips value for value. The three optional
     * sprite ids are the only fields that may be absent.</p>
     */
    public static final Codec<StarmapBodyVisual> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    StarmapBodyType.CODEC.fieldOf("body_type").forGetter(StarmapBodyVisual::getBodyType),
                    Codec.INT.fieldOf("marker_size").forGetter(StarmapBodyVisual::getMarkerSize),
                    Codec.INT.fieldOf("primary_color").forGetter(StarmapBodyVisual::getPrimaryColor),
                    Codec.INT.optionalFieldOf("secondary_color")
                            .forGetter(v -> Optional.of(v.getSecondaryColor())),
                    Codec.LONG.optionalFieldOf("texture_seed", 0L).forGetter(StarmapBodyVisual::getTextureSeed),
                    Codec.INT.optionalFieldOf("atmosphere_color")
                            .forGetter(v -> Optional.of(v.getAtmosphereColor())),
                    Codec.FLOAT.optionalFieldOf("atmosphere_strength", 0.0F)
                            .forGetter(StarmapBodyVisual::getAtmosphereStrength),
                    Codec.FLOAT.optionalFieldOf("surface_detail", 0.0F)
                            .forGetter(StarmapBodyVisual::getSurfaceDetail),
                    Codec.FLOAT.optionalFieldOf("band_strength", 0.0F)
                            .forGetter(StarmapBodyVisual::getBandStrength),
                    Codec.INT.optionalFieldOf("ring_color")
                            .forGetter(v -> Optional.of(v.getRingColor())),
                    Codec.FLOAT.optionalFieldOf("ring_strength", 0.0F)
                            .forGetter(StarmapBodyVisual::getRingStrength),
                    Codec.STRING.optionalFieldOf("texture")
                            .forGetter(v -> Optional.ofNullable(v.getTextureId())),
                    Codec.STRING.optionalFieldOf("focus_texture")
                            .forGetter(v -> Optional.ofNullable(v.getFocusTextureId())),
                    Codec.STRING.optionalFieldOf("texture_mask")
                            .forGetter(v -> Optional.ofNullable(v.getTextureMaskId()))
            ).apply(instance, StarmapBodyVisual::fromCodec));

    private static StarmapBodyVisual fromCodec(StarmapBodyType bodyType, int markerSize, int primaryColor,
                                               Optional<Integer> secondaryColor, long textureSeed,
                                               Optional<Integer> atmosphereColor, float atmosphereStrength,
                                               float surfaceDetail, float bandStrength,
                                               Optional<Integer> ringColor, float ringStrength,
                                               Optional<String> texture, Optional<String> focusTexture,
                                               Optional<String> textureMask)
    {
        Builder builder = builder(bodyType, primaryColor, markerSize, textureSeed)
                .secondaryColor(secondaryColor.orElse(primaryColor))
                .surfaceDetail(surfaceDetail)
                .bands(bandStrength)
                .atmosphere(atmosphereColor.orElse(primaryColor), atmosphereStrength)
                .rings(ringColor.orElse(primaryColor), ringStrength);
        texture.ifPresent(builder::texture);
        focusTexture.ifPresent(builder::focusTexture);
        textureMask.ifPresent(builder::textureMask);
        return builder.build();
    }

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

    /**
     * Value equality. This type is an immutable description of a body's marker
     * art, so two instances built from the same values are interchangeable; the
     * definition layer relies on that to compare a coded round trip against the
     * original.
     */
    @Override
    public boolean equals(Object object)
    {
        if (this == object)
            return true;
        if (!(object instanceof StarmapBodyVisual other))
            return false;
        return markerSize == other.markerSize
                && primaryColor == other.primaryColor
                && secondaryColor == other.secondaryColor
                && textureSeed == other.textureSeed
                && atmosphereColor == other.atmosphereColor
                && Float.compare(atmosphereStrength, other.atmosphereStrength) == 0
                && Float.compare(surfaceDetail, other.surfaceDetail) == 0
                && Float.compare(bandStrength, other.bandStrength) == 0
                && ringColor == other.ringColor
                && Float.compare(ringStrength, other.ringStrength) == 0
                && bodyType == other.bodyType
                && Objects.equals(textureId, other.textureId)
                && Objects.equals(focusTextureId, other.focusTextureId)
                && Objects.equals(textureMaskId, other.textureMaskId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(bodyType, markerSize, primaryColor, secondaryColor, textureSeed,
                atmosphereColor, atmosphereStrength, surfaceDetail, bandStrength,
                ringColor, ringStrength, textureId, focusTextureId, textureMaskId);
    }

    @Override
    public String toString()
    {
        return "StarmapBodyVisual[" + bodyType + ", size=" + markerSize
                + ", primary=0x" + Integer.toHexString(primaryColor) + "]";
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
