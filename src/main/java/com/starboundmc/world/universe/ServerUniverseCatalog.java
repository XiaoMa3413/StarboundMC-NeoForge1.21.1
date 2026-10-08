package com.starboundmc.world.universe;

import net.minecraft.server.MinecraftServer;

/** Server universe authority built from the loaded registry on startup and cleared on stop.
 * An unusable server registry is a startup error; it never falls back to built-in gameplay definitions. */
public final class ServerUniverseCatalog
{
    /**
     * The server's store is the fail-fast one: see {@link UniverseCatalogStore}.
     * A running server owns its universe data, so a registry it cannot use is a
     * startup failure, not something to paper over with the built-in universe.
     */
    private static final UniverseCatalogStore STORE = new UniverseCatalogStore("Server", true);

    private ServerUniverseCatalog()
    {
    }

    /** The running server's universe, or the built-in baseline before one starts. */
    public static UniverseCatalog current()
    {
        return STORE.current();
    }

    /** True when the catalog came from a running server's registry. */
    public static boolean isSynced()
    {
        return STORE.isSynced();
    }

    /** Builds the catalog from the server's loaded registries. */
    public static void initialize(MinecraftServer server)
    {
        if (server == null)
        {
            STORE.reset();
            return;
        }
        STORE.refreshFrom(server.registryAccess());
    }

    /**
     * Builds the catalog from a registry access directly.
     *
     * <p>Package-visible so the fail-fast contract can be driven through the real
     * production path — this is the call {@link #initialize} makes — rather than by
     * constructing a store, which would test the store but not the wiring that
     * decides which side fails fast.</p>
     */
    static void refreshFrom(net.minecraft.core.RegistryAccess registryAccess)
    {
        STORE.refreshFrom(registryAccess);
    }

    /** Drops the catalog when the server stops, so no world's universe outlives it. */
    public static void clear()
    {
        STORE.reset();
    }

    /** The built-in universe, for callers that need it explicitly. */
    public static UniverseCatalog baseline()
    {
        return STORE.baseline();
    }

    /**
     * Installs a catalog directly. For tests, which have no {@code MinecraftServer}
     * and need to exercise the empty and custom-universe paths.
     */
    public static void setForTesting(UniverseCatalog catalog)
    {
        STORE.install(catalog);
    }
}
