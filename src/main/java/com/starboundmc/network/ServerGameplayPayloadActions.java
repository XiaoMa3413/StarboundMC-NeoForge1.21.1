package com.starboundmc.network;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.menu.FuelControllerMenu;
import com.starboundmc.menu.ShipAiTerminalMenu;
import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.menu.UpgradeMenu;
import com.starboundmc.menu.WarpControlMenu;
import com.starboundmc.story.ShipEnvironmentService;
import com.starboundmc.story.ShipStoryService;
import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.world.ShipTravelService;
import com.starboundmc.world.TeleporterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Current gameplay operations behind the server payload authority boundary. */
public final class ServerGameplayPayloadActions implements ServerPayloadActions {
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
        if (!ShipEnvironmentService.isCoreOnline(player.getServer())) {
            ShipEnvironmentService.sendSnapshot(player, player.containerMenu.containerId);
            return;
        }
        if (destinationKey.equals("ship")) {
            ShipTravelService.teleportToShip(player);
        } else if (destinationKey.equals("planet")) {
            ShipTravelService.teleportToPlanetSurface(player);
        } else if (destinationKey.startsWith("n|")) {
            TeleporterManager.teleportToNamed(player, destinationKey.substring(2));
        }
    }

    @Override
    public void renameTeleporter(ServerPlayer player, BlockPos source, String name) {
        if (!validOpenTeleporter(player, source)) {
            return;
        }
        if (!ShipEnvironmentService.isCoreOnline(player.getServer())) {
            ShipEnvironmentService.sendSnapshot(player, player.containerMenu.containerId);
            return;
        }
        MinecraftServer server = player.getServer();
        TeleporterManager.setName(server, player.level().dimension(), source, name);
        ModNetwork.sendToPlayer(player,
                TeleporterListPacketHelper.build(server, player.level().dimension(), source));
    }

    @Override
    public void teleportToShip(ServerPlayer player) {
        if (com.starboundmc.warp.ShipCrewSafety.blocksDeparture(player)) {
            if (com.starboundmc.epp.EvaEmergencyRecall.canRequest(player)) {
                com.starboundmc.epp.EvaEmergencyRecall.request(player);
                return;
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.starboundmc.eva.return_manually"), true);
            return;
        }
        if (!player.isSpectator() && ShipEnvironmentService.isCoreOnline(player.getServer())) {
            ShipTravelService.teleportToShip(player);
        } else if (!player.isSpectator()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.starboundmc.warp.core_offline"), true);
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
                && menu.isBoundToBlock()
                && menu.pos.equals(source)
                && menu.stillValid(player)
                && player.level().getBlockState(source).is(ModBlocks.TELEPORTER.get());
    }

    @Override
    public void startWarp(ServerPlayer player, String entryId) {
        if (player.containerMenu instanceof WarpControlMenu menu && menu.stillValid(player)) {
            ShipWarpManager.startWarp(player, entryId);
        }
    }

    @Override
    public void shipAiAction(ServerPlayer player, int containerId, long requestId,
                             ShipAiActionPacket.Action action, int argument) {
        if (!player.isSpectator()
                && player.containerMenu instanceof ShipAiTerminalMenu menu
                && menu.containerId == containerId
                && menu.stillValid(player)) {
            ShipStoryService.handleTerminalAction(
                    player, containerId, requestId, action, argument);
        }
    }

    @Override
    public void startRefinement(ServerPlayer player, BlockPos pos) {
        VoxelMachineActions.startRefinement(player, pos);
    }

    @Override
    public void stopRefinement(ServerPlayer player, BlockPos pos) {
        VoxelMachineActions.stopRefinement(player, pos);
    }

    @Override
    public void claimRefinedVoxels(ServerPlayer player, BlockPos pos) {
        VoxelMachineActions.claimRefinedVoxels(player, pos);
    }

    @Override
    public void startPrint(ServerPlayer player, BlockPos pos, ResourceLocation recipeId, int quantity) {
        VoxelMachineActions.startPrint(player, pos, recipeId, quantity);
    }

    @Override
    public void cancelPrintQueue(ServerPlayer player, BlockPos pos, java.util.UUID queueId) {
        VoxelMachineActions.cancelPrintQueue(player, pos, queueId);
    }
}
