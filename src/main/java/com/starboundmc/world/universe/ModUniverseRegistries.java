package com.starboundmc.world.universe;

import com.starboundmc.StarboundMC;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Datapack registries owned by the universe layer.
 *
 * <p>Only one registry exists: {@link #STAR_SYSTEM}. Bodies are owned by their
 * system rather than living in a second registry, which is deliberate — a
 * body's entry id is meaningful only relative to the system that declares it,
 * and three registries referencing each other would make load order and
 * dangling references possible. A system is the unit of authoring, so a system
 * is the unit of registration.</p>
 *
 * <p>The registry is registered with a network codec so it syncs to clients.
 * The client needs the same geometry as the server to replay a flight curve
 * deterministically: star positions, body positions, radii, dock positions and
 * the visual parameters all come from here.</p>
 */
public final class ModUniverseRegistries
{
    /** Namespace for the built-in universe, matching the existing entry ids. */
    public static final ResourceKey<Registry<StarSystemDefinition>> STAR_SYSTEM =
            ResourceKey.createRegistryKey(
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "star_system"));

    private ModUniverseRegistries()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        modEventBus.addListener(ModUniverseRegistries::registerDatapackRegistries);
    }

    private static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event)
    {
        event.dataPackRegistry(STAR_SYSTEM, StarSystemDefinition.CODEC, StarSystemDefinition.CODEC);
    }

    /** Datapack path for one system definition: {@code data/<ns>/starboundmc/star_system/<id>.json}. */
    public static ResourceKey<StarSystemDefinition> systemKey(String systemId)
    {
        return ResourceKey.create(STAR_SYSTEM,
                ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, systemId));
    }
}
