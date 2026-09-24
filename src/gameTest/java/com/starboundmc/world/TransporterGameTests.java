package com.starboundmc.world;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.TransporterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class TransporterGameTests {
    @GameTest(template = "shuttle_test_empty")
    public static void explosionAndRemovalRespectSingleItemOwnership(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        for (int part = 0; part < 3; part++) {
            level.setBlockAndUpdate(pos, ModBlocks.TELEPORTER.get().defaultBlockState());
            h.assertTrue(TransporterBlock.ensureAssembly(level, pos), "Assembly failed");
            var explosion = new Explosion(level, null, pos.getX(), pos.getY(), pos.getZ(), 2,
                    false, Explosion.BlockInteraction.DESTROY);
            int[] drops = {0};
            var hit = pos.above(part);
            level.getBlockState(hit).onExplosionHit(level, hit, explosion,
                    (stack, ignored) -> { if (stack.is(ModBlocks.TELEPORTER.get().asItem())) drops[0] += stack.getCount(); });
            h.assertTrue(drops[0] == 1, "Explosion must produce one item from part " + part);
            for (int p = 0; p < 3; p++) h.assertTrue(level.isEmptyBlock(pos.above(p)), "Explosion left a sibling");
        }
        level.setBlockAndUpdate(pos, ModBlocks.TELEPORTER.get().defaultBlockState());
        TransporterBlock.ensureAssembly(level, pos);
        level.destroyBlock(pos.above(), false);
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).isEmpty(),
                "No-drop destruction created sibling loot");
        level.setBlockAndUpdate(pos, ModBlocks.TELEPORTER.get().defaultBlockState());
        TransporterBlock.ensureAssembly(level, pos);
        level.setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
        h.assertTrue(level.getBlockState(pos.above()).is(Blocks.STONE)
                        && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above(2)),
                "Replacement was overwritten or left a partial assembly");
        h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void orphanPartRemovesItselfWithoutDestroyingNeighbours(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 2, 1));
        var level = h.getLevel();
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), ModBlocks.TELEPORTER.get().defaultBlockState().setValue(TransporterBlock.PART, 1));
        h.runAfterDelay(4, () -> {
            h.assertTrue(level.isEmptyBlock(pos.above()) && level.getBlockState(pos).is(Blocks.STONE), "Orphan cleanup changed neighbour");
            h.succeed();
        });
    }
}
