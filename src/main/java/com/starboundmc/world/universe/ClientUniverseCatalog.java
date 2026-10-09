package com.starboundmc.world.universe;

import net.minecraft.core.RegistryAccess;

/** Client universe projection. A synchronized registry replaces the built-in session baseline.
 * Disconnect resets the projection so one server's definitions cannot leak into the next session. */
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
