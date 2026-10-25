package com.starboundmc;

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
        assertEquals("neoforge:obj", model("ship_door_open.json").get("loader").getAsString());
        var vertices = vertices("ship_door_open.obj");
        int faces = 0;
        for (String line : Files.readAllLines(MODELS.resolve("ship_door_open.obj"))) {
            if (!line.startsWith("f ")) continue;
            double minX = 1, maxX = 0;
            for (String corner : line.substring(2).split(" ")) {
                double x = vertices.get(Integer.parseInt(corner.split("/")[0])-1)[0];
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            }
            assertTrue(maxX <= .125001 || minX >= .874999, "Each face must stay inside a side cassette");
            faces++;
        }
        assertTrue(faces > 0);
    }

    @Test
    void closedModelIsAThinBulkheadInsteadOfAFullCube() throws IOException {
        assertEquals("neoforge:obj", model("ship_door.json").get("loader").getAsString());
        var vertices = vertices("ship_door.obj");
        assertTrue(vertices.stream().anyMatch(v -> v[0] > .4 && v[0] < .6), "Closed door needs center panels");
        for (var v : vertices) assertTrue(v[2] >= 4.75/16 && v[2] <= 11.01/16, "Bulkhead must remain thin");
    }

    private static java.util.List<double[]> vertices(String name) throws IOException {
        return Files.readAllLines(MODELS.resolve(name)).stream().filter(line -> line.startsWith("v "))
                .map(line -> java.util.Arrays.stream(line.substring(2).split(" ")).mapToDouble(Double::parseDouble).toArray()).toList();
    }

    private static JsonObject model(String name) throws IOException {
        return JsonParser.parseString(Files.readString(MODELS.resolve(name))).getAsJsonObject();
    }
}
