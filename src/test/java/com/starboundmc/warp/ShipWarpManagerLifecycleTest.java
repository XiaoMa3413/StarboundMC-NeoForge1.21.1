package com.starboundmc.warp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ShipWarpManagerLifecycleTest {
    @BeforeEach
    @AfterEach
    void resetAuthority() {
        ShipWarpManager.reset();
    }

    @Test
    void authorityQueriesFailBeforeServerInitialization() {
        assertThrows(IllegalStateException.class, ShipWarpManager::currentEntryId);
        assertThrows(IllegalStateException.class, ShipWarpManager::visitedEntries);
        assertThrows(IllegalStateException.class, ShipWarpManager::getFuel);
        assertThrows(IllegalStateException.class, ShipWarpManager::isWarping);
        assertThrows(IllegalStateException.class, ShipWarpManager::unknownBodyEntryId);
        assertThrows(IllegalStateException.class, ShipWarpManager::isStrandedAtUnknownBody);
    }

    @Test
    void mutationCannotSilentlySucceedWithoutAuthority() {
        assertThrows(IllegalStateException.class, () -> ShipWarpManager.addFuel(1, null));
    }
}
