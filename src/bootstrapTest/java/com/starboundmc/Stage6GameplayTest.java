package com.starboundmc;

import com.starboundmc.warp.ShipFuelService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage6GameplayTest {
    @Test
    void fuelAcceptanceIsClampedAndNeverPartialBeyondCapacity() {
        assertEquals(20, ShipFuelService.acceptedAmount(900, 20));
        assertEquals(5, ShipFuelService.acceptedAmount(995, 20));
        assertEquals(0, ShipFuelService.acceptedAmount(1000, 20));
        assertEquals(0, ShipFuelService.acceptedAmount(500, -20));
        assertEquals(ShipFuelService.MAX_FUEL, ShipFuelService.acceptedAmount(-50, 5000));
    }

    @Test
    void realFuelAndFurnaceMenusReplaceStageTwoShells() throws IOException {
        String menus = source("menu/ModMenus.java");
        String handler = source("network/ServerPayloadHandler.java");
        assertTrue(menus.contains("MenuType<FuelControllerMenu>"));
        assertTrue(menus.contains("MenuType<AlloyFurnaceMenu>"));
        assertFalse(menus.contains("Stage2FuelControllerMenu"));
        assertFalse(menus.contains("Stage2AlloyFurnaceMenu"));
        assertTrue(handler.contains("instanceof FuelControllerMenu menu"));
        assertTrue(handler.contains("menu.stillValid(player)"));
    }

    @Test
    void everyStageSixPayloadActionRevalidatesAuthoritativeState() throws IOException {
        String actions = source("network/Stage6ServerPayloadActions.java");
        assertTrue(actions.contains("validOpenTeleporter(player, source)"));
        assertTrue(actions.contains("menu.pos.equals(source)"));
        assertTrue(actions.contains("getBlockState(source).is(ModBlocks.TELEPORTER.get())"));
        assertTrue(actions.contains("instanceof FuelControllerMenu menu && menu.stillValid(player)"));
    }

    @Test
    void unnamedCoordinatesCannotAuthorizeNamedTeleport() throws IOException {
        String manager = source("world/TeleporterManager.java");
        int authorization = manager.indexOf("String registeredName = manager.names.get(key)");
        int parse = manager.indexOf("TeleporterEntry entry = parse(server, key, registeredName)");
        assertTrue(authorization >= 0 && authorization < parse);
        assertTrue(manager.contains("if (registeredName == null)"));
        assertTrue(manager.contains("manager.names.remove(key)"));
    }

    @Test
    void teleporterListsAreBoundedAndRefreshAfterRename() throws IOException {
        String helper = source("network/TeleporterListPacketHelper.java");
        String actions = source("network/Stage6ServerPayloadActions.java");
        assertTrue(helper.contains("PayloadSupport.MAX_LIST_ENTRIES"));
        assertTrue(helper.contains("destinationKey.length() <= PayloadSupport.MAX_ID_LENGTH"));
        assertTrue(actions.contains("TeleporterListPacketHelper.build"));
    }

    @Test
    void gameplayEventsUseNeoForgeAndClientKeysStayClientOnly() throws IOException {
        for (String file : new String[]{
                "event/SpawnHandler.java", "event/NetherPortalHandler.java",
                "client/ModKeyBindings.java", "client/ModKeyEvents.java"}) {
            assertFalse(source(file).contains("net.minecraftforge"), file);
        }
        assertTrue(source("event/NetherPortalHandler.java").contains("event.setCanceled(true)"));
        assertTrue(source("client/ModKeyEvents.java").contains("ClientTickEvent.Post"));
        assertTrue(source("client/ModKeyBindings.java").contains("value = Dist.CLIENT"));
    }

    @Test
    void travelHasSafeFallbackUntilTheShipDimensionIsRestored() throws IOException {
        String travel = source("world/Stage6TravelService.java");
        assertTrue(travel.contains("server.getLevel(SHIP_LEVEL)"));
        assertTrue(travel.contains("teleportToOverworldSpawn(player, false)"));
        assertTrue(travel.contains("overworld.getSharedSpawnPos()"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
    }
}
