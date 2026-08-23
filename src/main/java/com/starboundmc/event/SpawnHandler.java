package com.starboundmc.event;

import com.starboundmc.StarboundMC;
import com.starboundmc.item.ModItems;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StarboundMC.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
            ShipDimensions.teleportToShip(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        if (event.isEndConquered())
            return;
        if (event.getEntity() instanceof ServerPlayer player && player.getRespawnPosition() == null)
        {
            ShipDimensions.teleportToShip(player);
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
