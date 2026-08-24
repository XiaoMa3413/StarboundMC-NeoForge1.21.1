package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage3NetworkWiringTest {
    @Test
    void registersAllPayloadsWithExplicitDirectionsAndNewVersion() throws IOException {
        String network = source("network/ModNetwork.java");
        assertTrue(network.contains("PROTOCOL_VERSION = \"1\""));
        assertEquals(6, occurrences(network, "playToServer("));
        assertEquals(6, occurrences(network, "playToClient("));
        assertTrue(network.contains("PacketDistributor.sendToServer"));
        assertTrue(network.contains("PacketDistributor.sendToPlayer"));
        assertTrue(network.contains("PacketDistributor.sendToPlayersInDimension"));
    }

    @Test
    void keepsClientEffectsAndServerAuthorityInSeparateHandlers() throws IOException {
        String client = source("network/ClientPayloadHandler.java");
        String server = source("network/ServerPayloadHandler.java");
        assertFalse(client.contains("ServerPlayer"));
        assertTrue(client.contains("ClientPlanetState.setStarState"));
        assertTrue(client.contains("ClientPlanetState.setCurrent"));
        assertTrue(client.contains("ClientPlanetState.startWarp"));
        assertTrue(client.contains("ClientPlanetState.setFuel"));
        assertTrue(client.contains("ClientPlanetState.applyFlightSnapshot"));
        assertTrue(server.contains("instanceof ServerPlayer"));
        assertTrue(server.contains("containerMenu instanceof"));
        assertTrue(server.contains("validDestinationKey"));
    }

    @Test
    void removesLegacyForgeChannelCallsFromAllSources() throws IOException {
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            String sources = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(Stage3NetworkWiringTest::readUnchecked)
                    .reduce("", String::concat);
            assertFalse(sources.contains("net.minecraftforge.network"));
            assertFalse(sources.contains("SimpleChannel"));
            assertFalse(sources.contains("ModNetwork.CHANNEL"));
        }
    }

    private static int occurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
