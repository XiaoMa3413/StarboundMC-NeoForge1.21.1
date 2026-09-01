package com.starboundmc.network;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.entity.VoxelPrintingStationBlockEntity;
import com.starboundmc.block.entity.VoxelRefineryBlockEntity;
import com.starboundmc.economy.VoxelWalletService;
import com.starboundmc.menu.VoxelPrintingStationMenu;
import com.starboundmc.menu.VoxelRefineryMenu;
import com.starboundmc.recipe.VoxelPrintingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Voxel machine actions: refinery output is claimed from a public machine
 * buffer; printing deducts wallet voxels up front. Every mutation revalidates
 * the block and bound menu server-side.
 */
final class VoxelMachineActions {
    private VoxelMachineActions() {
    }

    static void startRefinement(ServerPlayer player, BlockPos pos) {
        if (player.level().getBlockEntity(pos) instanceof VoxelRefineryBlockEntity refinery
                && player.level().getBlockState(pos).is(ModBlocks.VOXEL_REFINERY.get())
                && player.containerMenu instanceof VoxelRefineryMenu menu
                && menu.isBoundToBlock() && menu.blockPos().equals(pos) && menu.stillValid(player)
                && !player.isSpectator()) {
            switch (refinery.tryStartRefinement(player.serverLevel())) {
                case STARTED -> {
                    // Progress sync supplies the normal success feedback.
                }
                case BUSY -> player.displayClientMessage(Component.translatable(
                        "message.starboundmc.voxel_refinery.busy"), true);
                case EMPTY_INPUT -> player.displayClientMessage(Component.translatable(
                        "message.starboundmc.voxel_refinery.empty"), true);
                case UNSUPPORTED_INPUT -> player.displayClientMessage(Component.translatable(
                        "message.starboundmc.voxel_refinery.unsupported"), true);
            }
        }
    }

    static void stopRefinement(ServerPlayer player, BlockPos pos) {
        if (player.level().getBlockEntity(pos) instanceof VoxelRefineryBlockEntity refinery
                && player.level().getBlockState(pos).is(ModBlocks.VOXEL_REFINERY.get())
                && player.containerMenu instanceof VoxelRefineryMenu menu
                && menu.isBoundToBlock() && menu.blockPos().equals(pos) && menu.stillValid(player)
                && !player.isSpectator()) {
            refinery.stopRefinement(player.serverLevel());
        }
    }

    static void claimRefinedVoxels(ServerPlayer player, BlockPos pos) {
        if (player.level().getBlockEntity(pos) instanceof VoxelRefineryBlockEntity refinery
                && player.level().getBlockState(pos).is(ModBlocks.VOXEL_REFINERY.get())
                && player.containerMenu instanceof VoxelRefineryMenu menu
                && menu.isBoundToBlock() && menu.blockPos().equals(pos) && menu.stillValid(player)
                && !player.isSpectator()) {
            int claimed = refinery.claimPendingVoxels(player);
            if (claimed > 0) {
                player.displayClientMessage(Component.translatable(
                        "message.starboundmc.voxel_refinery.claimed", claimed), true);
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.starboundmc.voxel_refinery.claim_unavailable"), true);
            }
        }
    }

    static void startPrint(ServerPlayer player, BlockPos pos, ResourceLocation recipeId, int quantity) {
        if (player.level().getBlockEntity(pos) instanceof VoxelPrintingStationBlockEntity station
                && player.level().getBlockState(pos).is(ModBlocks.VOXEL_PRINTING_STATION.get())
                && player.containerMenu instanceof VoxelPrintingStationMenu menu
                && menu.isBoundToBlock() && menu.blockPos().equals(pos) && menu.stillValid(player)
                && !player.isSpectator()) {
            RecipeHolder<VoxelPrintingRecipe> recipe = player.serverLevel().getRecipeManager()
                    .byKey(recipeId)
                    .filter(holder -> holder.value() instanceof VoxelPrintingRecipe)
                    .map(holder -> new RecipeHolder<>(holder.id(), (VoxelPrintingRecipe) holder.value()))
                    .orElse(null);
            if (recipe != null) {
                switch (station.tryEnqueuePrint(player.serverLevel(), player, recipe, quantity)) {
                    case QUEUED -> player.displayClientMessage(Component.translatable(
                            "message.starboundmc.voxel_printing.queued", quantity), true);
                    case INVALID_QUANTITY -> player.displayClientMessage(Component.translatable(
                            "message.starboundmc.voxel_printing.invalid_quantity"), true);
                    case QUEUE_FULL -> player.displayClientMessage(Component.translatable(
                            "message.starboundmc.voxel_printing.queue_full",
                            VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS), true);
                    case MISSING_MATERIALS -> player.displayClientMessage(Component.translatable(
                            "message.starboundmc.voxel_printing.materials"), true);
                    case INSUFFICIENT_VOXELS -> player.displayClientMessage(Component.translatable(
                            "message.starboundmc.voxel_printing.voxels",
                            (long) recipe.value().voxelCost() * quantity,
                            VoxelWalletService.balanceOf(player)), true);
                }
            }
        }
    }

    static void cancelPrintQueue(ServerPlayer player, BlockPos pos, java.util.UUID queueId) {
        if (player.level().getBlockEntity(pos) instanceof VoxelPrintingStationBlockEntity station
                && player.level().getBlockState(pos).is(ModBlocks.VOXEL_PRINTING_STATION.get())
                && player.containerMenu instanceof VoxelPrintingStationMenu menu
                && menu.isBoundToBlock() && menu.blockPos().equals(pos) && menu.stillValid(player)
                && !player.isSpectator()) {
            switch (station.cancelQueuedPrint(player, queueId)) {
                case CANCELLED -> player.displayClientMessage(Component.translatable(
                        "message.starboundmc.voxel_printing.cancelled"), true);
                case NOT_OWNER -> player.displayClientMessage(Component.translatable(
                        "message.starboundmc.voxel_printing.cancel_not_owner"), true);
                case ACTIVE -> player.displayClientMessage(Component.translatable(
                        "message.starboundmc.voxel_printing.cancel_active"), true);
                case NOT_FOUND -> player.displayClientMessage(Component.translatable(
                        "message.starboundmc.voxel_printing.cancel_missing"), true);
            }
        }
    }
}
