package com.starboundmc.world.starmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable, explicitly edged data graph used by the deep-space starmap. */
public final class StarmapGalaxyGraph {
    private final List<Node> nodes;
    private final List<Route> routes;
    private final Map<String, Node> nodesById;

    private StarmapGalaxyGraph(List<Node> nodes, List<Route> routes,
                               Map<String, Node> nodesById) {
        this.nodes = List.copyOf(nodes);
        this.routes = List.copyOf(routes);
        this.nodesById = Map.copyOf(nodesById);
    }

    public static StarmapGalaxyGraph of(List<Node> nodes, List<Route> routes) {
        Objects.requireNonNull(nodes, "nodes");
        Objects.requireNonNull(routes, "routes");
        LinkedHashMap<String, Node> byId = new LinkedHashMap<>();
        for (Node node : nodes) {
            Objects.requireNonNull(node, "node");
            if (byId.putIfAbsent(node.id(), node) != null)
                throw new IllegalArgumentException("Duplicate galaxy node id: " + node.id());
        }
        if (byId.isEmpty())
            throw new IllegalArgumentException("Galaxy graph must contain at least one node");

        Set<String> routeIds = new HashSet<>();
        Set<String> endpointPairs = new HashSet<>();
        for (Route route : routes) {
            Objects.requireNonNull(route, "route");
            if (!routeIds.add(route.id()))
                throw new IllegalArgumentException("Duplicate galaxy route id: " + route.id());
            if (!byId.containsKey(route.fromId()) || !byId.containsKey(route.toId()))
                throw new IllegalArgumentException("Unknown endpoint in galaxy route: " + route.id());
            String pair = route.fromId().compareTo(route.toId()) < 0
                    ? route.fromId() + '\u0000' + route.toId()
                    : route.toId() + '\u0000' + route.fromId();
            if (!endpointPairs.add(pair))
                throw new IllegalArgumentException("Duplicate galaxy route endpoints: " + route.id());
        }
        return new StarmapGalaxyGraph(nodes, routes, byId);
    }

    /** Built-in recovery graph used if a client resource is missing or invalid. */
    public static StarmapGalaxyGraph fallback() {
        List<Node> nodes = StarSystems.all().stream()
                .map(system -> new Node(system.getSystemId(), system,
                        system.getGalaxyMapPosition(), true, true))
                .toList();
        List<Route> routes = List.of(new Route("main-cold-hyperlane",
                StarSystems.SYS_MAIN, StarSystems.SYS_COLD, true, true));
        return of(nodes, routes);
    }

    public List<Node> nodes() {
        return nodes;
    }

    public List<Route> routes() {
        return routes;
    }

    public Node node(String id) {
        return id == null ? null : nodesById.get(id);
    }

    public Node node(StarSystem system) {
        return system == null ? null : node(system.getSystemId());
    }

    /** Connectivity ignores access state; it validates the authored topology itself. */
    public boolean isConnected() {
        if (nodes.size() <= 1)
            return true;
        Map<String, List<String>> adjacency = new HashMap<>();
        for (Node node : nodes)
            adjacency.put(node.id(), new ArrayList<>());
        for (Route route : routes) {
            adjacency.get(route.fromId()).add(route.toId());
            adjacency.get(route.toId()).add(route.fromId());
        }
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(nodes.getFirst().id());
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!visited.add(current))
                continue;
            queue.addAll(adjacency.get(current));
        }
        return visited.size() == nodes.size();
    }

    public record Node(String id, StarSystem system, GalaxyMapPosition position,
                       boolean unlocked, boolean reachable) {
        public Node {
            requireId("Galaxy node", id);
            Objects.requireNonNull(system, "system");
            Objects.requireNonNull(position, "position");
            if (!id.equals(system.getSystemId()))
                throw new IllegalArgumentException("Galaxy node id must match its star system id");
        }

        public String nameKey() {
            return system.getNameKey();
        }

        public String descriptionKey() {
            return system.getDescriptionKey();
        }

        public String starTypeKey() {
            return system.getStarTypeKey();
        }

        public int bodyCount() {
            return system.getEntries().size();
        }

        public boolean available() {
            return unlocked && reachable;
        }
    }

    public record Route(String id, String fromId, String toId,
                        boolean unlocked, boolean reachable) {
        public Route {
            requireId("Galaxy route", id);
            requireId("Galaxy route from", fromId);
            requireId("Galaxy route to", toId);
            if (fromId.equals(toId))
                throw new IllegalArgumentException("Galaxy route cannot connect a node to itself");
        }

        public boolean available() {
            return unlocked && reachable;
        }
    }

    private static void requireId(String label, String id) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException(label + " id must not be blank");
    }
}
