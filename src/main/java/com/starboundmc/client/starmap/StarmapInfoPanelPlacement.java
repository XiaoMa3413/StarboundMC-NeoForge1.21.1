package com.starboundmc.client.starmap;

/** Pure geometry for placing the floating information panel around a target. */
final class StarmapInfoPanelPlacement {
    private static final float LEFT_MARGIN = 14.0F;
    private static final float RIGHT_MARGIN = 18.0F;
    private static final float TOP_MARGIN = 38.0F;
    private static final float BOTTOM_MARGIN = 18.0F;
    private static final float TARGET_GAP = 14.0F;
    private static final float SIDE_HYSTERESIS = 8.0F;

    private StarmapInfoPanelPlacement() {
    }

    static Placement place(float frameX, float frameY, float frameWidth, float frameHeight,
                           float panelWidth, float panelHeight, float anchorX, float anchorY,
                           float anchorRadius, Side previousSide) {
        float minX = frameX + LEFT_MARGIN;
        float maxX = Math.max(minX, frameX + frameWidth - RIGHT_MARGIN - panelWidth);
        float minY = frameY + TOP_MARGIN;
        float maxY = Math.max(minY, frameY + frameHeight - BOTTOM_MARGIN - panelHeight);

        Candidate best = null;
        Candidate previous = null;
        for (Side side : Side.values()) {
            Candidate candidate = candidate(side, panelWidth, panelHeight, anchorX, anchorY,
                    anchorRadius, minX, minY, maxX, maxY);
            if (side == previousSide)
                previous = candidate;
            if (best == null || candidate.score < best.score)
                best = candidate;
        }
        if (previous != null && !previous.overlapsTarget
                && previous.clampDistance <= SIDE_HYSTERESIS)
            best = previous;
        return new Placement(best.x, best.y, panelWidth, panelHeight, best.side);
    }

    private static Candidate candidate(Side side, float panelWidth, float panelHeight,
                                       float anchorX, float anchorY, float anchorRadius,
                                       float minX, float minY, float maxX, float maxY) {
        float clearRadius = anchorRadius + TARGET_GAP;
        float rawX;
        float rawY;
        switch (side) {
            case RIGHT -> {
                rawX = anchorX + clearRadius;
                rawY = anchorY - panelHeight * 0.5F;
            }
            case LEFT -> {
                rawX = anchorX - clearRadius - panelWidth;
                rawY = anchorY - panelHeight * 0.5F;
            }
            case BELOW -> {
                rawX = anchorX - panelWidth * 0.5F;
                rawY = anchorY + clearRadius;
            }
            case ABOVE -> {
                rawX = anchorX - panelWidth * 0.5F;
                rawY = anchorY - clearRadius - panelHeight;
            }
            default -> throw new IllegalStateException("Unexpected side: " + side);
        }

        float x = clamp(rawX, minX, maxX);
        float y = clamp(rawY, minY, maxY);
        float clampDistance = Math.abs(rawX - x) + Math.abs(rawY - y);
        float protectedRadius = anchorRadius + 6.0F;
        float overlapWidth = overlap(x, x + panelWidth,
                anchorX - protectedRadius, anchorX + protectedRadius);
        float overlapHeight = overlap(y, y + panelHeight,
                anchorY - protectedRadius, anchorY + protectedRadius);
        float overlapArea = overlapWidth * overlapHeight;
        boolean overlapsTarget = overlapArea > 0.01F;
        float score = overlapArea * 1000.0F + clampDistance * 20.0F + side.preference;
        return new Candidate(x, y, clampDistance, score, overlapsTarget, side);
    }

    private static float overlap(float aMin, float aMax, float bMin, float bMax) {
        return Math.max(0.0F, Math.min(aMax, bMax) - Math.max(aMin, bMin));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    enum Side {
        RIGHT(0.0F),
        LEFT(2.0F),
        BELOW(6.0F),
        ABOVE(8.0F);

        private final float preference;

        Side(float preference) {
            this.preference = preference;
        }
    }

    record Placement(float x, float y, float width, float height, Side side) {
        boolean contains(float pointX, float pointY) {
            return pointX >= x && pointX < x + width
                    && pointY >= y && pointY < y + height;
        }
    }

    private record Candidate(float x, float y, float clampDistance, float score,
                             boolean overlapsTarget, Side side) {
    }
}
