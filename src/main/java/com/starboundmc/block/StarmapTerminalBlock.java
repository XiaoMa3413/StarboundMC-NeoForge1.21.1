package com.starboundmc.block;

import com.starboundmc.menu.StarmapTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;

/** Dedicated entry block for the LDLib2 starmap redraw. */
public final class StarmapTerminalBlock extends Block {
    public static final MapCodec<StarmapTerminalBlock> CODEC = simpleCodec(StarmapTerminalBlock::new);

    public StarmapTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.starboundmc.starmap_terminal");
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                        int containerId, net.minecraft.world.entity.player.Inventory inventory, Player ignored) {
                    return new StarmapTerminalMenu(containerId, inventory,
                            ContainerLevelAccess.create(level, pos));
                }
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
