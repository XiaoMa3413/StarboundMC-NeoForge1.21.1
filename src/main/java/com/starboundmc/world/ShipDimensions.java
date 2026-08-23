package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public class ShipDimensions
{
    public static final ResourceKey<LevelStem> SHIP_LEVEL_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "ship"));
    public static final ResourceKey<Level> SHIP_LEVEL = Registries.levelStemToLevel(SHIP_LEVEL_STEM);
    public static final ResourceKey<DimensionType> SHIP_DIMENSION_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "ship"));
    public static final ResourceKey<Biome> SHIP_BIOME =
            ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "ship"));

    public static final BlockPos SHIP_POS = new BlockPos(0, 102, 0);

    public static void teleportToShip(ServerPlayer player)
    {
        Stage6TravelService.teleportToShip(player);
    }

    /** Destination for the teleporter UI's "ship" entry: on top of the ship's teleporter. */
    public static BlockPos shipTeleporterDestination(ServerLevel ship)
    {
        if (ship.getBlockState(ShipStructure.SHIP_TELEPORTER_POS).is(ModBlocks.TELEPORTER.get()))
            return ShipStructure.SHIP_TELEPORTER_POS.above();
        return SHIP_POS;
    }

    /**
     * Destination for the teleporter UI's "planet surface" entry.
     * Lush uses the overworld; the other reachable planets use their authored
     * dimensions and safe surface scans through the shared travel boundary.
     */
    public static void teleportToPlanetSurface(ServerPlayer player)
    {
        Stage6TravelService.teleportToPlanetSurface(player);
    }
}
