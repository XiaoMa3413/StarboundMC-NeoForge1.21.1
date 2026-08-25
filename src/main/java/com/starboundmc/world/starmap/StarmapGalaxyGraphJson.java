package com.starboundmc.world.starmap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Strict reader for the resource-pack configurable galaxy graph. */
public final class StarmapGalaxyGraphJson {
    public static final int SCHEMA_VERSION = 1;

    private StarmapGalaxyGraphJson() {}

    public static StarmapGalaxyGraph read(Reader reader) {
        JsonElement parsed = JsonParser.parseReader(reader);
        if (!parsed.isJsonObject())
            throw new IllegalArgumentException("Galaxy graph root must be a JSON object");
        JsonObject root = parsed.getAsJsonObject();
        int version = required(root, "schema_version").getAsInt();
        if (version != SCHEMA_VERSION)
            throw new IllegalArgumentException("Unsupported galaxy graph schema version: " + version);

        List<StarmapGalaxyGraph.Node> nodes = readNodes(requiredArray(root, "nodes"));
        List<StarmapGalaxyGraph.Route> routes = readRoutes(requiredArray(root, "routes"));
        Set<String> configuredSystems = new HashSet<>();
        nodes.forEach(node -> configuredSystems.add(node.id()));
        for (StarSystem system : StarSystems.all()) {
            if (!configuredSystems.contains(system.getSystemId()))
                throw new IllegalArgumentException(
                        "Galaxy graph is missing star system: " + system.getSystemId());
        }
        return StarmapGalaxyGraph.of(nodes, routes);
    }

    private static List<StarmapGalaxyGraph.Node> readNodes(JsonArray values) {
        List<StarmapGalaxyGraph.Node> nodes = new ArrayList<>();
        for (JsonElement value : values) {
            if (!value.isJsonObject())
                throw new IllegalArgumentException("Galaxy node must be a JSON object");
            JsonObject object = value.getAsJsonObject();
            String systemId = required(object, "system_id").getAsString();
            StarSystem system = StarSystems.byId(systemId);
            if (system == null)
                throw new IllegalArgumentException("Unknown star system in galaxy graph: " + systemId);
            double x = required(object, "x").getAsDouble();
            double y = required(object, "y").getAsDouble();
            nodes.add(new StarmapGalaxyGraph.Node(systemId, system,
                    new GalaxyMapPosition(x, y), optionalBoolean(object, "unlocked", true),
                    optionalBoolean(object, "reachable", true)));
        }
        return nodes;
    }

    private static List<StarmapGalaxyGraph.Route> readRoutes(JsonArray values) {
        List<StarmapGalaxyGraph.Route> routes = new ArrayList<>();
        for (JsonElement value : values) {
            if (!value.isJsonObject())
                throw new IllegalArgumentException("Galaxy route must be a JSON object");
            JsonObject object = value.getAsJsonObject();
            routes.add(new StarmapGalaxyGraph.Route(
                    required(object, "id").getAsString(),
                    required(object, "from").getAsString(),
                    required(object, "to").getAsString(),
                    optionalBoolean(object, "unlocked", true),
                    optionalBoolean(object, "reachable", true)));
        }
        return routes;
    }

    private static JsonElement required(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull())
            throw new IllegalArgumentException("Galaxy graph is missing field: " + key);
        return value;
    }

    private static JsonArray requiredArray(JsonObject object, String key) {
        JsonElement value = required(object, key);
        if (!value.isJsonArray())
            throw new IllegalArgumentException("Galaxy graph field must be an array: " + key);
        return value.getAsJsonArray();
    }

    private static boolean optionalBoolean(JsonObject object, String key, boolean fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsBoolean();
    }
}
