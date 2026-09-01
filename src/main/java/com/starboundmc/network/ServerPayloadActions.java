package com.starboundmc.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative business operations invoked only after the network
 * layer has validated the sender and relevant menu. Later migration stages
 * install the real implementations and must revalidate world state, fuel,
 * dimensions, targets and permissions at the point of mutation.
 */
public interface ServerPayloadActions {
    ServerPayloadActions NONE = new ServerPayloadActions() {
    };

    default void upgradeMatterManipulator(ServerPlayer player, int track) {
    }

    default void startWarp(ServerPlayer player, String entryId) {
    }

    default void useTeleporter(ServerPlayer player, BlockPos source, String destinationKey) {
    }

    default void renameTeleporter(ServerPlayer player, BlockPos source, String name) {
    }

    default void teleportToShip(ServerPlayer player) {
    }

    default void addFuel(ServerPlayer player) {
    }

    default void shipAiAction(ServerPlayer player, int containerId, long requestId,
                              ShipAiActionPacket.Action action, int argument) {
    }

    default void startRefinement(ServerPlayer player, BlockPos pos) {
    }

    default void stopRefinement(ServerPlayer player, BlockPos pos) {
    }

    default void claimRefinedVoxels(ServerPlayer player, BlockPos pos) {
    }

    default void startPrint(ServerPlayer player, BlockPos pos,
                            net.minecraft.resources.ResourceLocation recipeId, int quantity) {
    }

    default void cancelPrintQueue(ServerPlayer player, BlockPos pos, java.util.UUID queueId) {
    }
}
