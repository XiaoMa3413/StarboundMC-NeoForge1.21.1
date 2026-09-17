package com.starboundmc;

import com.starboundmc.world.ShipDimensions;
import com.starboundmc.world.universe.UniverseDatagen;
import net.minecraft.core.RegistrySetBuilder;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * The mod's single datagen entry point.
 *
 * <p>Every datapack registry the mod owns is contributed to one
 * {@link RegistrySetBuilder} and registered with one
 * {@code createDatapackRegistryObjects} call.</p>
 *
 * <p>That single call is a hard requirement, not a style choice: NeoForge names
 * the provider "Registries" with no way to override it, so a second call from
 * the same mod aborts datagen with a duplicate-provider error. Composing the
 * builders here keeps each module owning its own bootstraps while the mod
 * registers them exactly once.</p>
 */
public final class ModDatagen
{
    public static final RegistrySetBuilder BUILDER =
            UniverseDatagen.applyTo(ShipDimensions.applyTo(new RegistrySetBuilder()));

    private ModDatagen()
    {
    }

    public static void register(GatherDataEvent event)
    {
        event.createDatapackRegistryObjects(BUILDER);
    }
}
