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
        int navigation = hud.indexOf("private void drawNavigation");
        int worldAr = hud.indexOf("ArWorldRenderer.INSTANCE.render(g, opacity", navigation);
        int visorDrift = hud.indexOf("g.pose().translate(driftX, driftY, 0)", worldAr);
        assertTrue(navigation >= 0 && worldAr > navigation && visorDrift > worldAr);

        String nova = source("shipai/NovaBroadcastHudLayer.java");
        assertFalse(nova.contains("HudVisor"));
        assertFalse(nova.contains("drift"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(CLIENT.resolve(relativePath));
    }
}
