package com.starboundmc.item;

import com.starboundmc.economy.VoxelWalletService;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The voxel: fundamental unit of matter and the mod's common currency.
 * Using the item (right-click) deposits the whole stack into the player
 * wallet; voxels picked up from the ground deposit automatically.
 */
public class VoxelItem extends Item {
    public VoxelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            int count = stack.getCount();
            VoxelWalletService.add(serverPlayer, count);
            stack.shrink(count);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.starboundmc.voxel.unit").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.starboundmc.voxel.currency").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.starboundmc.voxel.wallet").withStyle(ChatFormatting.DARK_GRAY));
    }
}
