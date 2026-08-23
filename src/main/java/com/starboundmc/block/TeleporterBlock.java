package com.starboundmc.block;

import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.TeleporterListPacketHelper;
import com.starboundmc.world.TeleporterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;

/**
 * The unified teleporter: right-click opens a destination UI (ship, current planet
 * surface, or any other named teleporter). Destinations land on top of the block.
 */
public class TeleporterBlock extends Block
{
    public TeleporterBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (level.isClientSide)
        {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.openMenu(getMenuProvider(state, level, pos));
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    TeleporterListPacketHelper.build(serverPlayer.getServer(), level.dimension(), pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos)
    {
        return new SimpleMenuProvider(
                (containerId, inventory, p) -> new TeleporterMenu(containerId, inventory, ContainerLevelAccess.create(level, pos), pos),
                Component.translatable("container.starboundmc.teleporter"));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving)
    {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getServer() != null)
        {
            TeleporterManager.remove(level.getServer(), level.dimension(), pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
