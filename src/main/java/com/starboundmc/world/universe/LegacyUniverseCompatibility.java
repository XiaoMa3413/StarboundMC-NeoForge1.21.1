package com.starboundmc.world.universe;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Translation between the saves written by the pre-data-layer builds and the
 * universe's entry ids.
 *
 * <p>Older saves stored a bare planet name ({@code "lush"}) rather than an entry
 * id ({@code "sys1:lush"}). This class owns that mapping so the rest of the code
 * never has to know the old naming.</p>
 *
 * <p>Deliberately string based. It used to work in terms of the {@code Planet}
 * enum, which meant the save format was tied to an enum whose values were also
 * the game's body list. The two are now separate concerns: this is a fixed
 * historical mapping that must keep working forever, and the universe is data
 * that a datapack can change.</p>
 *
 * <p>Nothing here guesses. {@code Planet.fromId} used to answer the starter planet
 * for any string it did not recognise, which would silently relocate a player
 * whose save named a body this build does not know; these methods return empty
 * instead and let the caller report the loss.</p>
 */
public final class LegacyUniverseCompatibility
{
    /**
     * Legacy planet name to the entry id that replaced it.
     *
     * <p>This is the mapping an old save needs, and it is frozen: these names
     * existed in released saves, so they must keep resolving even if the universe
     * is reorganised around them.</p>
     */
    private static final Map<String, String> ENTRY_BY_LEGACY_NAME = Map.of(
            "lush", "sys1:lush",
            "molten", "sys1:molten",
            "barren", "sys1:barren",
            "frozen", "sys2:frozen");

    /** The starter body, used where a location is missing entirely. */
    public static final String STARTER_ENTRY_ID = BuiltInUniverse.STARTER_BODY_ID;

    private LegacyUniverseCompatibility()
    {
    }

    /**
     * The entry id a stored legacy planet name refers to.
     *
     * <p>Matching ignores case and surrounding whitespace, because neither can
     * change which planet was meant. Anything else returns empty.</p>
     */
    public static Optional<String> parsePlanetId(String storedName)
    {
        if (storedName == null)
            return Optional.empty();
        String key = storedName.trim().toLowerCase(java.util.Locale.ROOT);
        return key.isEmpty() ? Optional.empty() : Optional.ofNullable(ENTRY_BY_LEGACY_NAME.get(key));
    }

    /**
     * The legacy planet name for an entry id, for writing the compatibility field.
     *
     * <p>Empty for a body that has no legacy name — a datapack body, or the gas
     * giant and rocky moon. The caller must write an empty value rather than a
     * plausible-looking wrong one, so an older build reading the save cannot be
     * told the player is somewhere they are not.</p>
     */
    public static Optional<String> legacyPlanetName(String entryId)
    {
        if (entryId == null)
            return Optional.empty();
        for (Map.Entry<String, String> entry : ENTRY_BY_LEGACY_NAME.entrySet())
        {
            if (entry.getValue().equals(entryId))
                return Optional.of(entry.getKey());
        }
        return Optional.empty();
    }

    /** True when this body had a name in the pre-data-layer builds. */
    public static boolean hasLegacyName(String entryId)
    {
        return legacyPlanetName(entryId).isPresent();
    }

    /** The frozen legacy mapping, for tests that pin the save compatibility contract. */
    public static Map<String, String> legacyNameMapping()
    {
        return ENTRY_BY_LEGACY_NAME;
    }

    /**
     * Entry id to legacy name, in the order the old enum declared the planets.
     *
     * <p>Order matters only for tests that walk the mapping predictably.</p>
     */
    public static Map<String, String> legacyNameByEntryId()
    {
        Map<String, String> inverted = new LinkedHashMap<>();
        for (String name : new String[] {"lush", "molten", "frozen", "barren"})
        {
            String entryId = ENTRY_BY_LEGACY_NAME.get(name);
            if (entryId != null)
                inverted.put(entryId, name);
        }
        return Map.copyOf(inverted);
    }
}
