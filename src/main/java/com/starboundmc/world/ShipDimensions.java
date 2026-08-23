package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.OptionalLong;

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

    /**
     * Generates the three dynamic-registry resources that define the ship
     * dimension. Keeping these values in code makes schema drift visible when
     * datagen is run against a newer Minecraft API.
     */
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, ShipDimensions::bootstrapDimensionType)
            .add(Registries.BIOME, ShipDimensions::bootstrapBiome)
            .add(Registries.LEVEL_STEM, ShipDimensions::bootstrapLevelStem);

    private static void bootstrapDimensionType(BootstrapContext<DimensionType> context)
    {
        context.register(SHIP_DIMENSION_TYPE, new DimensionType(
                OptionalLong.of(18000L),
                true,
                false,
                false,
                false,
                1.0D,
                true,
                false,
                0,
                256,
                256,
                BlockTags.INFINIBURN_OVERWORLD,
                ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "ship"),
                0.0F,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)));
    }

    private static void bootstrapBiome(BootstrapContext<Biome> context)
    {
        context.register(SHIP_BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .skyColor(0x0A0F32)
                        .fogColor(0x05070F)
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(new BiomeGenerationSettings.Builder(
                        context.lookup(Registries.PLACED_FEATURE),
                        context.lookup(Registries.CONFIGURED_CARVER)).build())
                .build());
    }

    private static void bootstrapLevelStem(BootstrapContext<LevelStem> context)
    {
        context.register(SHIP_LEVEL_STEM, new LevelStem(
                context.lookup(Registries.DIMENSION_TYPE).getOrThrow(SHIP_DIMENSION_TYPE),
                new ShipChunkGenerator(
                        new FixedBiomeSource(context.lookup(Registries.BIOME).getOrThrow(SHIP_BIOME)))));
    }

    public static void registerDatagen(GatherDataEvent event)
    {
        event.createDatapackRegistryObjects(BUILDER);
    }

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
