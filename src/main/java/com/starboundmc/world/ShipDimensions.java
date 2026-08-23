package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.TeleporterBlock;
import com.starboundmc.warp.ShipWarpManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
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
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.OptionalLong;
import java.util.Set;

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

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, ShipDimensions::bootstrapDimensionType)
            .add(Registries.BIOME, ShipDimensions::bootstrapBiome)
            .add(Registries.LEVEL_STEM, ShipDimensions::bootstrapLevelStem);

    private static void bootstrapDimensionType(BootstapContext<DimensionType> ctx)
    {
        ctx.register(SHIP_DIMENSION_TYPE, new DimensionType(
                OptionalLong.of(18000L), // fixed time: midnight, starry night
                true,   // hasSkylight
                false,  // hasCeiling
                false,  // ultrawarm
                false,  // natural
                1.0D,   // coordinateScale
                true,   // bedWorks
                false,  // respawnAnchorWorks
                0,      // minY
                256,    // height
                256,    // logicalHeight
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "ship"), // client effects
                0.0F,   // ambientLight
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)));
    }

    private static void bootstrapBiome(BootstapContext<Biome> ctx)
    {
        ctx.register(SHIP_BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5f)
                .downfall(0.0f)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(0x3F76E4)
                        .waterFogColor(0x050533)
                        .skyColor(0x0A0F32)
                        .fogColor(0x05070F)
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(new BiomeGenerationSettings.Builder(
                        ctx.lookup(Registries.PLACED_FEATURE),
                        ctx.lookup(Registries.CONFIGURED_CARVER)).build())
                .build());
    }

    private static void bootstrapLevelStem(BootstapContext<LevelStem> ctx)
    {
        ctx.register(SHIP_LEVEL_STEM, new LevelStem(
                ctx.lookup(Registries.DIMENSION_TYPE).getOrThrow(SHIP_DIMENSION_TYPE),
                new ShipChunkGenerator(
                        new FixedBiomeSource(ctx.lookup(Registries.BIOME).getOrThrow(SHIP_BIOME)))));
    }

    public static void registerDatagen(GatherDataEvent event)
    {
        event.getGenerator().addProvider(true, new DatapackBuiltinEntriesProvider(
                event.getGenerator().getPackOutput(),
                event.getLookupProvider(),
                BUILDER,
                Set.of(StarboundMC.MODID)));
    }

    public static void teleportToShip(ServerPlayer player)
    {
        ServerLevel shipLevel = player.getServer().getLevel(SHIP_LEVEL);
        if (shipLevel == null)
            return;
        player.teleportTo(shipLevel,
                SHIP_POS.getX() + 0.5, SHIP_POS.getY(), SHIP_POS.getZ() + 0.5,
                player.getYRot(), player.getXRot());

        ShipWarpManager.syncToPlayer(player);
    }

    /** Destination for the teleporter UI's "ship" entry: on top of the ship's teleporter. */
    public static BlockPos shipTeleporterDestination(ServerLevel ship)
    {
        if (ship.getBlockState(ShipStructure.SHIP_TELEPORTER_POS).getBlock() instanceof TeleporterBlock)
            return ShipStructure.SHIP_TELEPORTER_POS.above();
        return SHIP_POS;
    }

    /**
     * Destination for the teleporter UI's "planet surface" entry.
     * The other planets are not built yet, so this temporarily sends the player
     * to the overworld (respawn point, or the world spawn when none is set).
     */
    public static void teleportToPlanetSurface(ServerPlayer player)
    {
        // Molten/Frozen/Barren now have their own dimensions; Lush still uses
        // the overworld as a placeholder surface.
        Planet current = ShipWarpManager.getCurrentPlanet();
        if (current == Planet.MOLTEN)
        {
            MoltenPlanet.teleportToMolten(player);
            return;
        }
        if (current == Planet.FROZEN)
        {
            FrozenPlanet.teleportToFrozen(player);
            return;
        }
        if (current == Planet.BARREN)
        {
            BarrenPlanet.teleportToBarren(player);
            return;
        }

        ServerLevel overworld = player.getServer().overworld();
        BlockPos respawn = player.getRespawnPosition();
        if (respawn != null && player.getRespawnDimension().equals(Level.OVERWORLD))
        {
            player.teleportTo(overworld,
                    respawn.getX() + 0.5, respawn.getY() + 0.1, respawn.getZ() + 0.5,
                    player.getRespawnAngle(), 0.0F);
        }
        else
        {
            BlockPos spawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld,
                    spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    overworld.getSharedSpawnAngle(), 0.0F);
        }
    }
}
