package com.starboundmc.client.starmap;

import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.StarSystemDefinition;
import com.starboundmc.world.universe.UniverseTestSupport;
import com.starboundmc.world.universe.BuiltInUniverse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class StarmapNavigationStateTest {
    @Test
    void levelChangesPreserveContextButNeverCarryASelection() {
        StarSystemDefinition system = UniverseTestSupport.system(BuiltInUniverse.MAIN_SYSTEM_ID);
        CelestialBodyDefinition planet = system.bodies().stream()
                .filter(entry -> !entry.orbit().isMoon())
                .findFirst().orElseThrow();
        CelestialBodyDefinition moon = system.bodies().stream()
                .filter(entry -> entry.orbit().isMoon())
                .findFirst().orElseThrow();

        StarmapNavigationState galaxySelection = new StarmapNavigationState(
                StarmapLevel.GALAXY, system, null, null, false);
        StarmapNavigationState systemPage = galaxySelection.enterSystem(system);
        assertEquals(StarmapLevel.SYSTEM, systemPage.level());
        assertSame(system, systemPage.selectedSystem());
        assertNull(systemPage.selectedEntry());
        assertNull(systemPage.focusedPlanet());
        assertFalse(systemPage.centralStarSelected());

        StarmapNavigationState selectedPlanet = new StarmapNavigationState(
                StarmapLevel.SYSTEM, system, planet, null, false);
        StarmapNavigationState planetPage = selectedPlanet.enterPlanet(planet);
        assertEquals(StarmapLevel.PLANET, planetPage.level());
        assertSame(system, planetPage.selectedSystem());
        assertNull(planetPage.selectedEntry());
        assertSame(planet, planetPage.focusedPlanet());

        assertSame(selectedPlanet, selectedPlanet.enterPlanet(moon));

        StarmapNavigationState selectedMoon = new StarmapNavigationState(
                StarmapLevel.PLANET, system, moon, planet, false);
        StarmapNavigationState returnedSystem = selectedMoon.goBack();
        assertEquals(StarmapLevel.SYSTEM, returnedSystem.level());
        assertSame(system, returnedSystem.selectedSystem());
        assertNull(returnedSystem.selectedEntry());
        assertNull(returnedSystem.focusedPlanet());
        assertFalse(returnedSystem.centralStarSelected());

        StarmapNavigationState selectedStar = new StarmapNavigationState(
                StarmapLevel.SYSTEM, system, null, null, true);
        StarmapNavigationState returnedGalaxy = selectedStar.goBack();
        assertEquals(StarmapLevel.GALAXY, returnedGalaxy.level());
        assertNull(returnedGalaxy.selectedSystem());
        assertNull(returnedGalaxy.selectedEntry());
        assertNull(returnedGalaxy.focusedPlanet());
        assertFalse(returnedGalaxy.centralStarSelected());
    }
}
