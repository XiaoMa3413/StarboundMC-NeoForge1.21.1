package com.starboundmc.client.starmap;

import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import com.starboundmc.world.starmap.StarmapGalaxyGraph;
import com.starboundmc.world.starmap.StarmapGalaxyGraphJson;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/** Loads the current resource-pack version of the deep-space graph per screen open. */
final class StarmapGalaxyGraphResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation GRAPH = ResourceLocation.fromNamespaceAndPath(
            StarboundMC.MODID, "starmap/galaxy_graph.json");

    private StarmapGalaxyGraphResources() {}

    static StarmapGalaxyGraph load() {
        var resource = Minecraft.getInstance().getResourceManager().getResource(GRAPH);
        if (resource.isEmpty()) {
            LOGGER.warn("Starmap galaxy graph {} is missing; using built-in fallback", GRAPH);
            return StarmapGalaxyGraph.fallback();
        }
        try (var reader = resource.get().openAsReader()) {
            return StarmapGalaxyGraphJson.read(reader);
        } catch (Exception exception) {
            LOGGER.error("Failed to load starmap galaxy graph {}; using built-in fallback",
                    GRAPH, exception);
            return StarmapGalaxyGraph.fallback();
        }
    }
}
