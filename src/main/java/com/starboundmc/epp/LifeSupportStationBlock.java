// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.mojang.serialization.MapCodec;
import com.starboundmc.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class LifeSupportStationBlock extends BaseEntityBlock {
    public static final MapCodec<LifeSupportStationBlock> CODEC = simpleCodec(LifeSupportStationBlock::new);
    public LifeSupportStationBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LifeSupportStationEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.LIFE_SUPPORT_STATION.get(), LifeSupportStationEntity::tick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && !server.isSpectator() && level.getBlockEntity(pos) instanceof LifeSupportStationEntity station)
            server.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new LifeSupportMenu(id, inventory, station),
                    Component.translatable("block.starboundmc.life_support_station")));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) {
            if (level.getBlockEntity(pos) instanceof LifeSupportStationEntity station) Containers.dropContents(level, pos, station);
            super.onRemove(state, level, pos, next, moving);
        }
    }
}
