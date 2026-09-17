package com.starboundmc.world.universe;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import org.slf4j.Logger;

import java.util.List;

/**
 * One side's view of the universe: a baseline plus whatever a live registry
 * supplied.
 *
 * <p>The server and the client each own an instance of this, because they are
 * different authorities rather than two views of one thing. The client is told
 * what the universe is; the server decides it. Giving each its own holder keeps
 * a client that is mid-datapack-reload from reading a server's registry, and
 * lets each be torn down at its own session boundary.</p>
 *
 * <h2>Why there is always data</h2>
 *
 * <p>Both sides can be asked for universe data before a registry is reachable —
 * the client during resource reload and in the main menu, the server before a
 * world exists. An empty answer would make the star map blank and force every
 * caller to null-check. So the baseline is always {@link BuiltInUniverse}: the
 * definitions the mod ships. A registry, when present, replaces it wholesale.</p>
 *
 * <p>Replacing rather than merging is deliberate: a datapack that removes a
 * system must actually remove it, so a live registry is authoritative whenever
 * it is available.</p>
 */
final class UniverseCatalogStore
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Built once and shared: these definitions are immutable, and rebuilding the
     * baseline on every reset would churn for no benefit.
     */
    private static final UniverseCatalog BASELINE = UniverseCatalog.of(BuiltInUniverse.systems());

    /** Distinguishes the two sides in log output. */
    private final String label;

    private volatile UniverseCatalog current = BASELINE;
    private volatile boolean syncedFromRegistry;

    UniverseCatalogStore(String label)
    {
        this.label = label;
    }

    /** The active universe. Never null, never empty. */
    UniverseCatalog current()
    {
        return current;
    }

    /** True when {@link #current()} came from a live registry rather than the baseline. */
    boolean isSynced()
    {
        return syncedFromRegistry;
    }

    UniverseCatalog baseline()
    {
        return BASELINE;
    }

    /**
     * Replaces the catalog with the systems the supplied registry access holds.
     *
     * <p>An absent or empty registry is not an error: a client in the main menu,
     * or a server that has not finished loading, keeps the baseline. Failing
     * loudly here would break a case that has a perfectly good answer.</p>
     */
    void refreshFrom(RegistryAccess registryAccess)
    {
        if (registryAccess == null)
            return;
        Registry<StarSystemDefinition> registry = registryAccess
                .registry(ModUniverseRegistries.STAR_SYSTEM).orElse(null);
        if (registry == null || registry.size() == 0)
        {
            // Log only on the transition, not per call: this runs from
            // render-driven and login-driven paths and would otherwise spam.
            if (syncedFromRegistry)
                LOGGER.info("{} universe registry unavailable; using the built-in universe.", label);
            install(null);
            return;
        }

        List<StarSystemDefinition> systems = registry.stream().toList();
        try
        {
            current = UniverseCatalog.of(systems);
            syncedFromRegistry = true;
        }
        catch (IllegalArgumentException invalid)
        {
            // A malformed universe must not take the caller down with it.
            LOGGER.warn("{} rejected the universe registry ({}); using the built-in universe.",
                    label, invalid.getMessage());
            install(null);
        }
    }

    /** Installs a catalog directly, or reverts to the baseline when null. */
    void install(UniverseCatalog catalog)
    {
        current = catalog == null ? BASELINE : catalog;
        syncedFromRegistry = catalog != null && !catalog.isEmpty();
    }

    void reset()
    {
        current = BASELINE;
        syncedFromRegistry = false;
    }
}
