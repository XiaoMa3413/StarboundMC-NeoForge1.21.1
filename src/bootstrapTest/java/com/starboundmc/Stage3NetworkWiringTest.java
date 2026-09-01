package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage3NetworkWiringTest {
    @Test
    void registersAllPayloadsWithExplicitDirectionsAndNewVersion() throws IOException {
        String network = source("network/ModNetwork.java");
        assertTrue(network.contains("PROTOCOL_VERSION = \"5\""));
        assertEquals(12, occurrences(network, "playToServer("));
        assertEquals(11, occurrences(network, "playToClient("));
        assertTrue(network.contains("ShipEnvironmentSnapshotPacket.TYPE"));
        assertTrue(network.contains("PacketDistributor.sendToServer"));
        assertTrue(network.contains("PacketDistributor.sendToPlayer"));
        assertTrue(network.contains("PacketDistributor.sendToPlayersInDimension"));
        assertTrue(network.contains("PacketDistributor.sendToPlayersTrackingChunk"));
    }

    @Test
    void keepsClientEffectsAndServerAuthorityInSeparateHandlers() throws IOException {
        String client = source("network/ClientPayloadHandler.java");
        String server = source("network/ServerPayloadHandler.java");
        assertFalse(client.contains("ServerPlayer"));
        assertTrue(client.contains("ClientPlanetState.setStarState"));
        assertTrue(client.contains("ClientPlanetState.setCurrent"));
        assertTrue(client.contains("ClientPlanetState.startWarp"));
        assertTrue(client.contains("ClientPlanetState.setFuel"));
        assertTrue(client.contains("ClientPlanetState.applyFlightSnapshot"));
        assertTrue(client.contains("ClientShipStoryState.apply"));
        assertTrue(client.contains("ClientShipEnvironmentState.apply"));
        assertTrue(server.contains("instanceof ServerPlayer"));
        assertTrue(server.contains("containerMenu instanceof"));
        assertTrue(server.contains("menu.containerId == payload.containerId()"));
        assertTrue(server.contains("validDestinationKey"));
    }

    @Test
    void sendsVoxelMachinePositionToClientMenusForServerActions() throws IOException {
        String menus = source("menu/ModMenus.java");
        String refineryMenu = source("menu/VoxelRefineryMenu.java");
        String printingMenu = source("menu/VoxelPrintingStationMenu.java");
        String refineryBlock = source("block/VoxelRefineryBlock.java");
        String printingBlock = source("block/VoxelPrintingStationBlock.java");
        String machineActions = source("network/VoxelMachineActions.java");

        assertTrue(menus.contains("VoxelRefineryMenu::new"));
        assertTrue(menus.contains("VoxelPrintingStationMenu::new"));
        assertTrue(refineryMenu.contains("RegistryFriendlyByteBuf data"));
        assertTrue(refineryMenu.contains("data.readBlockPos()"));
        assertTrue(printingMenu.contains("RegistryFriendlyByteBuf data"));
        assertTrue(printingMenu.contains("data.readBlockPos()"));
        assertTrue(refineryBlock.contains("openMenu(getMenuProvider(refinery, level, pos), pos)"));
        assertTrue(printingBlock.contains("Component.translatable(\"container.starboundmc.voxel_printing_station\")), pos"));
        assertTrue(machineActions.contains("menu.blockPos().equals(pos)"));
    }

    @Test
    void voxelMachineMenusProvideSmartTransferAndActionFeedback() throws IOException {
        String refineryMenu = source("menu/VoxelRefineryMenu.java");
        String printingMenu = source("menu/VoxelPrintingStationMenu.java");
        String refineryScreen = source("client/voxel/VoxelRefineryRoot.java");
        String printingScreen = source("client/voxel/VoxelPrintingStationRoot.java");
        String actions = source("network/VoxelMachineActions.java");

        assertTrue(refineryMenu.contains("matchingRecipe(stack, player.level()).isPresent()"));
        assertTrue(refineryMenu.contains("moveItemStackTo(stack, MACHINE_START, PLAYER_START, false)"));
        assertTrue(printingMenu.contains("isPrintingMaterial(stack, player.level())"));
        assertTrue(printingMenu.contains("VoxelPrintingStationBlockEntity.MATERIAL_SLOTS, false"));
        assertTrue(refineryScreen.contains("startButton.setActive(canStart)"));
        assertTrue(refineryScreen.contains("StopRefinementPacket"));
        assertTrue(refineryScreen.contains("startButton.style(style -> style.tooltips(startHint))"));
        assertTrue(printingScreen.contains("printButton.setActive(canPrint)"));
        assertTrue(printingScreen.contains("printButton.style(style -> style.tooltips(reason))"));
        assertTrue(actions.contains("message.starboundmc.voxel_refinery.unsupported"));
        assertTrue(actions.contains("message.starboundmc.voxel_printing.materials"));
    }

    @Test
    void refineryUsesPublicAtomicClaimInsteadOfOperatorOwnership() throws IOException {
        String refinery = source("block/entity/VoxelRefineryBlockEntity.java");
        String network = source("network/ModNetwork.java");
        String actions = source("network/VoxelMachineActions.java");

        assertFalse(refinery.contains("operatorId"));
        assertFalse(refinery.contains("putUUID"));
        assertTrue(refinery.contains("int claimPendingVoxels(ServerPlayer player)"));
        assertTrue(refinery.contains("int voxels = pendingVoxels;"));
        assertTrue(refinery.contains("pendingVoxels = 0;"));
        assertTrue(refinery.contains("VoxelWalletService.add(player, voxels);"));
        int claimMethod = refinery.indexOf("int claimPendingVoxels(ServerPlayer player)");
        int clearOutput = refinery.indexOf("pendingVoxels = 0;", claimMethod);
        int creditPlayer = refinery.indexOf("VoxelWalletService.add(player, voxels);", claimMethod);
        assertTrue(claimMethod >= 0 && clearOutput > claimMethod && creditPlayer > clearOutput);
        assertTrue(network.contains("ClaimRefinedVoxelsPacket.TYPE"));
        assertTrue(actions.contains("refinery.claimPendingVoxels(player)"));
    }

    @Test
    void voxelMachineM4UsesLdlibListDetailsAndProgressDrivenRenderer() throws IOException {
        String refineryScreen = source("client/VoxelRefineryScreen.java");
        String refineryRoot = source("client/voxel/VoxelRefineryRoot.java");
        String printingScreen = source("client/VoxelPrintingStationScreen.java");
        String printingRoot = source("client/voxel/VoxelPrintingStationRoot.java");
        String renderer = source("client/VoxelPrintingStationRenderer.java");
        String registrar = source("client/Stage2ClientRegistrar.java");

        assertTrue(refineryScreen.contains("extends StarboundModularScreen"));
        assertTrue(printingScreen.contains("extends StarboundModularScreen"));
        assertTrue(refineryRoot.contains("ClaimRefinedVoxelsPacket"));
        assertTrue(refineryRoot.contains("snapshotAt(menu.blockPos())"));
        assertTrue(printingRoot.contains("new ScrollerView()"));
        assertTrue(printingRoot.contains("voxel-recipe-unavailable"));
        assertTrue(printingRoot.contains("updateRequirementCounts"));
        assertTrue(printingRoot.contains("printButton.setActive(canPrint)"));
        assertTrue(renderer.contains("snapshot.resultItemId()"));
        assertTrue(renderer.contains("station.getLevel().getGameTime() + partialTick"));
        assertTrue(renderer.contains("renderOverheadProbes"));
        assertTrue(registrar.contains("registerBlockEntityRenderer"));
    }

    @Test
    void voxelMachineM6UsesAlwaysOnPublicBatchAndRequesterOwnedQueue() throws IOException {
        String refinery = source("block/entity/VoxelRefineryBlockEntity.java");
        String station = source("block/entity/VoxelPrintingStationBlockEntity.java");
        String network = source("network/ModNetwork.java");
        String printingRoot = source("client/voxel/VoxelPrintingStationRoot.java");
        String printingScreen = source("client/VoxelPrintingStationScreen.java");
        String printingMenu = source("menu/VoxelPrintingStationMenu.java");
        String refineryMenu = source("menu/VoxelRefineryMenu.java");

        assertTrue(refinery.contains("pendingVoxels + refinery.jobVoxels"));
        assertTrue(refinery.contains("refinery.startNextJob(serverLevel);"));
        assertTrue(refinery.contains("boolean stopRefinement(ServerLevel level)"));
        assertTrue(refinery.contains("stopAfterInputChange(wasRunning)"));
        assertTrue(refinery.contains("stopAfterManualInputTake()"));
        assertTrue(refineryMenu.contains("refinery.stopAfterManualInputTake()"));
        assertFalse(refinery.contains("continuousMode"));
        assertTrue(refinery.contains("drainPendingVoxelsForDrop"));
        assertFalse(refinery.contains("operatorId"));

        assertTrue(station.contains("MAX_OUTSTANDING_CRAFTS = 64"));
        assertTrue(station.contains("Deque<PrintQueueEntry> printQueue"));
        assertTrue(station.contains("VoxelWalletService.trySpend(operator, (int) totalCost)"));
        assertTrue(station.contains("entry.requesterId.equals(requester.getUUID())"));
        assertTrue(station.contains("QueueCancelResult.ACTIVE"));
        assertTrue(station.contains("dropReservedResources"));
        assertTrue(station.contains("tag.put(\"print_queue\", queueTag)"));
        int startNext = station.indexOf("private boolean startNextQueuedCraft()");
        int capacityCheck = station.indexOf("canAcceptResult(entry.result)", startNext);
        int dequeueCraft = station.indexOf("entry.crafts.removeFirst()", startNext);
        assertTrue(startNext >= 0 && capacityCheck > startNext && dequeueCraft > capacityCheck,
                "FIFO work must remain reserved until the output can accept its result");
        int cancel = station.indexOf("QueueCancelResult cancelQueuedPrint");
        int ownerCheck = station.indexOf("entry.requesterId.equals(requester.getUUID())", cancel);
        int removeQueueEntry = station.indexOf("iterator.remove()", cancel);
        int refundVoxels = station.indexOf("refundVoxels(requester, refund)", cancel);
        int refundMaterials = station.indexOf("returnMaterials(requester, craft.materials)", cancel);
        assertTrue(cancel >= 0 && ownerCheck > cancel && removeQueueEntry > ownerCheck
                && refundVoxels > removeQueueEntry && refundMaterials > refundVoxels,
                "only the requester may remove queued work before both resources are refunded");

        assertFalse(network.contains("SetRefineryContinuousPacket.TYPE"));
        assertTrue(network.contains("StopRefinementPacket.TYPE"));
        assertTrue(network.contains("CancelPrintQueuePacket.TYPE"));
        assertTrue(network.contains("SyncPrintQueuePacket.TYPE"));
        assertTrue(printingRoot.contains("quantityMinus"));
        assertTrue(printingRoot.contains("quantityMinusTen"));
        assertTrue(printingRoot.contains("quantityPlusTen"));
        assertTrue(printingRoot.contains("maxCraftsForMaterials"));
        assertTrue(printingRoot.contains("target = Math.min(target, selectedQuantityCeiling())"));
        assertTrue(printingRoot.contains("PANEL_H = 234"));
        assertTrue(printingRoot.contains("\"voxel-inventory-section\", 38, 147, 172, 84"));
        assertTrue(printingScreen.contains("PANEL_H = 234"));
        assertTrue(printingMenu.contains("addPlayerInventory(inventory, 42, 157)"));
        assertTrue(printingRoot.contains("syncQueueRows"));
        assertTrue(printingRoot.contains("queue.active_progress"));
        assertTrue(printingRoot.contains("CancelPrintQueuePacket"));
    }

    @Test
    void keepsPersonalStoryStateOutOfBroadcastAndAttachmentSync() throws IOException {
        String attachments = source("story/ModAttachments.java");
        String service = source("story/ShipStoryService.java");
        assertFalse(attachments.contains(".sync("));
        assertTrue(service.contains("ModNetwork.sendToPlayer"));
        assertFalse(service.contains("sendToPlayersInDimension"));
    }

    @Test
    void removesLegacyForgeChannelCallsFromAllSources() throws IOException {
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            String sources = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(Stage3NetworkWiringTest::readUnchecked)
                    .reduce("", String::concat);
            assertFalse(sources.contains("net.minecraftforge.network"));
            assertFalse(sources.contains("SimpleChannel"));
            assertFalse(sources.contains("ModNetwork.CHANNEL"));
        }
    }

    private static int occurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
