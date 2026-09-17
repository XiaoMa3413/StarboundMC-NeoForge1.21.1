package com.starboundmc.warp;

import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.universe.BodyNavigationProfile;
import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.ClientUniverseCatalog;
import com.starboundmc.world.universe.ServerUniverseCatalog;
import com.starboundmc.world.universe.StarSystemDefinition;
import com.starboundmc.world.universe.UniverseCatalog;
import net.minecraft.world.phys.Vec3;

/**
 * Flight geometry looked up by body id (migration step A5).
 *
 * <p>This is the universe queries {@link ShipSpace} used to answer from
 * {@code EnumMap<Planet, ...>} tables. Those tables were the universe database
 * living inside a math helper; here they come from the catalog, so a body added
 * by a datapack is flyable without touching any of this.</p>
 *
 * <h2>Which catalog</h2>
 *
 * <p>Both sides of the connection must compute the same curve, and each has its
 * own catalog. The server is authoritative when it has one, so a running
 * server's universe wins; a pure client falls back to its own synced (or
 * built-in) catalog. Since both are built from the same registry they agree by
 * construction, and preferring the server keeps that true even in the window
 * before a client finishes syncing.</p>
 */
public final class UniverseNavigation
{
    private UniverseNavigation()
    {
    }

    /**
     * The universe flight geometry resolves against.
     *
     * <p>A synced server catalog is preferred because the server owns travel
     * authority; otherwise the client's catalog is used, which is itself the
     * synced server universe or the built-in baseline.</p>
     */
    public static UniverseCatalog active()
    {
        return ServerUniverseCatalog.isSynced()
                ? ServerUniverseCatalog.current()
                : ClientUniverseCatalog.current();
    }

    private static BodyNavigationProfile navigation(String entryId)
    {
        CelestialBodyDefinition body = entryId == null ? null
                : active().body(entryId).orElse(null);
        return body == null ? null : body.navigation().orElse(null);
    }

    /** True when the body can be flown to. */
    public static boolean isNavigable(String entryId)
    {
        return navigation(entryId) != null;
    }

    /** A body by entry id, or null when the active universe does not have it. */
    public static CelestialBodyDefinition body(String entryId)
    {
        return entryId == null ? null : active().body(entryId).orElse(null);
    }

    /** Body radius in virtual-space units. */
    public static double radius(String entryId)
    {
        BodyNavigationProfile profile = navigation(entryId);
        if (profile == null)
            throw new IllegalArgumentException("Unknown navigable body: " + entryId);
        return profile.bodyRadius();
    }

    /**
     * Dock heading. A body with no navigation profile yields 0 degrees, matching
     * the lenient lookup the legacy table used.
     */
    public static double yawDock(String entryId)
    {
        BodyNavigationProfile profile = navigation(entryId);
        return profile == null ? 0.0 : profile.dockYaw();
    }

    /** Where the ship parks at this body, in continuous-universe coordinates. */
    public static UniversePosition universeDock(String entryId)
    {
        BodyNavigationProfile profile = navigation(entryId);
        if (profile == null)
            throw new IllegalArgumentException("Unknown navigable body: " + entryId);
        return profile.dockPosition();
    }

    /** Where the body itself sits, in continuous-universe coordinates. */
    public static UniversePosition universeBodyPosition(String entryId)
    {
        BodyNavigationProfile profile = navigation(entryId);
        if (profile == null)
            throw new IllegalArgumentException("Unknown navigable body: " + entryId);
        return profile.bodyPosition();
    }

    /** Local-space view of the dock, for the renderers that still work in Vec3. */
    public static Vec3 vDock(String entryId)
    {
        return universeDock(entryId).toLocalVec3();
    }

    /** Local-space view of the body position. */
    public static Vec3 qPos(String entryId)
    {
        return universeBodyPosition(entryId).toLocalVec3();
    }

    /** Offset that yields the same apparent radius at every dock. */
    public static Vec3 dockOffset(String entryId)
    {
        return ShipSpace.dockOffset(radius(entryId));
    }

    /** Straight-line distance between two docks. */
    public static double flightDistance(String fromEntryId, String toEntryId)
    {
        return Math.sqrt(universeDock(fromEntryId).distanceToSqr(universeDock(toEntryId)));
    }

    /** Fixed virtual-space star position for a body's system. */
    public static Vec3 sunPos(String entryId)
    {
        return systemOf(entryId).stellarVisual().getVirtualPosition();
    }

    /** Unit direction from a body toward its system's star. */
    public static Vec3 sunDirection(String entryId)
    {
        return systemOf(entryId).lightingDirection();
    }

    /**
     * The system owning a body.
     *
     * <p>Ownership is a data property, replacing the legacy habit of splitting the
     * entry id on {@code ':'}.</p>
     */
    public static StarSystemDefinition systemOf(String entryId)
    {
        StarSystemDefinition system = entryId == null ? null
                : active().systemOfBody(entryId).orElse(null);
        if (system == null)
            throw new IllegalArgumentException("Body belongs to no known system: " + entryId);
        return system;
    }

    /** System id owning a body, or null when it is unknown. */
    public static String systemIdOf(String entryId)
    {
        return entryId == null ? null
                : active().systemOfBody(entryId).map(StarSystemDefinition::systemId).orElse(null);
    }

    /** True when both bodies belong to the same system. */
    public static boolean sameSystem(String fromEntryId, String toEntryId)
    {
        String from = systemIdOf(fromEntryId);
        String to = systemIdOf(toEntryId);
        return from != null && from.equals(to);
    }

    /**
     * Bodies that take part in route obstacle avoidance.
     *
     * <p>Only navigable bodies are returned. The gas giant and the rocky moon have
     * no navigation profile, so they stay out of route shaping exactly as they did
     * when this iterated the four-planet enum — letting them in would silently
     * bend every existing course.</p>
     */
    public static java.util.List<CelestialBodyDefinition> avoidanceBodies()
    {
        return active().navigableBodies();
    }

    /**
     * Other rendered bodies in the same system as the supplied one.
     *
     * <p>Used to warm the texture cache for a whole system at once: a primary and
     * its moon share the departure sky, and decoding them at different times makes
     * one pop in after the other.</p>
     */
    public static java.util.List<CelestialBodyDefinition> companionBodies(String entryId)
    {
        CelestialBodyDefinition self = body(entryId);
        StarSystemDefinition system = entryId == null ? null
                : active().systemOfBody(entryId).orElse(null);
        if (self == null || system == null)
            return java.util.List.of();
        java.util.List<CelestialBodyDefinition> companions = new java.util.ArrayList<>();
        for (CelestialBodyDefinition candidate : system.bodies())
        {
            if (!candidate.entryId().equals(entryId) && candidate.isSpaceRendered())
                companions.add(candidate);
        }
        return java.util.List.copyOf(companions);
    }
}
