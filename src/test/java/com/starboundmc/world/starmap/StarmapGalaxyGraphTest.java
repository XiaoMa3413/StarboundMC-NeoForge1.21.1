package com.starboundmc.world.starmap;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StarmapGalaxyGraphTest {
    @Test
    void bundledResourceDefinesAValidConnectedGraph() throws Exception {
        var stream = StarmapGalaxyGraphTest.class.getResourceAsStream(
                "/assets/starboundmc/starmap/galaxy_graph.json");
        if (stream == null)
            throw new AssertionError("Bundled galaxy graph resource is missing");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            StarmapGalaxyGraph graph = StarmapGalaxyGraphJson.read(reader);
            assertEquals(StarSystems.all().size(), graph.nodes().size());
            assertTrue(graph.isConnected());
        }
    }

    @Test
    void fallbackUsesStableSystemIdsAndAnExplicitConnectedRoute() {
        StarmapGalaxyGraph graph = StarmapGalaxyGraph.fallback();

        assertEquals(StarSystems.all().size(), graph.nodes().size());
        assertEquals(StarSystems.SYS_MAIN, graph.node(StarSystems.SYS_MAIN).id());
        assertEquals(StarSystems.byId(StarSystems.SYS_MAIN).getEntries().size(),
                graph.node(StarSystems.SYS_MAIN).bodyCount());
        assertEquals("main-cold-hyperlane", graph.routes().getFirst().id());
        assertTrue(graph.isConnected());
    }

    @Test
    void jsonOrderDoesNotDefineRouteTopologyAndStatusFieldsArePreserved() {
        String json = """
                {
                  "schema_version": 1,
                  "nodes": [
                    {"system_id":"sys2","x":0.73,"y":0.59,
                     "unlocked":true,"reachable":false},
                    {"system_id":"sys1","x":0.25,"y":0.38}
                  ],
                  "routes": [
                    {"id":"cold-to-main","from":"sys2","to":"sys1",
                     "unlocked":false,"reachable":true}
                  ]
                }
                """;

        StarmapGalaxyGraph graph = StarmapGalaxyGraphJson.read(new StringReader(json));

        assertEquals("sys2", graph.nodes().getFirst().id());
        assertEquals("sys2", graph.routes().getFirst().fromId());
        assertFalse(graph.node("sys2").available());
        assertTrue(graph.node("sys1").available());
        assertFalse(graph.routes().getFirst().available());
        assertTrue(graph.isConnected());
    }

    @Test
    void invalidEndpointsAndMissingSystemsAreRejected() {
        String unknownEndpoint = """
                {"schema_version":1,
                 "nodes":[
                   {"system_id":"sys1","x":0.25,"y":0.38},
                   {"system_id":"sys2","x":0.73,"y":0.59}
                 ],
                 "routes":[{"id":"broken","from":"sys1","to":"missing"}]}
                """;
        String missingSystem = """
                {"schema_version":1,
                 "nodes":[{"system_id":"sys1","x":0.25,"y":0.38}],
                 "routes":[]}
                """;

        assertThrows(IllegalArgumentException.class,
                () -> StarmapGalaxyGraphJson.read(new StringReader(unknownEndpoint)));
        assertThrows(IllegalArgumentException.class,
                () -> StarmapGalaxyGraphJson.read(new StringReader(missingSystem)));
    }

    @Test
    void duplicateUndirectedRoutesAreRejected() {
        StarmapGalaxyGraph.Node main = new StarmapGalaxyGraph.Node(
                StarSystems.SYS_MAIN, StarSystems.byId(StarSystems.SYS_MAIN),
                new GalaxyMapPosition(0.25, 0.38), true, true);
        StarmapGalaxyGraph.Node cold = new StarmapGalaxyGraph.Node(
                StarSystems.SYS_COLD, StarSystems.byId(StarSystems.SYS_COLD),
                new GalaxyMapPosition(0.73, 0.59), true, true);

        assertThrows(IllegalArgumentException.class, () -> StarmapGalaxyGraph.of(
                java.util.List.of(main, cold), java.util.List.of(
                        new StarmapGalaxyGraph.Route("out", "sys1", "sys2", true, true),
                        new StarmapGalaxyGraph.Route("back", "sys2", "sys1", true, true))));
    }
}
