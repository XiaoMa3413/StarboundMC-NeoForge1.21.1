// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.encounter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RelayGeometryTest {
    @Test void connectedExtensionsChangeDirectionalEnvelopeWithoutPressureAssumptions() {
        var blocks = new HashSet<BlockPos>();
        for (int x = 0; x <= 80; x++) blocks.add(new BlockPos(x, 103, 0));
        var surveyed = RelayGeometry.survey(blocks::contains);
        assertEquals(81, surveyed.size());
        assertEquals(.5, RelayGeometry.extent(surveyed, new Vec3(0, 0, 1)), 1e-8);
        assertEquals(80.5, RelayGeometry.extent(surveyed, new Vec3(1, 0, 0)), 1e-8);
        var candidates = RelayGeometry.candidates(surveyed);
        assertTrue(RelayGeometry.center(candidates.getFirst()).distanceTo(RelayGeometry.SHIP_CENTER) < 50,
                "Side antenna must not push forward target to global bounding-sphere radius");
    }
    @Test void realBlocksInsideSafetyMarginRejectCandidateIncludingDetachedBuilds() {
        var candidates = RelayGeometry.candidates(List.of(new BlockPos(0, 103, 16)));
        var first = candidates.getFirst(); var obstacle = first.offset(-RelayGeometry.MARGIN, 0, 0);
        assertFalse(RelayGeometry.clear(first, RelayGeometry.MARGIN, obstacle::equals));
        assertTrue(candidates.stream().skip(1).anyMatch(p -> RelayGeometry.clear(p, RelayGeometry.MARGIN, obstacle::equals)));
        assertTrue(candidates.stream().allMatch(p -> RelayGeometry.center(p).distanceTo(RelayGeometry.SHIP_CENTER) <= RelayGeometry.MAX_DISTANCE + 2));
    }
    @Test void oversizeSurveyRefusesInsteadOfTruncatingIntoPlayerStructure() {
        assertThrows(IllegalStateException.class, () -> RelayGeometry.survey(p -> p.getY() == 103 && p.getZ() == 0 && p.getX() >= 0));
        assertThrows(IllegalStateException.class, () -> RelayGeometry.survey(p -> false));
    }
    @Test void reservationBoundaryHasAnExplicitBuildLimit() {
        assertTrue(RelayGeometry.onBoundary(new BlockPos(0, 8, 15)));
        assertTrue(RelayGeometry.onBoundary(new BlockPos(15, 16, 15)));
        assertFalse(RelayGeometry.onBoundary(new BlockPos(1, 8, 15)));
    }
}
