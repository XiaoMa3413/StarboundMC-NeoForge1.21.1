package com.starboundmc;

import com.starboundmc.warp.ShipFuelService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FuelAcceptanceTest {
    @Test
    void fuelAcceptanceIsClampedAndNeverPartialBeyondCapacity() {
        assertEquals(20, ShipFuelService.acceptedAmount(900, 20));
        assertEquals(5, ShipFuelService.acceptedAmount(995, 20));
        assertEquals(0, ShipFuelService.acceptedAmount(1000, 20));
        assertEquals(0, ShipFuelService.acceptedAmount(500, -20));
        assertEquals(ShipFuelService.MAX_FUEL, ShipFuelService.acceptedAmount(-50, 5000));
    }

}
