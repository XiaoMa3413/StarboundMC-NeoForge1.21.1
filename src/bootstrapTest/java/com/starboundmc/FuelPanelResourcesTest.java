package com.starboundmc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class FuelPanelResourcesTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/starboundmc");

    @Test
    void fuelPanelObjResolvesMaterialsAndStaysInsideThinWallEnvelope() throws Exception {
        var model = json(ASSETS.resolve("models/block/fuel_controller.json"));
        assertEquals("neoforge:obj", model.get("loader").getAsString());
        assertEquals("starboundmc:models/block/fuel_panel.obj", model.get("model").getAsString());
        assertFalse(model.get("automatic_culling").getAsBoolean());
        String obj = Files.readString(ASSETS.resolve("models/block/fuel_panel.obj"));
        assertTrue(obj.contains("mtllib fuel_panel.mtl"));
        Set<String> used = new HashSet<>(), declared = new HashSet<>();
        int vertices = 0, faces = 0;
        for (String line : obj.lines().toList()) {
            if (line.startsWith("usemtl ")) used.add(line.substring(7));
            if (line.startsWith("f ")) faces++;
            if (line.startsWith("v ")) {
                String[] p = line.split(" +");
                double x = Double.parseDouble(p[1]), y = Double.parseDouble(p[2]), z = Double.parseDouble(p[3]);
                assertTrue(x >= .5 / 16 - 1e-6 && x <= 15.5 / 16 + 1e-6);
                assertTrue(y >= 2.0 / 16 - 1e-6 && y <= 14.0 / 16 + 1e-6);
                assertTrue(z >= 12.35 / 16 - 1e-6 && z <= 1 + 1e-6, "Panel projects too far from wall");
                vertices++;
            }
        }
        assertTrue(vertices > 100 && faces > 100, "Missing exported mesh");
        for (String line : Files.readAllLines(ASSETS.resolve("models/block/fuel_panel.mtl"))) {
            if (line.startsWith("newmtl ")) declared.add(line.substring(7));
            if (line.startsWith("map_Kd ")) {
                String name = line.substring(7).replace("starboundmc:", "");
                assertNotNull(ImageIO.read(ASSETS.resolve("textures/" + name + ".png").toFile()));
            }
        }
        assertEquals(declared, used);
        assertEquals(7, used.size());
    }

    @Test
    void displayHasContinuousUvsAndFrameIsOneConnectedMesh() throws Exception {
        var source = json(Path.of("docs/models/fuel-panel-textured-v1.bbmodel"));
        JsonObject display = null, frame = null;
        int texture = -1;
        var textures = source.getAsJsonArray("textures");
        for (int i = 0; i < textures.size(); i++)
            if (textures.get(i).getAsJsonObject().get("name").getAsString().equals("fuel_panel_screen")) texture = i;
        for (var e : source.getAsJsonArray("elements")) {
            var mesh = e.getAsJsonObject();
            switch (mesh.get("name").getAsString()) {
                case "amber_energy_schematic" -> display = mesh;
                case "continuous_chamfered_outer_frame" -> frame = mesh;
            }
        }
        assertNotNull(display);
        assertNotNull(frame);
        assertTrue(texture >= 0);
        var uvByVertex = new HashMap<String, String>();
        int mappedFaces = 0;
        for (var f : display.getAsJsonObject("faces").asMap().values()) {
            var face = f.getAsJsonObject();
            if (face.get("texture").getAsInt() != texture) continue;
            mappedFaces++;
            for (var entry : face.getAsJsonObject("uv").entrySet()) {
                var prior = uvByVertex.putIfAbsent(entry.getKey(), entry.getValue().toString());
                if (prior != null) assertEquals(prior, entry.getValue().toString());
            }
        }
        assertTrue(mappedFaces > 1 && uvByVertex.size() > 4);
        var edges = new HashMap<String, Set<String>>();
        for (var f : frame.getAsJsonObject("faces").asMap().values()) {
            var vs = f.getAsJsonObject().getAsJsonArray("vertices");
            for (int i = 0; i < vs.size(); i++) {
                String a = vs.get(i).getAsString(), b = vs.get((i + 1) % vs.size()).getAsString();
                edges.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                edges.computeIfAbsent(b, k -> new HashSet<>()).add(a);
            }
        }
        Set<String> seen = new HashSet<>();
        var queue = new java.util.ArrayDeque<String>();
        queue.add(edges.keySet().iterator().next());
        while (!queue.isEmpty()) {
            String v = queue.remove();
            if (seen.add(v)) queue.addAll(edges.get(v));
        }
        assertEquals(frame.getAsJsonObject("vertices").size(), seen.size(), "Frame has disconnected pieces");
        var image = ImageIO.read(ASSETS.resolve("textures/block/fuel_panel_screen.png").toFile());
        assertEquals(128, image.getWidth());
        assertEquals(64, image.getHeight());
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
