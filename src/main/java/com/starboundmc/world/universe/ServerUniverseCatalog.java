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
    private static final UniverseCatalogStore STORE = new UniverseCatalogStore("Server");

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
