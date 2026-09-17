package com.starboundmc.world.universe;

import net.minecraft.server.MinecraftServer;

/**
 * The server's universe, built from the registry the save actually loaded
 * (migration step A3, server half).
 *
 * <p>The server is the authority on the universe: it validates warps, charges
 * fuel, unlocks systems and gates story progress. Until this existed, only the
 * client had a catalog, so any server-side question about "where is this body"
 * still had to go through the legacy enum tables.</p>
 *
 * <h2>Lifecycle</h2>
 *
 * <p>Built on {@code ServerStartedEvent} and cleared on
 * {@code ServerStoppedEvent}. Clearing matters because a dedicated server or an
 * integrated one can host more than one world in a single JVM session (returning
 * to the title screen and loading another save); a catalog left over from the
 * previous world would describe systems the current one does not have.</p>
 *
 * <p>Between those two events the catalog reflects the running server's registry,
 * which is what makes a datapack-added system usable by warp logic.</p>
 */
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
