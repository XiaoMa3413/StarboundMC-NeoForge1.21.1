// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArLabelLayoutTest {
    @Test
    void eightCoincidentLabelsFitWithoutOverlapAtEitherViewportEdge() {
        for (float anchorY : new float[]{0, 135, 269}) {
            var layout = new ArLabelLayout();
            float[] rows = new float[8];
            for (int i = 0; i < rows.length; i++) {
                assertTrue(layout.place(1, anchorY, 80, 480, 270));
                assertTrue(layout.x() - 80 >= 8);
                assertTrue(layout.y() >= 8 && layout.y() <= 256);
                for (int j = 0; j < i; j++) assertTrue(Math.abs(rows[j] - layout.y()) >= 10);
                rows[i] = layout.y();
            }
        }
    }

    @Test
    void exhaustionSuppressesLowerPriorityTextAndNextFrameReusesBuffer() {
        var layout = new ArLabelLayout();
        assertTrue(layout.place(100, 0, 50, 200, 22));
        assertFalse(layout.place(100, 0, 50, 200, 22));
        layout.clear();
        assertTrue(layout.place(100, 0, 50, 200, 22));
        assertFalse(layout.place(0, 0, 200, 200, 22));
    }
}
