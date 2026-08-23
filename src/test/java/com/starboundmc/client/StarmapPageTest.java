package com.starboundmc.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarmapPageTest
{
    @Test
    void systemAndBodyFocusShareTheSystemContext()
    {
        assertFalse(StarmapPage.GALAXY.showsSystemContext());
        assertTrue(StarmapPage.SYSTEM.showsSystemContext());
        assertTrue(StarmapPage.BODY_FOCUS.showsSystemContext());
    }
}
