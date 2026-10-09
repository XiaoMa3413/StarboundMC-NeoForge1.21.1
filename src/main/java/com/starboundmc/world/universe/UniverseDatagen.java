package com.starboundmc.world.universe;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.worldgen.BootstrapContext;

/** Generates the shipped datapack universe from the built-in definitions. */
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
