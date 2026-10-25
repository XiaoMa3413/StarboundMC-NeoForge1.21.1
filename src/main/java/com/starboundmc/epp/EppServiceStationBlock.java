// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The station supplies tools; each player keeps a separate, temporary service tray. */
public final class EppServiceStationBlock extends Block {
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = com.starboundmc.block.MachineryShapes.boxes(
            new double[]{1,0,1,4,1.6,4}, new double[]{12,0,1,15,1.6,4},
            new double[]{1,0,12,4,1.6,15}, new double[]{12,0,12,15,1.6,15},
            new double[]{1.2,1.4,2.5,14.8,7.2,14.7}, new double[]{.6,7,1.2,15.4,8.4,15},
            new double[]{3.2,8.4,9.6,12.8,15.7,14.2}, new double[]{1.1,8.4,6.5,4,12.1,9.1},
            new double[]{12.5,8.4,6.8,14.2,11.8,8.4});
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }
    public EppServiceStationBlock(Properties properties) { super(properties); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && !server.isSpectator())
            server.openMenu(new SimpleMenuProvider((id, inv, owner) -> new EppServiceMenu(id, inv, pos),
                    Component.translatable("block.starboundmc.epp_service_station")), pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
