package com.starboundmc.world.universe;

/**
 * Null-returning universe lookups for tests.
 *
 * <p>The legacy registry returned {@code null} for an unknown id, and many tests
 * assert exactly that. The catalog returns {@link java.util.Optional}, so these
 * helpers keep the old contract available without every test repeating
 * {@code .orElse(null)}.</p>
 */
public final class UniverseTestSupport
{
    private UniverseTestSupport()
    {
    }

    /** The active client universe. */
    public static UniverseCatalog universe()
    {
        ClientUniverseCatalog.reset();
        return ClientUniverseCatalog.current();
    }

    public static StarSystemDefinition system(String systemId)
    {
        return universe().system(systemId).orElse(null);
    }

    public static CelestialBodyDefinition body(String entryId)
    {
        return universe().body(entryId).orElse(null);
    }

    /** System id owning a body, or null. */
    public static String systemIdOfEntry(String entryId)
    {
        return universe().systemOfBody(entryId)
                .map(StarSystemDefinition::systemId).orElse(null);
    }

    /**
     * Entry ids of the bodies that can be flown to.
     *
     * <p>This is the shape tests iterate where they used to loop over
     * {@code Planet.values()}. The four navigable bodies are the same four, so a
     * test that covered every route still covers every route.</p>
     */
    public static java.util.List<String> navigableEntryIds()
    {
        return universe().navigableBodies().stream()
                .map(CelestialBodyDefinition::entryId)
                .toList();
    }
}
