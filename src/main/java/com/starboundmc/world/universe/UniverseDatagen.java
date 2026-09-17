package com.starboundmc.world.universe;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.worldgen.BootstrapContext;

/**
 * Datagen for the built-in universe (migration step A2).
 *
 * <p>This class only turns {@link BuiltInUniverse} into datapack JSON. The data
 * itself lives in {@code BuiltInUniverse} because it is runtime data too: the
 * client falls back to it before a server registry has been synced.</p>
 */
public final class UniverseDatagen
{
    public static final RegistrySetBuilder BUILDER = applyTo(new RegistrySetBuilder());

    private UniverseDatagen()
    {
    }

    /**
     * Contributes the universe registries to a shared builder.
     *
     * <p>Datapack registries are merged into one builder rather than registered
     * separately because NeoForge's {@code createDatapackRegistryObjects} always
     * names its provider "Registries"; calling it twice from the same mod fails
     * with a duplicate-provider error.</p>
     */
    public static RegistrySetBuilder applyTo(RegistrySetBuilder builder)
    {
        return builder.add(ModUniverseRegistries.STAR_SYSTEM, UniverseDatagen::bootstrap);
    }

    private static void bootstrap(BootstrapContext<StarSystemDefinition> context)
    {
        for (StarSystemDefinition system : BuiltInUniverse.systems())
            context.register(ModUniverseRegistries.systemKey(system.systemId()), system);
    }
}
