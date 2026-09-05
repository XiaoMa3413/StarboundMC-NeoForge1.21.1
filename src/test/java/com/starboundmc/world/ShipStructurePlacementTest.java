package com.starboundmc.world;

import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class ShipStructurePlacementTest
{
    private static final Path ARCHIVE = Path.of("docs/blueprints/shuttle-interior-2026-09-05");

    @Test
    void preservesTheSavedTemplateExceptForTheRequestedStarterSupplies() throws Exception
    {
        try (var input = ShipStructure.class.getResourceAsStream(ShipStructure.TEMPLATE_RESOURCE))
        {
            assertNotNull(input);
            var shipped = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            var archived = NbtIo.readCompressed(ARCHIVE.resolve("shuttle-interior.nbt"), NbtAccounter.unlimitedHeap());
            int suppliedCrates = 0;
            for (Tag tag : shipped.getList("blocks", Tag.TAG_COMPOUND))
            {
                var entity = ((CompoundTag) tag).getCompound("nbt");
                if (entity.getString("id").equals("starboundmc:ship_crate"))
                {
                    assertEquals(starterSupplies(), entity.getList("Items", Tag.TAG_COMPOUND));
                    entity.put("Items", new ListTag());
                    suppliedCrates++;
                }
            }
            assertEquals(1, suppliedCrates);
            assertEquals(archived, shipped, "Only the starter crate inventory may differ from the authored template");
        }
        var expected = new HashMap<BlockPos, ShipStructure.Placement>();
        var snapshot = JsonParser.parseString(Files.readString(ARCHIVE.resolve("blocks.json"))).getAsJsonObject();
        for (var element : snapshot.getAsJsonArray("blocks"))
        {
            var block = element.getAsJsonObject();
            var coordinates = block.getAsJsonArray("pos");
            var pos = new BlockPos(coordinates.get(0).getAsInt(), coordinates.get(1).getAsInt(), coordinates.get(2).getAsInt());
            var state = block.getAsJsonObject("state");
            var stateTag = new CompoundTag();
            stateTag.putString("Name", state.get("Name").getAsString());
            if (state.has("Properties"))
            {
                var properties = new CompoundTag();
                state.getAsJsonObject("Properties").entrySet().forEach(e -> properties.putString(e.getKey(), e.getValue().getAsString()));
                stateTag.put("Properties", properties);
            }
            CompoundTag entity = block.has("nbt_snbt") ? TagParser.parseTag(block.get("nbt_snbt").getAsString()) : null;
            if (entity != null)
            {
                entity.remove("x");
                entity.remove("y");
                entity.remove("z");
                if (entity.getString("id").equals("starboundmc:ship_crate"))
                    entity.put("Items", starterSupplies());
            }
            expected.put(pos, new ShipStructure.Placement(stateTag, entity));
        }
        assertEquals(1178, expected.size());
        assertEquals(expected, ShipStructure.layout(), "Do not rebuild the old procedural interior over the user's layout");
    }

    @Test
    void usesTheAuthoredEquipmentPositionsAndRetainsTheirOrientations()
    {
        assertDevice(ShipStructure.SHIP_TELEPORTER_POS, "teleporter", null);
        assertEquals(new BlockPos(0, 102, -7), ShipStructure.SHIP_TELEPORTER_POS.above());
        assertDevice(ShipStructure.SHIP_AI_TERMINAL_POS, "ship_ai_terminal", ShipStructure.SHIP_AI_TERMINAL_FACING);
        assertDevice(ShipStructure.SHIP_VOXEL_PRINTING_STATION_POS, "voxel_printing_station", ShipStructure.SHIP_VOXEL_PRINTING_STATION_FACING);
        assertDevice(ShipStructure.SHIP_CHAIR_POS, "captain_chair", ShipStructure.SHIP_CHAIR_FACING);
        assertDevice(new BlockPos(0, 102, 12), "starmap_terminal", Direction.NORTH);
        assertDevice(new BlockPos(-2, 102, -10), "fuel_controller", Direction.EAST);
        assertDevice(new BlockPos(-2, 103, 3), "ship_crate", Direction.EAST);
        assertEquals(3, ShipStructure.layout().values().stream().filter(p -> p.blockEntityTag() != null).count());
        assertEquals(1, ShipStructure.layout().values().stream()
                .filter(p -> p.stateTag().getString("Name").equals("starboundmc:ship_crate")).count());
    }

    @Test
    void partitionsTheAuthoredShipAcrossSixChunksWithoutMissingOrDuplicatingBlocks()
    {
        var seen = new HashSet<BlockPos>();
        int occupied = 0;
        for (int x = -2; x <= 1; x++)
            for (int z = -2; z <= 2; z++)
            {
                var entries = ShipStructure.blocksInChunk(new ChunkPos(x, z));
                if (!entries.isEmpty()) occupied++;
                for (var entry : entries)
                {
                    assertEquals(new ChunkPos(x, z), new ChunkPos(entry.getKey()));
                    assertTrue(seen.add(entry.getKey()));
                }
            }
        assertEquals(6, occupied);
        assertEquals(ShipStructure.layout().keySet(), seen);
        assertTrue(seen.contains(new BlockPos(-1, 100, 17)));
        assertTrue(seen.contains(new BlockPos(1, 100, 17)));
        assertFalse(seen.contains(new BlockPos(0, 102, -9)), "Removed bulkhead must stay removed");
    }

    @Test
    void keepsHeadroomAboveTheNewSlabDeckAndTheTeleporterLanding()
    {
        var blocks = ShipStructure.layout();
        for (int z = -8; z <= 9; z++)
            for (int x = -1; x <= 1; x++)
            {
                for (int y = 102; y <= 103; y++)
                    assertFalse(blocks.containsKey(new BlockPos(x, y, z)), "Headroom blocked");
                assertTrue(blocks.containsKey(new BlockPos(x, 101, z)));
            }
        assertFalse(blocks.containsKey(ShipStructure.SHIP_TELEPORTER_POS.above()));
        assertFalse(blocks.containsKey(ShipStructure.SHIP_TELEPORTER_POS.above(2)));
        for (int x = -2; x <= 2; x++)
            assertEquals("minecraft:gray_stained_glass", blocks.get(new BlockPos(x, 103, 13)).stateTag().getString("Name"));
    }

    private static void assertDevice(BlockPos pos, String id, Direction facing)
    {
        var placement = ShipStructure.layout().get(pos);
        assertNotNull(placement, "Missing device at " + pos);
        assertEquals("starboundmc:" + id, placement.stateTag().getString("Name"));
        if (facing != null)
            assertEquals(facing.getName(), placement.stateTag().getCompound("Properties").getString("facing"));
    }

    private static ListTag starterSupplies() throws Exception
    {
        return TagParser.parseTag("{Items:[{id:\"starboundmc:survival_knife\",count:1},{id:\"starboundmc:emergency_food_can\",count:3}]}")
                .getList("Items", Tag.TAG_COMPOUND);
    }
}
