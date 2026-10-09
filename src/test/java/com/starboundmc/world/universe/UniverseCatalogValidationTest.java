package com.starboundmc.world.universe;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UniverseCatalogValidationTest {
    @Test void stellarCodecRetainsSectorAndLocalCoordinates() {
        var profile = BuiltInUniverse.systems().getFirst().stellarVisual();
        var json = com.starboundmc.world.starmap.StellarVisualProfile.CODEC.encodeStart(JsonOps.INSTANCE, profile).getOrThrow().getAsJsonObject();
        var expected = com.starboundmc.space.UniversePosition.of(new com.starboundmc.space.SectorCoordinate(71, -22, 8), 123, 456, 789);
        json.add("position", com.starboundmc.space.UniversePosition.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow());
        var restored = com.starboundmc.world.starmap.StellarVisualProfile.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(expected, restored.getUniversePosition());
        var encoded = com.starboundmc.world.starmap.StellarVisualProfile.CODEC.encodeStart(JsonOps.INSTANCE, restored).getOrThrow();
        assertEquals(restored, com.starboundmc.world.starmap.StellarVisualProfile.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    @Test void invalidAuthoredVisualResourceFailsAtCatalogConstruction() {
        var json = StarSystemDefinition.CODEC.encodeStart(JsonOps.INSTANCE, BuiltInUniverse.systems().getFirst()).getOrThrow().getAsJsonObject();
        json.getAsJsonArray("bodies").get(0).getAsJsonObject().getAsJsonObject("space_visual")
                .addProperty("texture", "Invalid Resource Name");
        var system = StarSystemDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertThrows(net.minecraft.ResourceLocationException.class, () -> UniverseCatalog.of(List.of(system)));
    }
    @Test void aMapVisualWithoutFlightGeometryIsNotSubmittedToTheSpaceRenderer() {
        var json = StarSystemDefinition.CODEC.encodeStart(JsonOps.INSTANCE, BuiltInUniverse.systems().getFirst()).getOrThrow().getAsJsonObject();
        var body = json.getAsJsonArray("bodies").get(0).getAsJsonObject();
        String id = body.get("entry_id").getAsString();
        body.remove("navigation");
        var system = StarSystemDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        var catalog = UniverseCatalog.of(List.of(system));
        assertTrue(catalog.body(id).orElseThrow().spaceVisual().isPresent());
        assertTrue(catalog.spaceRenderedBodies().stream().noneMatch(b -> b.entryId().equals(id)));
        assertTrue(catalog.spaceRenderedBodies().stream().allMatch(b -> b.navigation().isPresent() && b.spaceVisual().isPresent()));
    }
}
