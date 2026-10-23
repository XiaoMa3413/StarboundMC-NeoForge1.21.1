package com.starboundmc.weapon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponProfileTest {
    @Test void shortChargeIsCancelledWithoutDamage() {
        for (int tick = -1; tick < 10; tick++) assertEquals(0, WeaponProfile.STARCORE_RAILGUN.chargedDamage(tick));
    }
    @Test void railgunChargeIsMonotonicAndCapped() {
        var gun = WeaponProfile.STARCORE_RAILGUN;
        assertEquals(18, gun.chargedDamage(10), .001);
        assertEquals(30, gun.chargedDamage(20), .001);
        assertEquals(30, gun.chargedDamage(72000), .001);
        for (int tick = 11; tick <= 20; tick++) assertTrue(gun.chargedDamage(tick) > gun.chargedDamage(tick - 1));
    }
}
