package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage4WiringTest {
    @Test
    void registersPersistentAndNetworkSynchronizedUpgradeComponent() throws IOException {
        String components = source("item/ModDataComponents.java");
        assertTrue(components.contains("matter_manipulator_upgrades"));
        assertTrue(components.contains(".persistent(MatterManipulatorUpgrades.CODEC)"));
        assertTrue(components.contains(".networkSynchronized(MatterManipulatorUpgrades.STREAM_CODEC)"));
    }

    @Test
    void replacesDirectStackNbtWithComponentAccess() throws IOException {
        String item = source("item/MatterManipulatorItem.java");
        assertTrue(item.contains("stack.set(ModDataComponents.MATTER_MANIPULATOR_UPGRADES.get()"));
        assertTrue(item.contains("DataComponents.CUSTOM_DATA"));
        assertFalse(item.contains("getOrCreateTag()"));
        assertFalse(item.contains("stack.hasTag()"));
    }

    @Test
    void connectsRealUpgradeMenuToServerAuthorityPort() throws IOException {
        String menus = source("menu/ModMenus.java");
        String actions = source("network/Stage4ServerPayloadActions.java");
        assertTrue(menus.contains("MenuType<UpgradeMenu>"));
        assertFalse(menus.contains("Stage2UpgradeMenu"));
        assertTrue(actions.contains("menu.tryUpgrade(player, track)"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
    }
}
