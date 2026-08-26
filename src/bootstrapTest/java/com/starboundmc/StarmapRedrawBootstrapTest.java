package com.starboundmc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StarmapRedrawBootstrapTest {
    @Test
    void registersTheNewTerminalBoundary() throws IOException {
        String blocks = Files.readString(Path.of("src/main/java/com/starboundmc/block/ModBlocks.java"));
        String menus = Files.readString(Path.of("src/main/java/com/starboundmc/menu/ModMenus.java"));
        String screens = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/Stage2ClientRegistrar.java"));
        assertTrue(blocks.contains("starmap_terminal"));
        assertTrue(menus.contains("starmap_terminal_menu"));
        assertTrue(screens.contains("StarmapTerminalScreen"));
    }

    @Test
    void ldlibRootContainsThreeLevelsAndBackInput() throws IOException {
        String root = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/starmap/StarmapTerminalRoot.java"));
        String screen = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/starmap/StarmapTerminalScreen.java"));
        String vectors = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/starmap/StarmapVectorDrawing.java"));
        assertTrue(root.contains("StarmapLevel.GALAXY"));
        assertTrue(root.contains("StarmapLevel.SYSTEM"));
        assertTrue(root.contains("StarmapLevel.PLANET"));
        assertTrue(root.contains("GLFW_MOUSE_BUTTON_RIGHT"));
        assertTrue(root.contains("StarmapSceneElement"));
        String scene = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/starmap/StarmapSceneElement.java"));
        String starfield = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/starmap/StarmapStarfieldCache.java"));
        assertTrue(scene.contains("StarmapStarfieldCache"));
        assertFalse(scene.contains("new Random("));
        assertFalse(scene.contains("root.prepareFrame("));
        assertTrue(screen.contains("root.prepareFrame(partialTick)"));
        assertTrue(starfield.contains("getTextureManager().release(location)"));
        assertTrue(vectors.contains("drawDashedLine"));
        assertFalse(root.contains("nearestSystem("));
        assertFalse(root.contains("nearestSystemEntry("));
        assertFalse(root.contains("nearestPlanetTarget("));
        assertTrue(screen.contains("GLFW_KEY_ESCAPE"));
    }

    @Test
    void animationSubscriptionsAreReleasedWithTheElementTree() throws IOException {
        for (String file : new String[] {
                "StarmapInfoPanelElement.java",
                "StarmapSelectionOverlayElement.java",
                "StarmapTransitionOverlayElement.java"
        }) {
            String source = Files.readString(Path.of(
                    "src/main/java/com/starboundmc/client/starmap", file));
            assertTrue(source.contains("UIEvents.REMOVED"), file);
            assertTrue(source.contains(".unsubscribe()"), file);
        }

        String screen = Files.readString(Path.of(
                "src/main/java/com/starboundmc/client/starmap/StarmapTerminalScreen.java"));
        assertTrue(screen.contains("protected void init() {\n        disposeModularUi();"));
        assertTrue(screen.contains("modularUI.onRemoved()"));
        assertTrue(screen.contains("modularUI.setScreen(null)"));
    }
}
