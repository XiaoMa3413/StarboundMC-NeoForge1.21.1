package com.starboundmc.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Default ship authored in Minecraft and frozen from the approved interior save. */
public final class ShipStructure
{
    public static final int FLOOR_Y = 100;
    public static final int CEIL_Y = 110;
    public static final int MIN_X = -11;
    public static final int MAX_X = 11;
    public static final int MIN_Z = -15;
    public static final int MAX_Z = 17;
    public static final BlockPos TEMPLATE_ORIGIN = new BlockPos(MIN_X, FLOOR_Y, MIN_Z);
    static final String TEMPLATE_RESOURCE = "/data/starboundmc/structure/starter_ship.nbt";

    public static final BlockPos SHIP_TELEPORTER_POS = new BlockPos(0, 101, -7);
    public static final BlockPos SHIP_AI_TERMINAL_POS = new BlockPos(2, 103, 3);
    public static final Direction SHIP_AI_TERMINAL_FACING = Direction.WEST;
    public static final BlockPos SHIP_VOXEL_PRINTING_STATION_POS = new BlockPos(-2, 102, 3);
    public static final Direction SHIP_VOXEL_PRINTING_STATION_FACING = Direction.EAST;
    public static final BlockPos SHIP_CHAIR_POS = new BlockPos(0, 102, 10);
    public static final Direction SHIP_CHAIR_FACING = Direction.SOUTH;

    private ShipStructure() {}

    /** Constants remain usable without loading registries or the template. */
    private static final class Layout
    {
        static final Map<BlockPos, Placement> BLOCKS = readTemplate();
    }

    static Map<BlockPos, Placement> layout() { return Layout.BLOCKS; }

    static List<Map.Entry<BlockPos, Placement>> blocksInChunk(ChunkPos chunk)
    {
        return layout().entrySet().stream().filter(e -> new ChunkPos(e.getKey()).equals(chunk)).toList();
    }

    public static int placeInChunk(ChunkAccess chunk)
    {
        int count = 0;
        for (var entry : blocksInChunk(chunk.getPos()))
        {
            BlockPos pos = entry.getKey();
            Placement placement = entry.getValue();
            BlockState state = placement.state();
            chunk.setBlockState(pos, state, false);
            if (placement.blockEntityTag() != null)
            {
                // Defer deserialization until LevelChunk promotion has registry access.
                // Keep inventory/components rather than replacing them with an empty BE.
                chunk.setBlockEntityNbt(placement.blockEntityTagAt(pos));
            }
            else if (state.getBlock() instanceof EntityBlock entityBlock)
            {
                BlockEntity entity = entityBlock.newBlockEntity(pos, state);
                if (entity != null) chunk.setBlockEntity(entity);
            }
            count++;
        }
        return count;
    }

    private static Map<BlockPos, Placement> readTemplate()
    {
        try (var input = ShipStructure.class.getResourceAsStream(TEMPLATE_RESOURCE))
        {
            if (input == null) throw new IllegalStateException("Missing default ship " + TEMPLATE_RESOURCE);
            CompoundTag template = NbtIo.readCompressed(input, NbtAccounter.create(8 * 1024 * 1024));
            var size = template.getList("size", Tag.TAG_INT);
            if (size.size() != 3 || size.getInt(0) != 23 || size.getInt(1) != 11 || size.getInt(2) != 33)
                throw new IllegalStateException("Default ship template bounds changed");
            if (!template.getList("entities", Tag.TAG_COMPOUND).isEmpty())
                throw new IllegalStateException("Chunk-generated ship does not support template entities");
            var palette = template.getList("palette", Tag.TAG_COMPOUND);
            Map<BlockPos, Placement> blocks = new LinkedHashMap<>();
            for (Tag tag : template.getList("blocks", Tag.TAG_COMPOUND))
            {
                CompoundTag block = (CompoundTag) tag;
                var local = block.getList("pos", Tag.TAG_INT);
                if (local.size() != 3)
                    throw new IllegalStateException("Invalid default ship block coordinate");
                for (int axis = 0; axis < 3; axis++)
                    if (local.getInt(axis) < 0 || local.getInt(axis) >= size.getInt(axis))
                        throw new IllegalStateException("Default ship block outside template");
                int index = block.getInt("state");
                if (index < 0 || index >= palette.size())
                    throw new IllegalStateException("Invalid default ship palette index");
                var state = palette.getCompound(index);
                // This generator starts with empty chunks; omitted air cannot resurrect old geometry.
                if (state.getString("Name").equals("minecraft:air")) continue;
                var pos = TEMPLATE_ORIGIN.offset(local.getInt(0), local.getInt(1), local.getInt(2));
                var placement = new Placement(state.copy(),
                        block.contains("nbt", Tag.TAG_COMPOUND) ? block.getCompound("nbt").copy() : null);
                if (blocks.put(pos, placement) != null)
                    throw new IllegalStateException("Duplicate default ship block at " + pos);
            }
            return Collections.unmodifiableMap(blocks);
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Cannot read default ship template", ex);
        }
    }

    record Placement(CompoundTag stateTag, CompoundTag blockEntityTag)
    {
        BlockState state()
        {
            BlockState result = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag);
            if (!NbtUtils.writeBlockState(result).equals(stateTag))
                throw new IllegalStateException("Unknown or invalid default ship block state: " + stateTag);
            return result;
        }

        CompoundTag blockEntityTagAt(BlockPos pos)
        {
            CompoundTag tag = blockEntityTag.copy();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            tag.remove("keepPacked");
            return tag;
        }
    }
}
