package com.starboundmc.world;

import com.mojang.authlib.GameProfile;
import com.starboundmc.entity.SeatEntity;
import com.starboundmc.block.entity.ShipCrateBlockEntity;
import com.starboundmc.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Runs against real registries, generated chunks, collision shapes and block entities. */
@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class ShuttleGameTests
{
    @GameTest(template = "shuttle_test_empty", timeoutTicks = 200)
    public static void generatedShip(GameTestHelper helper)
    {
        // GameTestServer only creates vanilla dimensions. Generate real ProtoChunks
        // with the ship generator, then install their blocks in the isolated test level.
        var ship = helper.getLevel();
        var biomes = ship.registryAccess().registryOrThrow(Registries.BIOME);
        var generator = new ShipChunkGenerator(new FixedBiomeSource(ship.getBiome(BlockPos.ZERO)));
        for (var pos : BlockPos.betweenClosed(-11, 100, -15, 11, 110, 17))
            ship.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        for (int x = -1; x <= 0; x++)
            for (int z = -1; z <= 1; z++)
            {
                var chunk = new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, ship, biomes, null);
                generator.fillFromNoise(Blender.empty(), null, null, chunk).join();
                var promoted = new LevelChunk(ship, chunk, null);
                for (var entry : ShipStructure.blocksInChunk(chunk.getPos()))
                {
                    var state = chunk.getBlockState(entry.getKey());
                    helper.assertTrue(state.equals(entry.getValue().state()), "Wrong ProtoChunk block at " + entry.getKey());
                    if (state.getBlock() instanceof EntityBlock)
                    {
                        var entity = promoted.getBlockEntity(entry.getKey());
                        helper.assertTrue(entity != null, "Missing promoted block entity at " + entry.getKey());
                        var saved = entity.saveWithFullMetadata(ship.registryAccess());
                        var expected = entry.getValue().blockEntityTagAt(entry.getKey());
                        for (String key : expected.getAllKeys())
                            helper.assertTrue(expected.get(key).equals(saved.get(key)), "Lost block entity data: " + key);
                    }
                    ship.setBlock(entry.getKey(), state, 2);
                    if (entry.getValue().blockEntityTag() != null)
                        ship.getBlockEntity(entry.getKey()).loadWithComponents(
                                entry.getValue().blockEntityTagAt(entry.getKey()), ship.registryAccess());
                }
            }
        for (var entry : ShipStructure.layout().entrySet())
        {
            var state = ship.getBlockState(entry.getKey());
            helper.assertTrue(state.equals(entry.getValue().state()), "Wrong generated block at " + entry.getKey());
            if (state.getBlock() instanceof EntityBlock block)
            {
                var entity = ship.getBlockEntity(entry.getKey());
                var expected = block.newBlockEntity(entry.getKey(), state);
                helper.assertTrue(entity != null && entity.getType() == expected.getType(),
                        "Missing or wrong block entity at " + entry.getKey());
            }
            helper.assertTrue(state.canSurvive(ship, entry.getKey()), "Unsupported block at " + entry.getKey());
        }
        for (int z = -8; z <= 9; z++)
            for (int x = -1; x <= 1; x++)
                for (int y = 102; y <= 103; y++)
                    helper.assertTrue(ship.isEmptyBlock(new BlockPos(x, y, z)), "Main passage obstructed");
        var destination = ShipDimensions.shipTeleporterDestination(ship);
        helper.assertTrue(destination.equals(new BlockPos(0, 102, -7)), "Wrong teleporter landing");
        var crate = (ShipCrateBlockEntity) ship.getBlockEntity(new BlockPos(-2, 103, 3));
        helper.assertTrue(crate.getItem(0).is(ModItems.SURVIVAL_KNIFE.get()) && crate.getItem(0).getCount() == 1,
                "Starter crate must contain one survival knife");
        helper.assertTrue(crate.getItem(1).is(ModItems.EMERGENCY_FOOD_CAN.get()) && crate.getItem(1).getCount() == 3,
                "Starter crate must contain three food cans");

        // FakePlayer refuses riding, while the framework login helper fires mod
        // handshake events. A real player with a local connection exercises the
        // chair without pretending a vanilla test connection negotiated mod payloads.
        var player = new ServerPlayer(ship.getServer(), ship,
                new GameProfile(UUID.randomUUID(), "ShuttleTest"), ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(ship.getServer(), connection, player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.setPos(0.5, 102, 9.5);
        var chair = ShipStructure.SHIP_CHAIR_POS;
        ship.getBlockState(chair).useWithoutItem(ship, player,
                new BlockHitResult(Vec3.atCenterOf(chair), Direction.NORTH, chair, false));
        helper.assertTrue(player.getVehicle() instanceof SeatEntity, "Chair did not create a usable seat");
        var seat = (SeatEntity) player.getVehicle();
        seat.positionRider(player);
        helper.assertTrue(Math.abs(player.getEyeY() - 103.52) < 0.001, "Unexpected seated eye: " + player.getEyeY());
        for (int z = 11; z <= 22; z++)
        {
            var state = ship.getBlockState(BlockPos.containing(0.5, player.getEyeY(), z));
            helper.assertTrue(state.isAir() || state.is(Blocks.GRAY_STAINED_GLASS), "Seated forward view blocked");
        }
        var dismount = seat.getDismountLocationForPassenger(player);
        helper.assertTrue(ship.noCollision(player, player.getBoundingBox().move(dismount.subtract(player.position()))),
                "Dismount intersects chair or roof: " + dismount);
        player.stopRiding();
        seat.discard();
        player.discard();
        channel.finishAndReleaseAll();
        helper.succeed();
    }
}
