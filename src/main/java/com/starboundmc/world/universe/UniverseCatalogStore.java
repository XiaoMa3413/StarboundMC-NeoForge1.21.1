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
 *
 * <h2>Falling back versus failing fast</h2>
 *
 * <p>The two sides differ once a world is running, and the difference is
 * deliberate. A client keeps the baseline when the registry is missing or
 * malformed: it cannot repair the server's data, and a blank star map over a
 * datapack it merely has not synced yet would be a worse failure than showing the
 * shipped universe.</p>
 *
 * <p>A server is the authority, and its registry is loaded from the very world it
 * is about to serve — so a registry it cannot use is not a transient condition,
 * it means the save's universe disagrees with the data on disk. Continuing would
 * silently substitute a different universe than the save was written against,
 * which is exactly how a ship ends up parked at a body this build cannot place.
 * A server therefore fails fast and names the problem.</p>
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

    /**
     * Whether an unusable registry is fatal.
     *
     * <p>True for the server, which owns the data; false for the client, which is
     * told what the universe is and must keep working when it has not been told.</p>
     */
    private final boolean failFastOnUnusableRegistry;

    private volatile UniverseCatalog current = BASELINE;
    private volatile boolean syncedFromRegistry;

    UniverseCatalogStore(String label)
    {
        this(label, false);
    }

    UniverseCatalogStore(String label, boolean failFastOnUnusableRegistry)
    {
        this.label = label;
        this.failFastOnUnusableRegistry = failFastOnUnusableRegistry;
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
     * <p>A client keeps the baseline when the registry is absent or unusable, which
     * is the main-menu and not-yet-synced case. A server configured to fail fast
     * refuses instead, because it is looking at the data its own world loaded: see
     * the class comment.</p>
     */
    void refreshFrom(RegistryAccess registryAccess)
    {
        if (registryAccess == null)
        {
            if (failFastOnUnusableRegistry)
                throw new IllegalStateException(label
                        + " universe: no registry access was supplied for a running world");
            return;
        }
        Registry<StarSystemDefinition> registry = registryAccess
                .registry(ModUniverseRegistries.STAR_SYSTEM).orElse(null);
        if (registry == null || registry.size() == 0)
        {
            if (failFastOnUnusableRegistry)
            {
                // The mod ships this registry, so a running server that cannot see
                // it has not loaded its own data. Substituting the built-in universe
                // here would serve a different universe than the save assumes.
                throw new IllegalStateException(label
                        + " universe: the " + ModUniverseRegistries.STAR_SYSTEM.location()
                        + " registry is empty or absent in a running world");
            }
            // Log only on the transition, not per call: this runs from
            // render-driven and login-driven paths and would otherwise spam.
            if (syncedFromRegistry)
                LOGGER.info("{} universe registry unavailable; using the built-in universe.", label);
            install(null);
            return;
        }

        installValidated(registry.stream().toList(), "the universe registry");
    }

    /**
     * Indexes the supplied systems, or falls back (or refuses) when they are invalid.
     *
     * <p>Separate from {@link #refreshFrom} because this is the decision, and the
     * registry is only where the systems come from. Keeping them apart means the
     * fallback-versus-fail-fast contract can be tested without standing up a
     * {@code RegistryAccess}.</p>
     */
    void installValidated(List<StarSystemDefinition> systems, String source)
    {
        try
        {
            current = UniverseCatalog.of(systems);
            syncedFromRegistry = true;
        }
        catch (IllegalArgumentException invalid)
        {
            if (failFastOnUnusableRegistry)
            {
                throw new IllegalStateException(label + " universe: " + source
                        + " was rejected: " + invalid.getMessage(), invalid);
            }
            // A malformed universe must not take the client down with it.
            LOGGER.warn("{} rejected {} ({}); using the built-in universe.",
                    label, source, invalid.getMessage());
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
