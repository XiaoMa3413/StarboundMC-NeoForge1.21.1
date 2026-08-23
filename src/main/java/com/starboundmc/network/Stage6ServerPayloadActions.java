package com.starboundmc.network;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.menu.FuelControllerMenu;
import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.menu.UpgradeMenu;
import com.starboundmc.world.Stage6TravelService;
import com.starboundmc.world.TeleporterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Stage 6 gameplay actions attached to the payload authority boundary. */
public class Stage6ServerPayloadActions implements ServerPayloadActions {
    @Override
    public void upgradeMatterManipulator(ServerPlayer player, int track) {
        if (player.containerMenu instanceof UpgradeMenu menu && menu.stillValid(player)) {
            menu.tryUpgrade(player, track);
        }
    }

    @Override
    public void useTeleporter(ServerPlayer player, BlockPos source, String destinationKey) {
        if (!validOpenTeleporter(player, source)) {
            return;
        }
        if (destinationKey.equals("ship")) {
            Stage6TravelService.teleportToShip(player);
        } else if (destinationKey.equals("planet")) {
            Stage6TravelService.teleportToPlanetSurface(player);
        } else if (destinationKey.startsWith("n|")) {
            TeleporterManager.teleportToNamed(player, destinationKey.substring(2));
        }
    }

    @Override
    public void renameTeleporter(ServerPlayer player, BlockPos source, String name) {
        if (!validOpenTeleporter(player, source)) {
            return;
        }
        MinecraftServer server = player.getServer();
        TeleporterManager.setName(server, player.level().dimension(), source, name);
        ModNetwork.sendToPlayer(player,
                TeleporterListPacketHelper.build(server, player.level().dimension(), source));
    }

    @Override
    public void teleportToShip(ServerPlayer player) {
        if (!player.isSpectator()) {
            Stage6TravelService.teleportToShip(player);
        }
    }

    @Override
    public void addFuel(ServerPlayer player) {
        if (player.containerMenu instanceof FuelControllerMenu menu && menu.stillValid(player)) {
            menu.addAllFuelItems(player);
        }
    }

    private static boolean validOpenTeleporter(ServerPlayer player, BlockPos source) {
        return player.getServer() != null
                && player.containerMenu instanceof TeleporterMenu menu
                && menu.pos.equals(source)
                && menu.stillValid(player)
                && player.level().getBlockState(source).is(ModBlocks.TELEPORTER.get());
    }
}
