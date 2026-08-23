package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import com.starboundmc.item.ModItems;
import com.starboundmc.world.Stage6TravelService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = StarboundMC.MODID)
public class SpawnHandler
{
    private static final String STARTER_KEY = "starboundmc.starter_given";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player && !player.getPersistentData().getBoolean(STARTER_KEY))
        {
            giveStarterKit(player);
            player.getPersistentData().putBoolean(STARTER_KEY, true);
            Stage6TravelService.teleportToShip(player);
        }
        else if (event.getEntity() instanceof ServerPlayer player)
            Stage6TravelService.syncState(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        if (event.getOriginal().getPersistentData().getBoolean(STARTER_KEY))
            event.getEntity().getPersistentData().putBoolean(STARTER_KEY, true);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (event.isEndConquered())
            return;
        if (event.getEntity() instanceof ServerPlayer player && player.getRespawnPosition() == null)
        {
            Stage6TravelService.teleportToShip(player);
        }
    }

    private static void giveStarterKit(ServerPlayer player)
    {
        give(player, new ItemStack(ModItems.MATTER_MANIPULATOR.get()));
        give(player, new ItemStack(ModItems.TELEPORTER_ITEM.get()));
    }

    private static void give(ServerPlayer player, ItemStack stack)
    {
        if (!player.getInventory().add(stack))
            player.drop(stack, false);
    }
}
