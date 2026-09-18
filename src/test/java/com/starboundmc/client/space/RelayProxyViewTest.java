package com.starboundmc.client.space;

import com.starboundmc.encounter.RelayGeometry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RelayProxyViewTest {
    @Test void remoteProxyStaysInsideRenderBudgetAndPreservesAngularSize() {
        var camera = RelayGeometry.SHIP_CENTER;
        var target = camera.add(0, 0, 120);
        var placement = RelayProxyView.placement(camera, target, 8, 4);
        assertEquals(RelayProxyView.renderBudget(4), placement.center().distanceTo(camera), 1.0e-8);
        assertTrue(placement.scale() < 1);
        assertEquals(RelayGeometry.WIDTH / placement.desiredDistance(),
                RelayGeometry.WIDTH * placement.scale() / placement.center().distanceTo(camera), 1.0e-8);
    }

    @Test void nearProxyConvergesExactlyOnReservedAnchor() {
        var camera = RelayGeometry.SHIP_CENTER;
        var target = camera.add(0, 0, 40);
        var placement = RelayProxyView.placement(camera, target, RelayProxyView.approachDistanceFactor(1), 8);
        assertEquals(target, placement.center());
        assertEquals(1, placement.scale(), 1.0e-8);
        assertEquals(8, RelayProxyView.approachDistanceFactor(0), 1.0e-8);
        assertEquals(2.75, RelayProxyView.approachDistanceFactor(.5), 1.0e-8);
    }

    @Test void localHandoffIsSlowerThanRecoveryFromAnUnloadedChunk() {
        float faded = RelayProxyView.advanceAlpha(1, true, 8);
        assertEquals(.5f, faded, 1.0e-6f);
        assertEquals(0, RelayProxyView.advanceAlpha(faded, true, 8), 1.0e-6f);
        assertEquals(1, RelayProxyView.advanceAlpha(0, false, 4), 1.0e-6f);
        assertEquals(.5f, RelayProxyView.advanceAlpha(0, false, 2), 1.0e-6f);
    }
}
