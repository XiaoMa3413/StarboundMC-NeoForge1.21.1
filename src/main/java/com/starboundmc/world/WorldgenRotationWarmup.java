package com.starboundmc.world;

import com.mojang.math.OctahedralGroup;
import net.minecraft.core.Direction;

/**
 * Initializes Minecraft's lazily built structure-rotation lookup tables before
 * parallel chunk generation can use them.
 *
 * <p>Minecraft 1.21.1 publishes each table before it has finished filling it.
 * When several dimensions generate structures concurrently, a worldgen thread
 * can therefore observe a missing direction and crash while rotating a jigsaw
 * block. Common setup runs before any world is opened, making it a safe place
 * to finish all of the tables on one thread.</p>
 */
public final class WorldgenRotationWarmup {
    private static boolean initialized;

    private WorldgenRotationWarmup() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        for (OctahedralGroup rotation : OctahedralGroup.values()) {
            for (Direction direction : Direction.values()) {
                if (rotation.rotate(direction) == null) {
                    throw new IllegalStateException("Incomplete structure rotation table: "
                            + rotation + " / " + direction);
                }
            }
        }
        initialized = true;
    }
}
