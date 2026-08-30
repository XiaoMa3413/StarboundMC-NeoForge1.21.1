package com.starboundmc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShipDoorShapeTest {
    private static final Path BLOCKS = Path.of("src/main/java/com/starboundmc/block/Stage2Blocks.java");
    private static final Path REGISTRY = Path.of("src/main/java/com/starboundmc/block/ModBlocks.java");
    private static final Path MODELS = Path.of("src/main/resources/assets/starboundmc/models/block");

    @Test
    void doorUsesStateDependentDirectionalCollisionShapes() throws IOException {
        String blocks = Files.readString(BLOCKS);
        String registry = Files.readString(REGISTRY);

        assertTrue(blocks.contains("OPEN_NORTH_SOUTH"));
        assertTrue(blocks.contains("OPEN_EAST_WEST"));
        assertTrue(blocks.contains("state.getValue(OPEN)"));
        assertTrue(blocks.contains("state.getValue(FACING).getAxis()"));
        assertTrue(blocks.contains("setOpen(level, pos, open)"));
        assertFalse(blocks.contains("ShipDoorBlockEntity::tick"));
        assertTrue(registry.contains("Stage2Blocks.ShipDoor::new,"));
        assertTrue(registry.contains("Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()"));
    }

    @Test
    void openModelContainsOnlySideCassettesAndNoOpaqueCenter() throws IOException {
        JsonArray elements = model("ship_door_open.json").getAsJsonArray("elements");
        assertEquals(2, elements.size());

        for (var element : elements) {
            JsonObject box = element.getAsJsonObject();
            JsonArray from = box.getAsJsonArray("from");
            JsonArray to = box.getAsJsonArray("to");
            double minX = from.get(0).getAsDouble();
            double maxX = to.get(0).getAsDouble();

            assertTrue(maxX <= 2.0 || minX >= 14.0, "open model must leave its center empty");
            assertFalse(isFullCube(from, to), "open model must not contain a full-cube element");
        }
    }

    @Test
    void closedModelIsAThinBulkheadInsteadOfAFullCube() throws IOException {
        JsonArray elements = model("ship_door.json").getAsJsonArray("elements");
        assertEquals(3, elements.size());
        for (var element : elements) {
            JsonObject box = element.getAsJsonObject();
            assertFalse(isFullCube(box.getAsJsonArray("from"), box.getAsJsonArray("to")));
        }
    }

    private static JsonObject model(String name) throws IOException {
        return JsonParser.parseString(Files.readString(MODELS.resolve(name))).getAsJsonObject();
    }

    private static boolean isFullCube(JsonArray from, JsonArray to) {
        return from.get(0).getAsDouble() == 0.0
                && from.get(1).getAsDouble() == 0.0
                && from.get(2).getAsDouble() == 0.0
                && to.get(0).getAsDouble() == 16.0
                && to.get(1).getAsDouble() == 16.0
                && to.get(2).getAsDouble() == 16.0;
    }
}
