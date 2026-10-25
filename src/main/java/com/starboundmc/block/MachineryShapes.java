package com.starboundmc.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Coarse physical envelopes; tiny bevels and instrument markings need no collision boxes. */
public final class MachineryShapes {
    private MachineryShapes() {}

    public static VoxelShape boxes(double[]... boxes) {
        VoxelShape shape = Shapes.empty();
        for (double[] b : boxes) shape = Shapes.or(shape, Block.box(b[0],b[1],b[2],b[3],b[4],b[5]));
        return shape.optimize();
    }

    public static Map<Direction, VoxelShape> horizontal(VoxelShape north) {
        Map<Direction, VoxelShape> result = new EnumMap<>(Direction.class);
        result.put(Direction.NORTH, north);
        for (Direction direction : new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            VoxelShape shape = Shapes.empty();
            for (var b : north.toAabbs()) {
                var box = switch (direction) {
                    case EAST -> Shapes.box(1-b.maxZ,b.minY,b.minX,1-b.minZ,b.maxY,b.maxX);
                    case SOUTH -> Shapes.box(1-b.maxX,b.minY,1-b.maxZ,1-b.minX,b.maxY,1-b.minZ);
                    default -> Shapes.box(b.minZ,b.minY,1-b.maxX,b.maxZ,b.maxY,1-b.minX);
                };
                shape = Shapes.or(shape, box);
            }
            result.put(direction, shape.optimize());
        }
        return Map.copyOf(result);
    }
}
