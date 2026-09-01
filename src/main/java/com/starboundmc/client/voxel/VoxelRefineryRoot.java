package com.starboundmc.client.voxel;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.starboundmc.client.ClientVoxelMachineState;
import com.starboundmc.client.ClientVoxelWalletState;
import com.starboundmc.item.ModItems;
import com.starboundmc.menu.VoxelRefineryMenu;
import com.starboundmc.network.ClaimRefinedVoxelsPacket;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.StartRefinementPacket;
import com.starboundmc.network.StopRefinementPacket;
import dev.vfyjxf.taffy.style.TaffyPosition;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Stable LDLib2 tree for the public-output voxel refinery. */
public final class VoxelRefineryRoot extends UIElement {
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;

    private final VoxelRefineryMenu menu;
    private final UIElement arrowFill = new UIElement();
    private final Label status = new Label();
    private final Label wallet = new Label();
    private final Label outputAmount = new Label();
    private final Button startButton = new Button();
    private final Button claimButton = new Button();
    private ViewState lastState;

    public VoxelRefineryRoot(VoxelRefineryMenu menu, int left, int top,
                             Component title, Component inventoryTitle) {
        this.menu = menu;
        addClass("machine-inventory-screen");
        setAllowHitTest(false);
        layout(layout -> layout.widthPercent(100).heightPercent(100));

        var shell = new UIElement().addClasses("inventory-machine-shell", "voxel-refinery-shell");
        shell.setAllowHitTest(false);
        shell.setOverflowVisible(false);
        shell.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(left)
                .top(top)
                .width(PANEL_W)
                .height(PANEL_H));

