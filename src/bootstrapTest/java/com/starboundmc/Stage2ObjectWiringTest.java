package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage2ObjectWiringTest {
    @Test
    void registersRealSeatAndIndependentMenus() throws IOException {
        String entities = source("entity/ModEntities.java");
        assertTrue(entities.contains("EntityType<SeatEntity>"));
        assertFalse(entities.contains("Stage1SeatEntity"));

        String menus = source("menu/ModMenus.java");
        assertTrue(menus.contains("MenuType<ShipConsoleMenu>"));
        assertTrue(menus.contains("MenuType<ShipCrateMenu>"));
        assertTrue(menus.contains("MenuType<TeleporterMenu>"));
    }

    @Test
    void keepsClientRegistrationsBehindClientBoundary() throws IOException {
        String registrar = source("client/Stage2ClientRegistrar.java");
        assertTrue(registrar.contains("value = Dist.CLIENT"));
        assertTrue(registrar.contains("RegisterMenuScreensEvent"));
        assertTrue(registrar.contains("registerEntityRenderer(ModEntities.SEAT.get()"));
    }

    @Test
    void givesPlacedCrateASavedInventoryShell() throws IOException {
        String blockEntities = source("block/ModBlockEntities.java");
        assertTrue(blockEntities.contains("new SimpleContainer(SLOT_COUNT)"));
        assertTrue(blockEntities.contains("ContainerHelper.loadAllItems"));
        assertTrue(blockEntities.contains("ContainerHelper.saveAllItems"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
    }
}
