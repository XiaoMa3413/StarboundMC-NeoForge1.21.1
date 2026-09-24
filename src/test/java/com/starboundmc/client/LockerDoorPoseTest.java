package com.starboundmc.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LockerDoorPoseTest {
    @Test
    void doorRetreatsAndHologramDisappearsBeforeSliding() {
        assertEquals(new LockerDoorPose(0, 0, 1), LockerDoorPose.at(0));
        var retreat = LockerDoorPose.at(0.1F);
        assertEquals(0, retreat.slide());
        assertTrue(retreat.retreat() > 0);
        var slide = LockerDoorPose.at(0.5F);
        assertEquals(0.65F / 16, slide.retreat());
        assertEquals(0, slide.hologramAlpha());
        assertTrue(slide.slide() > 0);
    }

    @Test
    void travelStaysInsideTheSealedCabinetAndReturnsExactlyHome() {
        float previousSlide = 0;
        for (int tick = 0; tick <= 160; tick++) {
            var pose = LockerDoorPose.at(tick / 160.0F);
            assertTrue(pose.slide() >= previousSlide);
            assertTrue(14.28F / 16 + pose.slide() < 15.5F / 16);
            assertTrue(pose.hologramAlpha() >= 0 && pose.hologramAlpha() <= 1);
            previousSlide = pose.slide();
        }
        assertEquals(1.2F / 16, LockerDoorPose.at(1).slide());
        assertEquals(LockerDoorPose.at(0), LockerDoorPose.at(-1));
        assertEquals(LockerDoorPose.at(1), LockerDoorPose.at(2));
    }
}
