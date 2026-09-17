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
    void fuelCrystalRefillsFiftyFuel() throws IOException {
        String service = source("warp/ShipFuelService.java");
        assertTrue(service.contains("return 50;"), "fuel crystal must refill 50 fuel");
        assertFalse(service.contains("return 30;"), "the old 30-fuel value must be gone");
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
        assertTrue(actions.contains("ShipEnvironmentService.isCoreOnline"));
        assertTrue(actions.contains("message.starboundmc.warp.core_offline"));
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

    /**
     * Returning to the ship still falls back when the ship dimension is gone, but a
     * surface landing must not.
     *
     * <p>Those are different situations. A missing ship dimension is a broken world
     * and the player needs somewhere safe to go; a missing surface means the body
     * they asked for is not there, and sending them to the overworld instead would
     * relocate them to a planet they never chose — one the story code counts as a
     * surface, so it would also complete the prologue's landing mission. Landings
     * therefore refuse and report, and this pins that difference.</p>
     */
    @Test
    void travelFallsBackForTheShipButRefusesAFailedLanding() throws IOException {
        String travel = source("world/Stage6TravelService.java");
        assertTrue(travel.contains("server.getLevel(SHIP_LEVEL)"));
        // The ship-dimension fallback survives: that is a broken world, not a
        // landing, so there is no better destination than the overworld spawn.
        assertTrue(travel.contains("SurfaceLandingService.teleportToOverworldSpawn(player, false)"));

        // A failed landing must refuse instead. The overworld-spawn call that used to
        // be reachable from the landing path is gone, and the refusal table replaced it.
        assertFalse(travel.contains("SurfaceLandingService.teleportToOverworldSpawn(player, true)"),
                "a failed landing must refuse, not relocate the player to the overworld");
        assertTrue(travel.contains("landingRefusal("),
                "the landing path must consult the refusal table");

        // The story hooks are driven only by a landing that actually happened.
        int landingCall = travel.indexOf("SurfaceLandingService.teleportToSurface(player, body)");
        int storyCall = travel.indexOf("ShipStoryService.onPlanetSurfaceArrival(player)");
        assertTrue(landingCall > 0 && storyCall > landingCall,
                "the surface mission must be driven after a successful landing, not before");

        String landing = source("world/SurfaceLandingService.java");
        assertTrue(landing.contains("overworld.getSharedSpawnPos()"));
        assertTrue(landing.contains("player.getRespawnPosition()"));
    }

    /**
     * A8 removed the per-planet {@code switch}: the destination now comes from the
     * body's surface definition, so a new body needs no branch here.
     */
    @Test
    void surfaceTravelDoesNotSwitchOverThePlanetEnum() throws IOException {
        String travel = source("world/Stage6TravelService.java");
        assertFalse(travel.contains("switch (current)"));
        assertFalse(travel.contains("case MOLTEN"));
        assertFalse(travel.contains("case FROZEN"));
        assertFalse(travel.contains("case BARREN"));
        assertFalse(travel.contains("case LUSH"));
        assertTrue(travel.contains("SurfaceLandingService.teleportToSurface(player, body)"));
        assertTrue(travel.contains("UniverseNavigation.body(currentEntryId)"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
    }
}
