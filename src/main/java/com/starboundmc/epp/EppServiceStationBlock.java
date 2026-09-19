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
    public EppServiceStationBlock(Properties properties) { super(properties); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && !server.isSpectator())
            server.openMenu(new SimpleMenuProvider((id, inv, owner) -> new EppServiceMenu(id, inv, pos),
                    Component.translatable("block.starboundmc.epp_service_station")), pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
