package com.starboundmc.weapon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponProfileTest {
    @Test void shortChargeIsCancelledWithoutDamage() {
        for (int tick = -1; tick < 20; tick++) assertEquals(0, WeaponProfile.STARCORE_RAILGUN.chargedDamage(tick));
    }
    @Test void railgunChargeIsMonotonicAndCapped() {
        var gun = WeaponProfile.STARCORE_RAILGUN;
        assertEquals(18, gun.chargedDamage(20), .001);
        assertEquals(30, gun.chargedDamage(40), .001);
        assertEquals(30, gun.chargedDamage(72000), .001);
        for (int tick = 21; tick <= 40; tick++) assertTrue(gun.chargedDamage(tick) > gun.chargedDamage(tick - 1));
    }
}
