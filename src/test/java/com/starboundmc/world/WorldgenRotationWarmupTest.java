package com.starboundmc.world;

import net.minecraft.core.FrontAndTop;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorldgenRotationWarmupTest {
    @Test
    void everyJigsawOrientationCanBeRotatedAfterWarmup() {
        WorldgenRotationWarmup.initialize();

        for (Rotation rotation : Rotation.values()) {
            for (FrontAndTop orientation : FrontAndTop.values()) {
                assertNotNull(rotation.rotation().rotate(orientation),
                        () -> "Missing jigsaw rotation for " + rotation + " / " + orientation);
            }
        }
    }
}
