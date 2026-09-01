package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import com.starboundmc.economy.VoxelWalletService;
import com.starboundmc.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Voxel item-to-wallet conversion: picked-up voxel items deposit straight
 * into the player wallet instead of occupying inventory space.
 */
@EventBusSubscriber(modid = StarboundMC.MODID)
public final class VoxelWalletEvents {
    private VoxelWalletEvents() {
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!event.getItemEntity().getItem().is(ModItems.VOXEL)) {
            return;
        }
        int count = event.getItemEntity().getItem().getCount();
        if (count <= 0) {
            return;
        }
        VoxelWalletService.add(player, count);
        event.getItemEntity().discard();
        event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.1F,
                0.5F + player.getRandom().nextFloat() * 0.2F);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VoxelWalletService.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VoxelWalletService.sync(player);
        }
    }
}
