package com.starboundmc.story;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.ShipEngineUnitBlock;
import com.starboundmc.block.entity.ShipEngineBlockEntity;
import com.starboundmc.item.ModItems;
import com.starboundmc.menu.ShipEngineMenu;
import com.starboundmc.warp.ShipStateData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class ShipEngineGameTests {
    @GameTest(template = "shuttle_test_empty")
    public static void thrusterCannotAcceptRepairCore(GameTestHelper h) {
        var state = ModBlocks.SHIP_ENGINE.get().defaultBlockState();
        var thruster = new ShipEngineBlockEntity(BlockPos.ZERO, state);
        h.assertTrue(!thruster.canPlaceItem(0, new ItemStack(ModItems.SUBLIGHT_IGNITION_CORE.get())),
                "Thruster accepts ignition modules");
        h.assertTrue(!ShipStoryService.acceptSublightCore(readyShip(), thruster, 100), "Thruster repairs ship");
        h.succeed();
    }

    private static ShipStateData readyShip() {
        var ship = new ShipStateData();
        ship.beginCoreReboot(0, 1);
        ship.finishCoreRebootIfDue(1);
        ship.activateSurfaceMission();
        ship.completeSurfaceMission();
        ship.beginMineralScan(1, 1);
        ship.advanceMineralScanIfDue(2, 1, 1);
        ship.advanceMineralScanIfDue(3, 1, 1);
        ship.advanceMineralScanIfDue(4, 1, 1);
        return ship;
    }

    @GameTest(template = "shuttle_test_empty")
    public static void coreConsumptionAndSavedIgnition(GameTestHelper h) {
        var state = ModBlocks.SHIP_ENGINE_UNIT.get().defaultBlockState();
        var first = new ShipEngineBlockEntity(BlockPos.ZERO, state);
        var second = new ShipEngineBlockEntity(BlockPos.ZERO.above(), state);
        var ship = readyShip();
        first.setItem(0, new ItemStack(Items.DIAMOND, 3));
        h.assertTrue(!ShipStoryService.acceptSublightCore(ship, first, 100), "Raw diamonds must not repair");
        h.assertTrue(first.getItem(0).getCount() == 3, "Rejected diamonds were consumed");
        first.setItem(0, new ItemStack(ModItems.SUBLIGHT_IGNITION_CORE.get(), 3));
        second.setItem(0, new ItemStack(ModItems.SUBLIGHT_IGNITION_CORE.get(), 2));
        h.assertTrue(!ShipStoryService.acceptSublightCore(new ShipStateData(), first, 100), "Skipped prerequisites");
        h.assertTrue(first.getItem(0).getCount() == 3, "Locked engine consumed core");
        h.assertTrue(ShipStoryService.acceptSublightCore(ship, first, 100), "Core was not accepted");
        h.assertTrue(first.getItem(0).getCount() == 2, "Must consume exactly one core");
        h.assertTrue(ship.getStoryProgress().sublightEngine() == EngineState.IGNITING, "No ignition state");
        h.assertTrue(!ShipStoryService.acceptSublightCore(ship, first, 101), "Duplicate request accepted");
        h.assertTrue(!ShipStoryService.acceptSublightCore(ship, second, 101), "Second engine charged again");
        h.assertTrue(second.getItem(0).getCount() == 2, "Other socket was charged");
        var restoredShip = ShipStateData.load(ship.save(new CompoundTag(), h.getLevel().registryAccess()),
                h.getLevel().registryAccess());
        var restoredSocket = new ShipEngineBlockEntity(BlockPos.ZERO, state);
        restoredSocket.loadWithComponents(first.saveWithFullMetadata(h.getLevel().registryAccess()),
                h.getLevel().registryAccess());
        h.assertTrue(restoredSocket.getItem(0).getCount() == 2, "Socket failed NBT round-trip");
        h.assertTrue(!ShipStoryService.acceptSublightCore(restoredShip, restoredSocket, 150), "Reload charged again");
        h.assertTrue(!restoredShip.finishSublightIgnitionIfDue(159), "Ignition completed early");
        h.assertTrue(restoredShip.finishSublightIgnitionIfDue(160), "Reloaded ignition stalled");
        h.assertTrue(restoredShip.getStoryProgress().canTravelWithinSystem(), "Travel still locked");
        h.assertTrue(!restoredShip.getStoryProgress().canTravelBetweenSystems(), "Hyperdrive accidentally unlocked");
        h.assertTrue(!ShipStoryService.acceptSublightCore(restoredShip, restoredSocket, 200), "Online engine charged again");
        h.assertTrue(restoredSocket.getItem(0).getCount() == 2, "Online engine lost core");
        h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void socketInventoryAndMissingStorageFailsClosed(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        var level = h.getLevel();
        level.setBlock(pos, ModBlocks.SHIP_ENGINE_UNIT.get().defaultBlockState(), 3);
        var engine = ShipEngineUnitBlock.storage(level, pos);
        h.assertTrue(engine != null && engine.isEmpty(), "Placed engine has no usable socket");
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getCenter());
        var menu = new ShipEngineMenu(1, player.getInventory(), engine);
        h.assertTrue(menu.slots.size() == 37, "Duplicate menu slots");
        var core = new ItemStack(ModItems.SUBLIGHT_IGNITION_CORE.get(), 4);
        h.assertTrue(menu.getSlot(0).mayPlace(core), "Repair tag failed to load");
        h.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "Socket accepts raw diamonds");
        h.assertTrue(!engine.canPlaceItem(0, new ItemStack(Items.DIAMOND)), "Hopper bypasses module filter");
        player.getInventory().setItem(9, core);
        menu.quickMoveStack(player, 1);
        h.assertTrue(engine.getItem(0).getCount() == 4, "Shift insertion lost items");
        menu.quickMoveStack(player, 0);
        h.assertTrue(engine.isEmpty() && player.getInventory().countItem(ModItems.SUBLIGHT_IGNITION_CORE.get()) == 4,
                "Shift extraction lost items");
        h.assertTrue(menu.stillValid(player), "Nearby menu invalid");
        player.setPos(pos.getX() + 20, pos.getY(), pos.getZ());
        h.assertTrue(!menu.stillValid(player), "Remote access accepted");
        level.removeBlockEntity(pos);
        h.assertTrue(ShipEngineUnitBlock.storage(level, pos) == null, "Missing storage was silently reconstructed");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        h.assertTrue(!menu.stillValid(player), "Broken engine menu still valid");
        player.discard();
        h.succeed();
    }
}
