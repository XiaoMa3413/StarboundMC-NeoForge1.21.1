package com.starboundmc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BootstrapIdentityTest {
    @Test
    void keepsPublishedModId() {
        assertEquals("starboundmc", StarboundMC.MODID);
    }
}
