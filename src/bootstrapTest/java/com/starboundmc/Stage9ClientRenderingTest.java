package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage9ClientRenderingTest {
    private static final Path CLIENT = Path.of("src/main/java/com/starboundmc/client");

    @Test
    void clientPackageNoLongerReferencesForgeApis() throws IOException {
        try (Stream<Path> sources = Files.walk(CLIENT)) {
            assertFalse(sources.filter(path -> path.toString().endsWith(".java"))
                    .map(Stage9ClientRenderingTest::read)
                    .anyMatch(source -> source.contains("net.minecraftforge")));
        }
    }

    @Test
    void realStarmapAndWarpOverlayAreRegisteredOnTheClientBus() throws IOException {
        String registrar = source("Stage2ClientRegistrar.java");
        assertTrue(registrar.contains("ShipConsoleScreen::new"));
        assertTrue(registrar.contains("RegisterGuiLayersEvent"));
        assertTrue(registrar.contains("WarpFlashOverlay.FLASH"));
        assertTrue(registrar.contains("RegisterDimensionSpecialEffectsEvent"));
    }

    @Test
    void immediateRenderersUseTheMinecraft121MeshApi() throws IOException {
        for (String renderer : new String[] {
                "LaserRenderer.java", "FrozenSkyRenderer.java", "PlanetRenderer.java",
                "StellarRenderer.java", "StellarPointBatchRenderer.java" }) {
            String source = source(renderer);
            assertFalse(source.contains("getBuilder()"), renderer);
            assertFalse(source.contains(".end()"), renderer);
            assertFalse(source.contains(".vertex("), renderer);
        }
        assertTrue(source("PlanetRenderer.java").contains("POSITION_TEX_COLOR"));
    }

    @Test
    void theEntireClientPackageAndItsLogicTestsParticipateInTheBuild() throws IOException {
        String build = Files.readString(Path.of("build.gradle"));
        assertTrue(build.contains("include 'com/starboundmc/client/*.java'"));
        assertTrue(build.contains("include 'com/starboundmc/client/space/*.java'"));
    }

    private static String source(String name) throws IOException {
        return Files.readString(CLIENT.resolve(name));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
