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
        // EPP equipment plus EVA input and authoritative state snapshots.
        assertTrue(network.contains("PROTOCOL_VERSION = \"14\""));
        assertEquals(17, occurrences(network, "playToServer("));
        // Existing twelve clientbound payloads are retained.
        assertEquals(17, occurrences(network, "playToClient("));
        assertTrue(network.contains("ShipEnvironmentSnapshotPacket.TYPE"));
        assertTrue(network.contains("NovaBroadcastPacket.TYPE"));
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
        // The body identity now arrives with the star-state packet, so the client
        // handler no longer translates a legacy planet name (migration §21/§22).
        assertFalse(client.contains("Planet.fromId"));
        assertFalse(client.contains("SyncPlanetPacket"));
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
        assertTrue(printingMenu.contains("HIDDEN_SLOT_POSITION"));
        assertTrue(printingMenu.contains("public boolean isActive()"));
        assertTrue(printingMenu.contains("station.returnLegacyMaterials(inventory.player)"));
        assertFalse(printingMenu.contains("isPrintingMaterial("));
        assertTrue(refineryScreen.contains("startButton.setActive(canStart)"));
        assertTrue(refineryScreen.contains("StopRefinementPacket"));
        assertTrue(refineryScreen.contains("startButton.style(style -> style.tooltips(startHint))"));
        // The craft action now lives inside the selected-item panel, so the page reaches it
        // through the composition root rather than owning the button itself.
        assertTrue(printingScreen.contains("craftButton().setActive(canPrint)"));
        assertTrue(printingScreen.contains("craftButton().style(style -> style.tooltips(reasonTooltip))"));
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
        String station = source("block/entity/VoxelPrintingStationBlockEntity.java");
        String printingRecipe = source("recipe/VoxelPrintingRecipe.java");

        assertTrue(refineryScreen.contains("extends StarboundModularScreen"));
        assertTrue(printingScreen.contains("extends StarboundModularScreen"));
        assertTrue(refineryRoot.contains("ClaimRefinedVoxelsPacket"));
        assertTrue(refineryRoot.contains("snapshotAt(menu.blockPos())"));
        String walletHud = source("client/VoxelWalletHud.java");
        assertTrue(walletHud.contains("ModItems.VOXEL.get()"));
        assertTrue(walletHud.contains("gui.starboundmc.voxel_wallet.label"));
        assertTrue(walletHud.contains("screen.getXSize() + SIDE_GAP"));
        assertTrue(walletHud.contains("screen.getGuiLeft() - SIDE_GAP"));
        assertTrue(walletHud.contains("screen.getYSize() + SIDE_GAP"));
        assertTrue(walletHud.contains("return null;"));
        assertTrue(printingRoot.contains("new ScrollerView()"));
        assertTrue(printingRoot.contains("gui.starboundmc.voxel_printing.device.printing"));
        assertTrue(printingRoot.contains("machine-status"));
        // The catalogue surface is a component: the browser builds the rows, the page supplies the
        // entries and applies its own relevance rule.
        assertTrue(printingRoot.contains("recipeBrowser.setEntries(entries)"));
        String recipeBrowser = source("client/ui/components/RecipeBrowser.java");
        assertTrue(recipeBrowser.contains("new RecipeListRow("));
        assertTrue(printingRoot.contains("entry.view.setCraftable(ready)"));
        assertTrue(printingRoot.contains("updateRequirements(recipe)"));
        // Material lines are a component concern: the view takes the data, and the requirement
        // list inside it is what builds and pools RequirementRow instances.
        assertTrue(printingRoot.contains("selectedItemView.setRequirements(lines)"));
        String materialView = source("client/ui/components/MaterialRequirementView.java");
        assertTrue(materialView.contains("new RequirementRow("));
        assertTrue(printingRoot.contains("craftButton().setActive(canPrint)"));
        assertTrue(printingRoot.contains("itemDescription(result)"));
        assertTrue(printingRoot.contains("getTooltipLines"));
        // The output frame is the one place the item appears: it previews the selection, reveals
        // colour as progress advances, and steps aside for the real item. It sits on the vanilla
        // output slot's coordinate, which is where the menu draws that item.
        assertTrue(printingRoot.contains("selectedItemView.setOutput("));
        String outputSlot = source("client/ui/components/OutputPreviewSlot.java");
        assertTrue(outputSlot.contains("voxel-printing-output-socket"));
        assertTrue(outputSlot.contains("SOCKET_SIZE") || outputSlot.contains("SIZE = 18"));
        assertTrue(printingMenuSource().contains("OUTPUT_SLOT, 151, 31"));
        assertTrue(outputSlot.contains("setDisplay(true)"));
        assertFalse(printingRoot.contains("resultIcon"));
        assertFalse(printingRoot.contains("voxel-printing-material-socket"));
        assertTrue(renderer.contains("snapshot.resultItemId()"));
        assertTrue(renderer.contains("station.getLevel().getGameTime() + partialTick"));
        assertTrue(renderer.contains("renderOverheadProbes"));
        assertTrue(printingRoot.contains("player.getInventory().items"));
        assertTrue(station.contains("operator.getInventory().items"));
        assertTrue(printingRecipe.contains("reserveMaterials"));
        int simulateBackpack = station.indexOf("simulated.add(stack.copy())");
        int spendVoxels = station.indexOf("VoxelWalletService.trySpend");
        int applyBackpack = station.indexOf("operator.getInventory().items.set");
        assertTrue(simulateBackpack >= 0 && spendVoxels > simulateBackpack
                && applyBackpack > spendVoxels,
                "Backpack materials must remain simulated until every resource check succeeds");
        assertTrue(registrar.contains("registerBlockEntityRenderer"));
    }

    private static String printingMenuSource() throws IOException {
        return source("menu/VoxelPrintingStationMenu.java");
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
        assertTrue(printingRoot.contains("selectedItemView.quantityStepper()"));
        assertTrue(printingRoot.contains(
                "selectedItemView.quantityStepper().setValueAndMaximum(quantity, selectionLimit)"));
        assertTrue(printingRoot.contains("maxCraftsForRequirements"));
        assertTrue(printingRoot.contains("PANEL_H = 240"));
        assertTrue(printingRoot.contains("\"voxel-printing-recipe-pane\", 6, 28, 136, 207"));
        assertTrue(printingRoot.contains("WORKSPACE_H = 120"));
        // The region is still anchored where the menu's slot layout requires; only its interior
        // is delegated to the panel.
        assertTrue(printingRoot.contains("\"voxel-printing-detail-pane\""));
        assertTrue(printingRoot.contains("left(146).top(28)"));
        assertTrue(printingRoot.contains("\"voxel-printing-queue-pane\", 6, 28"));
        assertTrue(printingRoot.contains("queuePane.setDisplay(false)"),
                "Queue must start hidden: it is a secondary mode, not a dominant third column");
        assertTrue(printingRoot.contains("recipesPane.setDisplay(!showingQueue)"));
        assertTrue(printingRoot.contains("workspacePane.setDisplay(!showingQueue)"));
        assertTrue(printingRoot.contains("queuePane.setDisplay(showingQueue)"));
        assertTrue(printingRoot.contains("\"voxel-inventory-section\", 144, 151, 172, 84"));
        assertTrue(printingScreen.contains("PANEL_H = 240"));
        assertTrue(printingMenu.contains("addPlayerInventory(inventory, 148, 161)"));
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
