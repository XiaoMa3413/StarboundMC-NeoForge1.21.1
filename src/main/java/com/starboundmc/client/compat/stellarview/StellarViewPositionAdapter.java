package com.starboundmc.client.compat.stellarview;

import java.util.Objects;

import com.starboundmc.space.UniversePosition;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.povstalec.stellarview.api.client.ExternalViewCenterCoords;
import net.povstalec.stellarview.common.util.SpaceCoords;

/** Maps the ship simulation's sector/local coordinates without using the player's world position. */
public final class StellarViewPositionAdapter implements ExternalViewCenterCoords
{
    private UniversePosition position = UniversePosition.of(0.0, 0.0, 0.0);

    public void setPosition(UniversePosition position)
    {
        this.position = Objects.requireNonNull(position, "position");
    }

    @Override
    public SpaceCoords sample(ClientLevel level, Camera camera, float partialTicks)
    {
        return toSpaceCoords(position);
    }

    /** One SBMC virtual unit maps to one Stellar View light year. */
    public static SpaceCoords toSpaceCoords(UniversePosition position)
    {
        Objects.requireNonNull(position, "position");
        long x = (long) Math.floor(position.localX());
        long y = (long) Math.floor(position.localY());
        long z = (long) Math.floor(position.localZ());
        return new SpaceCoords(
                Math.addExact(Math.multiplyExact(position.sector().x(), (long) UniversePosition.SECTOR_SIZE), x),
                Math.addExact(Math.multiplyExact(position.sector().y(), (long) UniversePosition.SECTOR_SIZE), y),
                Math.addExact(Math.multiplyExact(position.sector().z(), (long) UniversePosition.SECTOR_SIZE), z),
                (position.localX() - x) * SpaceCoords.KM_PER_LY,
                (position.localY() - y) * SpaceCoords.KM_PER_LY,
                (position.localZ() - z) * SpaceCoords.KM_PER_LY);
    }
}
