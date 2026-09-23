package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HudLayerArchitectureTest {
    private static final Path CLIENT = Path.of("src/main/java/com/starboundmc/client");

    @Test
    void genericHudOwnsCompositionInsteadOfEppState() throws IOException {
        String registrar = source("Stage2ClientRegistrar.java");
        assertTrue(registrar.contains("StarboundHudLayer.INSTANCE"));
        assertTrue(registrar.contains("\"starbound_hud\""));
        assertFalse(registrar.contains("OxygenHudLayer"));

        String eppState = source("epp/EppClientState.java");
        assertFalse(eppState.contains("HudLayer"));
        assertFalse(eppState.contains(".reset();"));

        String connections = source("ClientConnectionEvents.java");
        assertTrue(connections.contains("StarboundHudLayer.INSTANCE.resetConnectionState()"));

        String renderer = source("hud/ar/ArWorldRenderer.java");
        assertTrue(renderer.contains("new TutorialTargetProvider()"));
    }

    @Test
    void worldArRemainsOutsideVisorDriftAndNovaRemainsFlat() throws IOException {
        String hud = source("hud/StarboundHudLayer.java");
        assertFalse(hud.contains("ArWorldRenderer.INSTANCE.render("));
        String registrar = source("Stage2ClientRegistrar.java");
        assertTrue(registrar.contains("\"world_ar\""));
        assertTrue(registrar.contains("ArWorldRenderer.INSTANCE::render"));
        String ar = source("hud/ar/ArWorldRenderer.java");
        assertFalse(ar.contains("HudVisor"));
        assertFalse(ar.contains("navigationOpacity"));
        assertFalse(ar.contains("SHIP_LEVEL"));
        assertFalse(ar.contains("showEvaNavigation"));
        String memory = source("hud/ar/ArVisualStateCache.java");
        assertFalse(memory.contains("HudVisorMotion"));
        assertFalse(memory.contains("Matrix4f"));

        String nova = source("shipai/NovaBroadcastHudLayer.java");
        assertFalse(nova.contains("HudVisor"));
        assertFalse(nova.contains("drift"));
        String novaRoot = source("shipai/NovaBroadcastHudRoot.java");
        assertFalse(novaRoot.contains("HudVisor"));
        assertFalse(novaRoot.contains("ArWorldRenderer"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(CLIENT.resolve(relativePath));
    }
}
