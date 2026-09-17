package com.starboundmc.client;

import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.ClientUniverseCatalog;
import com.starboundmc.world.universe.StarSystemDefinition;
import com.starboundmc.world.universe.UniverseCatalog;

import java.util.List;

/**
 * Universe lookups for the star-map UI, in terms the UI actually asks for.
 *
 * <p>This exists so the star map does not have to care which source the universe
 * came from, and so the "is this the body I am parked at" question has exactly
 * one implementation. That question has a subtlety worth keeping in one place:
 * the authoritative answer is the synced entry id, but before the first
 * star-state packet arrives that id is null while the legacy planet already
 * defaults to Lush. Falling back to the planet keeps a freshly opened console
 * pointing at the right body instead of at nothing.</p>
 */
public final class StarmapUniverse
{
    private StarmapUniverse()
    {
    }

    /** The active universe catalog. */
    public static UniverseCatalog catalog()
    {
        return ClientUniverseCatalog.current();
    }

    public static List<StarSystemDefinition> allSystems()
    {
        return catalog().allSystems();
    }

    /**
     * Bodies the cockpit window draws.
     *
     * <p>Replaces the renderer's {@code Planet.values()} iteration, so a body added
     * by a datapack renders without a new enum constant.</p>
     */
    public static List<CelestialBodyDefinition> spaceRenderedBodies()
    {
        return catalog().spaceRenderedBodies();
    }

    /**
     * A stable identity for the active catalog, for cache invalidation.
     *
     * <p>Compared by identity. The catalog is replaced wholesale when the universe
     * changes, so identity is sufficient and cannot be forgotten the way an
     * explicit invalidation call can.</p>
     */
    public static Object catalogIdentity()
    {
        return catalog();
    }

    /** System owning a body, by id. */
    public static StarSystemDefinition systemOf(String entryId)
    {
        return catalog().systemOfBody(entryId).orElse(null);
    }

    public static StarSystemDefinition system(String systemId)
    {
        return catalog().system(systemId).orElse(null);
    }

    public static CelestialBodyDefinition body(String entryId)
    {
        return catalog().body(entryId).orElse(null);
    }

    /**
     * The body that owns a dimension, or null.
     *
     * <p>How a surface renderer identifies its own sky: a dimension knows which
     * body it is, which is what lets the sky stay dimension-driven rather than
     * naming a body.</p>
     */
    public static CelestialBodyDefinition bodyForDimension(net.minecraft.resources.ResourceLocation dimension)
    {
        return dimension == null ? null : catalog().bodyByDimension(dimension).orElse(null);
    }

    public static StarSystemDefinition systemOfBody(String entryId)
    {
        return catalog().systemOfBody(entryId).orElse(null);
    }

    /** The system id owning a body, or null. Replaces prefix splitting. */
    public static String systemIdOfEntry(String entryId)
    {
        return catalog().systemOfBody(entryId)
                .map(StarSystemDefinition::systemId).orElse(null);
    }

    /**
     * Whether the ship is currently at this body.
     *
     * <p>Uses the synced entry id when present, and falls back to the legacy
     * planet only while that id is still unknown.</p>
     */
    public static boolean isCurrent(String entryId)
    {
        if (entryId == null)
            return false;
        // getCurrentEntryId() already falls back to the locally tracked body, so
        // the synced id and the tracked id are the same question now.
        return entryId.equals(ClientPlanetState.getCurrentEntryId());
    }

    /**
     * The body's star-map sprite, for UI that shows where the ship is.
     *
     * <p>Uses the body's authored sprite id when it has one, and otherwise falls
     * back to the built-in naming convention. Resolving through the definition
     * means a body that authors its own path works, which splitting the entry id
     * on {@code ':'} could not express.</p>
     */
    public static net.minecraft.resources.ResourceLocation bodySprite(String entryId)
    {
        CelestialBodyDefinition body = body(entryId);
        if (body != null)
        {
            String texture = body.starmapVisual().getTextureId();
            if (texture != null)
                return net.minecraft.resources.ResourceLocation.parse(texture);
        }
        String name = entryId == null ? "lush"
                : entryId.substring(entryId.indexOf(':') + 1);
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                com.starboundmc.StarboundMC.MODID,
                "textures/gui/starmap/bodies/" + name + ".png");
    }
}