        shell.addChildren(buildHeader(title), buildProcess(), buildInventory(inventoryTitle));
        addChild(shell);
        refresh();
    }

    private UIElement buildHeader(Component title) {
        var header = VoxelUiSupport.positioned("machine-inventory-header", 3, 3, 170, 21);
        header.addClass("voxel-machine-header");
        header.addChildren(
                VoxelUiSupport.positioned("voxel-refinery-rail", 0, 0, 3, 21),
                VoxelUiSupport.label(title, "machine-inventory-title", 8, 5, 120, 10));

        var publicOutput = VoxelUiSupport.label(
                Component.translatable("gui.starboundmc.voxel_refinery.public_output"),
                "voxel-machine-header-status", 128, 6, 37, 8);
        publicOutput.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        header.addChild(publicOutput);
        return header;
    }

    private UIElement buildProcess() {
        var process = VoxelUiSupport.positioned("voxel-refinery-process", 4, 26, 168, 46);

        process.addChild(VoxelUiSupport.slotSocket("voxel-refinery-input-socket", 39, 9));
        process.addChild(VoxelUiSupport.label(
                Component.translatable("gui.starboundmc.voxel_refinery.input"),
                "voxel-machine-caption", 29, 0, 38, 8));

        var arrow = VoxelUiSupport.positioned("voxel-refinery-arrow", 67, 13, 42, 10);
        arrowFill.addClass("voxel-refinery-arrow-fill");
        arrowFill.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(1)
                .top(1)
                .widthPercent(0)
                .height(8));
        arrow.addChild(arrowFill);
        var glyph = VoxelUiSupport.label(Component.literal("➜"),
                "voxel-refinery-arrow-glyph", 67, 3, 42, 10);
        VoxelUiSupport.center(glyph);

        claimButton.noText();
        claimButton.addClass("voxel-refinery-claim");
        claimButton.setOverflowVisible(false);
        claimButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(113)
                .top(5)
                .width(47)
                .height(27));
        claimButton.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && claimButton.isActive()) {
                ModNetwork.sendToServer(new ClaimRefinedVoxelsPacket(menu.blockPos()));
                event.stopPropagation();
            }
        });

        var voxelIcon = new UIElement().addClass("voxel-refinery-output-icon");
        voxelIcon.setAllowHitTest(false);
        voxelIcon.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(3)
                .top(5)
                .width(16)
                .height(16));
        voxelIcon.style(style -> style.backgroundTexture(new ItemStackTexture(ModItems.VOXEL.get())));

        outputAmount.addClass("voxel-refinery-output-amount");
        outputAmount.setAllowHitTest(false);
        outputAmount.setOverflowVisible(false);
        outputAmount.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(20)
                .top(3)
                .width(24)
                .height(20));
        outputAmount.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        claimButton.addChildren(voxelIcon, outputAmount);

        startButton.setText(Component.translatable("gui.starboundmc.voxel_refinery.start"));
        startButton.addClass("voxel-machine-action");
        startButton.text.setAllowHitTest(false);
        startButton.text.setOverflowVisible(false);
        startButton.text.layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .marginHorizontal(0));
        startButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(67)
                .top(28)
                .width(93)
                .height(16)
                .paddingAll(1));
        startButton.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        startButton.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && startButton.isActive()) {
                boolean running = lastState != null && lastState.running();
                ModNetwork.sendToServer(running
                        ? new StopRefinementPacket(menu.blockPos())
                        : new StartRefinementPacket(menu.blockPos()));
                event.stopPropagation();
            }
        });

        wallet.addClass("voxel-refinery-wallet");
        wallet.setAllowHitTest(false);
        wallet.setOverflowVisible(false);
        wallet.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(4)
                .top(31)
                .width(59)
                .height(11));
        wallet.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        status.addClass("voxel-refinery-status");
        status.setAllowHitTest(false);
        status.setOverflowVisible(false);
        status.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(67)
                .top(0)
                .width(93)
                .height(9));
        status.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        process.addChildren(glyph, arrow, claimButton, startButton, wallet, status);
        return process;
    }

    private UIElement buildInventory(Component inventoryTitle) {
        var section = VoxelUiSupport.positioned("voxel-inventory-section", 4, 74, 168, 88);
        section.addChild(VoxelUiSupport.label(
                inventoryTitle, "machine-inventory-caption", 4, 0, 156, 8));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                section.addChild(VoxelUiSupport.slotSocket(
                        "player-slot-socket", 3 + col * 18, 9 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            section.addChild(VoxelUiSupport.slotSocket(
                    "hotbar-slot-socket", 3 + col * 18, 67));
        }
        return section;
    }

    public void refresh() {
        var minecraft = Minecraft.getInstance();
        var snapshot = ClientVoxelMachineState.snapshotAt(menu.blockPos());
        boolean running = snapshot != null && snapshot.progress() > 0;
        int pending = snapshot == null ? 0 : snapshot.voxels();
        int expected = 0;
        boolean empty = menu.getSlot(0).getItem().isEmpty();
        boolean supported = false;
        if (!empty && minecraft.level != null) {
            var recipe = menu.matchingRecipe(minecraft.level);
            supported = recipe.isPresent();
            expected = recipe.map(holder -> holder.value().voxels()).orElse(0);
        }
        int balance = ClientVoxelWalletState.balance();
        int progress = running
                ? (int) ((snapshot.totalTicks() - snapshot.progress()) * 100L
                / Math.max(1, snapshot.totalTicks())) : 0;

        ViewState next = new ViewState(running, pending, expected,
                empty, supported, balance, progress);
        if (next.equals(lastState)) {
            return;
        }
        lastState = next;

        arrowFill.layout(layout -> layout.widthPercent(progress));
        wallet.setText(Component.translatable("gui.starboundmc.voxel_wallet",
                String.format(Locale.ROOT, "%,d", balance)));
        Component startHint;
        if (running) {
            startHint = Component.translatable("gui.starboundmc.voxel_refinery.hint.stop");
        } else if (empty) {
            startHint = Component.translatable("gui.starboundmc.voxel_refinery.hint.empty");
        } else if (!supported) {
            startHint = Component.translatable("gui.starboundmc.voxel_refinery.hint.unsupported");
        } else {
            startHint = Component.translatable("gui.starboundmc.voxel_refinery.hint.ready", expected);
        }
        startButton.setText(Component.translatable(running
                ? "gui.starboundmc.voxel_refinery.stop"
                : "gui.starboundmc.voxel_refinery.start"));
        startButton.removeClass("voxel-refinery-stop");
        if (running) {
            startButton.addClass("voxel-refinery-stop");
        }
        boolean canStart = running || supported;
        startButton.setActive(canStart);
        startButton.style(style -> style.tooltips(startHint));

        Component claimHint;
        if (running) {
            claimHint = Component.translatable("gui.starboundmc.voxel_refinery.hint.claim_running");
        } else if (pending <= 0) {
            claimHint = Component.translatable("gui.starboundmc.voxel_refinery.hint.nothing_to_claim");
        } else {
            claimHint = Component.translatable("gui.starboundmc.voxel_refinery.hint.claim", pending);
        }
        claimButton.setActive(!running && pending > 0);
        claimButton.style(style -> style.tooltips(claimHint));
        claimButton.removeClasses("voxel-output-ready", "voxel-output-running");
        if (running) {
            claimButton.addClass("voxel-output-running");
        } else if (pending > 0) {
            claimButton.addClass("voxel-output-ready");
        }

        if (running) {
            status.setText(Component.translatable(
                    "gui.starboundmc.voxel_refinery.progress", progress + "%"));
            outputAmount.setText(Component.literal(pending > 0 ? pending + "+" : "…"));
        } else if (pending > 0) {
            status.setText(Component.translatable("gui.starboundmc.voxel_refinery.status.ready"));
            outputAmount.setText(Component.literal(String.format(Locale.ROOT, "%,d", pending)));
        } else if (supported) {
            status.setText(Component.translatable("gui.starboundmc.voxel_refinery.status.preview"));
            outputAmount.setText(Component.literal("≈" + expected));
        } else {
            status.setText(startHint);
            outputAmount.setText(Component.literal("—"));
        }
        status.style(style -> style.tooltips(startHint));
    }

    private record ViewState(boolean running, int pending, int expected, boolean empty,
                             boolean supported, int balance, int progress) {
    }
}
