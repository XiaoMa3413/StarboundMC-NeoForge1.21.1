// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.encounter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Predicate;

/** Bounded connected-structure survey, directional support and deterministic candidate search. */
public final class RelayGeometry {
    public static final int WIDTH = 31, HEIGHT = 17, DEPTH = 31, MARGIN = 8;
    public static final int SCAN_RADIUS = 128, MAX_BLOCKS = 32768;
    public static final double MAX_DISTANCE = 160, EVA_GAP = 80;
    public static final Vec3 SHIP_CENTER = new Vec3(.5, 103, .5);
    private RelayGeometry() { }
    public static AABB bounds(BlockPos origin) { return new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + WIDTH, origin.getY() + HEIGHT, origin.getZ() + DEPTH); }
    public static Vec3 center(BlockPos origin) { return bounds(origin).getCenter(); }
    public static List<BlockPos> survey(Predicate<BlockPos> occupied) {
        var visited = new HashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        var blocks = new ArrayList<BlockPos>();
        for (BlockPos p : BlockPos.betweenClosed(-11, 100, -15, 11, 110, 17)) {
            var copy = p.immutable();
            if (occupied.test(copy) && visited.add(copy)) queue.add(copy);
        }
        while (!queue.isEmpty()) {
            var p = queue.removeFirst(); blocks.add(p);
            if (blocks.size() > MAX_BLOCKS || Math.abs(p.getX()) >= SCAN_RADIUS
                    || Math.abs(p.getZ()) >= SCAN_RADIUS || Math.abs(p.getY() - 103) >= SCAN_RADIUS)
                throw new IllegalStateException("ship_too_large");
            for (Direction d : Direction.values()) {
                var neighbor = p.relative(d);
                if (visited.add(neighbor) && occupied.test(neighbor)) queue.add(neighbor);
            }
        }
        if (blocks.isEmpty()) throw new IllegalStateException("ship_missing");
        return List.copyOf(blocks);
    }
    public static double extent(List<BlockPos> blocks, Vec3 direction) {
        double extent = 0;
        for (var p : blocks) extent = Math.max(extent, Vec3.atCenterOf(p).subtract(SHIP_CENTER).dot(direction)
                + .5 * (Math.abs(direction.x) + Math.abs(direction.y) + Math.abs(direction.z)));
        return extent;
    }
    public static List<BlockPos> candidates(List<BlockPos> ship) {
        return candidates(ship, EVA_GAP);
    }
    public static List<BlockPos> candidates(List<BlockPos> ship, double gap) {
        if (!Double.isFinite(gap) || gap < 24 || gap > 120) throw new IllegalArgumentException("EVA gap must be 24..120");
        var directions = new ArrayList<Vec3>();
        for (double yaw : new double[]{0, 25, -25, 60, -60, 90, -90}) {
            double r = Math.toRadians(yaw); directions.add(new Vec3(Math.sin(r), 0, Math.cos(r)));
        }
        directions.add(new Vec3(0, .35, 1).normalize()); directions.add(new Vec3(0, -.35, 1).normalize());
        var result = new LinkedHashSet<BlockPos>();
        // Try every direction at the preferred gap before searching farther out.
        // Half-block rounding has <= sqrt(3)/2 projection error; one block keeps
        // the requested envelope separation even on diagonal candidates.
        for (double extra = 0; extra <= MAX_DISTANCE; extra += 16) for (var d : directions) {
            double station = .5 * (WIDTH * Math.abs(d.x) + HEIGHT * Math.abs(d.y) + DEPTH * Math.abs(d.z));
            double distance = extent(ship, d) + station + gap + extra + 1;
            if (distance > MAX_DISTANCE + 1) continue;
            var raw = SHIP_CENTER.add(d.scale(distance)).subtract(WIDTH / 2.0, HEIGHT / 2.0, DEPTH / 2.0);
            var origin = new BlockPos((int)Math.round(raw.x), (int)Math.round(raw.y), (int)Math.round(raw.z));
            if (center(origin).distanceTo(SHIP_CENTER) <= MAX_DISTANCE) result.add(origin);
        }
        return List.copyOf(result);
    }
    public static boolean clear(BlockPos origin, int margin, Predicate<BlockPos> occupied) {
        for (var pos : BlockPos.betweenClosed(origin.offset(-margin, -margin, -margin),
                origin.offset(WIDTH - 1 + margin, HEIGHT - 1 + margin, DEPTH - 1 + margin)))
            if (occupied.test(pos)) return false;
        return true;
    }
    public static boolean onBoundary(BlockPos relative) {
        return relative.getX() == 0 || relative.getX() == WIDTH - 1 || relative.getY() == 0
                || relative.getY() == HEIGHT - 1 || relative.getZ() == 0 || relative.getZ() == DEPTH - 1;
    }
}
