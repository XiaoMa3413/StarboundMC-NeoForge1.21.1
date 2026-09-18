// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.encounter;
import com.starboundmc.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class RelayGameTests {
    private static BlockPos site(GameTestHelper h, int offset) { return h.absolutePos(new BlockPos(offset, 150, 80)); }
    @GameTest(template = "shuttle_test_empty", timeoutTicks = 200)
    public static void stationPreservesEditsAndConsumedLootAcrossSaveAndRevisit(GameTestHelper h) {
        var origin = site(h, 0); var level = h.getLevel();
        h.assertTrue(RelayStructure.place(level, origin, new CompoundTag()), "Initial station placement failed");
        var control = (BarrelBlockEntity) level.getBlockEntity(origin.offset(11, 5, 21));
        var supplies = (BarrelBlockEntity) level.getBlockEntity(origin.offset(20, 5, 10));
        h.assertTrue(control.getItem(0).is(ModItems.RELAY_DATA_CORE.get()) && control.getItem(1).is(ModItems.MATTER_MANIPULATOR_MODULE.get()), "Guaranteed technology missing");
        h.assertTrue(supplies.getItem(0).is(ModItems.OXYGEN_CANISTER.get()) && supplies.getItem(1).is(ModItems.OXYGEN_CANISTER.get()), "Guaranteed oxygen missing");
        control.removeItemNoUpdate(0); supplies.removeItemNoUpdate(0);
        control.setItem(5, new ItemStack(Items.DIAMOND, 7));
        var edited = origin.offset(10, 6, 6); level.setBlockAndUpdate(edited, Blocks.GOLD_BLOCK.defaultBlockState());
        var snapshot = RelayStructure.capture(level, origin);
        var saved = new RelayData(); saved.snapshot = snapshot;
        saved = RelayData.load(saved.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
        RelayStructure.clear(level, origin);
        h.assertTrue(level.getEntities(null, RelayGeometry.bounds(origin)).isEmpty(), "Clearing snapshotted barrels duplicated dropped contents");
        h.assertTrue(RelayStructure.place(level, origin, saved.snapshot), "Revisit failed");
        control = (BarrelBlockEntity) level.getBlockEntity(origin.offset(11, 5, 21));
        supplies = (BarrelBlockEntity) level.getBlockEntity(origin.offset(20, 5, 10));
        h.assertTrue(control.getItem(0).isEmpty() && supplies.getItem(0).isEmpty(), "Revisit replenished taken loot");
        h.assertTrue(control.getItem(5).is(Items.DIAMOND) && control.getItem(5).getCount() == 7, "Player container contents lost");
        h.assertTrue(level.getBlockState(edited).is(Blocks.GOLD_BLOCK), "Player block edit lost");
        RelayStructure.clear(level, origin); h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void lateObstacleCancelsMaterializationWithoutReplacingPlayerBlocks(GameTestHelper h) {
        var origin = site(h, 80); var level = h.getLevel(); var obstacle = origin.offset(-2, 6, 5);
        level.setBlockAndUpdate(obstacle, Blocks.DIAMOND_BLOCK.defaultBlockState());
        var data = new RelayData(); data.origin = origin; data.phase = RelayData.Phase.APPROACHING;
        RelayEncounter.materialize(level, data);
        h.assertTrue(data.phase == RelayData.Phase.AVAILABLE, "Late collision did not cancel approach");
        h.assertTrue(level.getBlockState(obstacle).is(Blocks.DIAMOND_BLOCK), "Player obstacle overwritten");
        h.assertTrue(level.getBlockState(origin.offset(8, 4, 6)).isAir(), "Partial station created on collision");
        level.removeBlock(obstacle, false); h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void interruptedPlacementRecoveryIsIdempotentAndRejectsForeignBlocks(GameTestHelper h) {
        var origin = site(h, 160); var level = h.getLevel();
        var template = RelayStructure.template(level, new CompoundTag()).save(new CompoundTag());
        h.assertTrue(RelayStructure.recover(level, origin, template, false), "Fresh journal recovery failed");
        h.assertTrue(RelayStructure.recover(level, origin, template, false), "Idempotent recovery failed");
        var control = (BarrelBlockEntity) level.getBlockEntity(origin.offset(11, 5, 21));
        h.assertTrue(control.getItem(0).getCount() == 1, "Recovery duplicated technology");
        control.setItem(5, new ItemStack(Items.DIAMOND, 7));
        h.assertTrue(!RelayStructure.recover(level, origin, template, true), "Recovery accepted extra player items");
        h.assertTrue(control.getItem(5).getCount() == 7, "Recovery removed extra player items");
        control.removeItemNoUpdate(5);
        control.setItem(0, new ItemStack(Items.DIAMOND));
        h.assertTrue(!RelayStructure.recover(level, origin, template, false), "Changed inventory overwritten by recovery");
        h.assertTrue(control.getItem(0).is(Items.DIAMOND), "Recovery lost changed inventory");
        var modified = RelayStructure.capture(level, origin);
        level.setBlockAndUpdate(origin.offset(4, 5, 4), Blocks.GOLD_BLOCK.defaultBlockState());
        h.assertTrue(!RelayStructure.recover(level, origin, modified, true), "Foreign block removed during recovery");
        h.assertTrue(level.getBlockState(origin.offset(4, 5, 4)).is(Blocks.GOLD_BLOCK), "Foreign block was destroyed");
        level.removeBlock(origin.offset(4, 5, 4), false);
        h.assertTrue(RelayStructure.recover(level, origin, modified, true), "Safe interrupted removal failed");
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void buildBoundaryStopsCleanupBeforeExtensionsCanBeLost(GameTestHelper h) {
        var origin = site(h, 240); var level = h.getLevel();
        h.assertTrue(RelayStructure.place(level, origin, new CompoundTag()), "Station placement failed");
        h.assertTrue(!RelayStructure.touchesBoundary(level, origin), "Authored station touches reserved boundary");
        level.setBlockAndUpdate(origin.offset(0, 8, 15), Blocks.IRON_BLOCK.defaultBlockState());
        h.assertTrue(RelayStructure.touchesBoundary(level, origin), "Player extension not detected");
        RelayStructure.clear(level, origin); h.succeed();
    }
}
