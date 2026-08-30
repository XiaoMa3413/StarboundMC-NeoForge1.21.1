package com.starboundmc.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipStructurePlacementTest
{
    @Test
    void placesTheAiTerminalAtTheSharedCabinEdge()
    {
        assertEquals(new BlockPos(0, ShipStructure.FLOOR_Y + 1, 3),
                ShipStructure.SHIP_AI_TERMINAL_POS);
        assertNotEquals(ShipStructure.SHIP_TELEPORTER_POS, ShipStructure.SHIP_AI_TERMINAL_POS);
        assertTrue(StarterShipHullProfile.isCoreCabinColumn(
                ShipStructure.SHIP_AI_TERMINAL_POS.getX(), ShipStructure.SHIP_AI_TERMINAL_POS.getZ()));
        assertEquals(Direction.SOUTH, ShipStructure.SHIP_AI_TERMINAL_FACING);
    }
}
