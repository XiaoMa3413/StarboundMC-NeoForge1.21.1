package com.starboundmc.world.universe;

import net.minecraft.core.RegistryAccess;

/**
 * The client's view of the universe, and the only place client code should ask
 * universe questions (migration step A4).
 *
 * <p>Shares its implementation with {@link ServerUniverseCatalog} through
 * {@link UniverseCatalogStore}; see that class for why a baseline always exists
 * and why a registry replaces rather than merges.</p>
 *
 * <h2>Lifecycle</h2>
 *
 * <p>{@link #reset()} drops back to the baseline and is called on disconnect, so
 * one server's universe cannot leak into the next session. A stale catalog is
 * worse than a baseline one: the baseline is at least the universe the mod ships
 * with, whereas stale server data is a universe the player is no longer in.</p>
 */
public final class ClientUniverseCatalog
{
    private static final UniverseCatalogStore STORE = new UniverseCatalogStore("Client");

    private ClientUniverseCatalog()
    {
    }

    /** The active universe. Never null, never empty. */
    public static UniverseCatalog current()
    {
        return STORE.current();
    }

    /** True when {@link #current()} came from a server registry rather than the baseline. */
    public static boolean isSynced()
    {
        return STORE.isSynced();
    }

    /** Replaces the catalog with the systems the supplied registry access holds. */
    public static void refreshFrom(RegistryAccess registryAccess)
    {
        STORE.refreshFrom(registryAccess);
    }

    /** Drops back to the built-in universe. Called on disconnect. */
    public static void reset()
    {
        STORE.reset();
    }

    /**
     * Installs a catalog directly. For tests, which have no {@code RegistryAccess}
     * and need to exercise the empty and custom-universe paths.
     */
    public static void setForTesting(UniverseCatalog catalog)
    {
        STORE.install(catalog);
    }

    /** The built-in universe, exposed for callers that need it explicitly. */
    public static UniverseCatalog baseline()
    {
        return STORE.baseline();
    }
}
