package com.starboundmc;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        assertTrue(root.contains("StarmapLevel.GALAXY"));
        assertTrue(root.contains("StarmapLevel.SYSTEM"));
        assertTrue(root.contains("StarmapLevel.PLANET"));
        assertTrue(root.contains("GLFW_MOUSE_BUTTON_RIGHT"));
        assertTrue(root.contains("drawDashedLine"));
        assertTrue(screen.contains("GLFW_KEY_ESCAPE"));
    }
}
