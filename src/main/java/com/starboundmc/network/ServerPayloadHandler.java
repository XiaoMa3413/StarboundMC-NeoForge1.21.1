package com.starboundmc.network;

import com.starboundmc.menu.FuelControllerMenu;
import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.menu.UpgradeMenu;
import com.starboundmc.menu.WarpControlMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

final class ServerPayloadHandler {
    private ServerPayloadHandler() {
    }

    static void handle(UpgradeMatterManipulatorPacket payload, IPayloadContext context) {
        ServerPlayer player = sender(context);
        int track = payload.track();
        if (player != null && track >= 0 && track <= 3
                && player.containerMenu instanceof UpgradeMenu) {
            ModNetwork.serverActions().upgradeMatterManipulator(player, track);
        }
    }

    static void handle(StartWarpPacket payload, IPayloadContext context) {
        ServerPlayer player = sender(context);
        if (player != null && !payload.entryId().isBlank()
                && player.containerMenu instanceof WarpControlMenu menu
                && menu.stillValid(player)) {
            ModNetwork.serverActions().startWarp(player, payload.entryId());
        }
    }

    static void handle(TeleporterUsePacket payload, IPayloadContext context) {
        ServerPlayer player = sender(context);
        if (player == null || !validDestinationKey(payload.key())
                || !(player.containerMenu instanceof TeleporterMenu menu)
                || !validTeleporterMenu(player, menu)) {
            return;
        }
        ModNetwork.serverActions().useTeleporter(player, menu.pos, payload.key());
    }

    static void handle(TeleporterRenamePacket payload, IPayloadContext context) {
        ServerPlayer player = sender(context);
        if (player == null || !(player.containerMenu instanceof TeleporterMenu menu)
                || !validTeleporterMenu(player, menu)) {
            return;
        }
        ModNetwork.serverActions().renameTeleporter(player, menu.pos, payload.name());
    }

    static void handle(TeleportToShipPacket payload, IPayloadContext context) {
        ServerPlayer player = sender(context);
        if (player != null && !player.isSpectator()) {
            ModNetwork.serverActions().teleportToShip(player);
        }
    }

    static void handle(AddFuelPacket payload, IPayloadContext context) {
        ServerPlayer player = sender(context);
        if (player != null
                && player.containerMenu instanceof FuelControllerMenu menu
                && menu.stillValid(player)) {
            ModNetwork.serverActions().addFuel(player);
        }
    }

    private static ServerPlayer sender(IPayloadContext context) {
        return context.player() instanceof ServerPlayer player ? player : null;
    }

    private static boolean validTeleporterMenu(ServerPlayer player, TeleporterMenu menu) {
        return !menu.pos.equals(BlockPos.ZERO) && menu.stillValid(player);
    }

    private static boolean validDestinationKey(String key) {
        return key.equals("ship") || key.equals("planet")
                || key.startsWith("n|") && key.length() > 2;
    }
}
