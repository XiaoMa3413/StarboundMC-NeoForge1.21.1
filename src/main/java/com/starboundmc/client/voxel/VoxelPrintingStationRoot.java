package com.starboundmc.client.voxel;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.starboundmc.block.entity.VoxelPrintingStationBlockEntity;
import com.starboundmc.client.ClientPrintQueueState;
import com.starboundmc.client.ClientVoxelMachineState;
import com.starboundmc.client.ClientVoxelWalletState;
import com.starboundmc.menu.VoxelPrintingStationMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.StartPrintPacket;
import com.starboundmc.network.CancelPrintQueuePacket;
import com.starboundmc.network.SyncPrintQueuePacket;
import com.starboundmc.recipe.MachineRecipeInput;
import com.starboundmc.recipe.VoxelPrintingRecipe;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

/** Stable LDLib2 tree for list-driven voxel printing. */
public final class VoxelPrintingStationRoot extends UIElement {
    private static final int PANEL_W = 364;
    private static final int PANEL_H = 234;

    private final VoxelPrintingStationMenu menu;
    private final List<RecipeHolder<VoxelPrintingRecipe>> recipes;
    private final List<RecipeRow> rows = new ArrayList<>();
    private final ScrollerView recipeList = new ScrollerView();
    private final ScrollerView queueList = new ScrollerView();
    private final Map<UUID, QueueRow> queueRows = new LinkedHashMap<>();
    private final Label wallet = new Label();
    private final Label queueTitle = new Label();
    private final Label queueEmpty = new Label();
    private final Label detailName = new Label();
    private final Label detailOutput = new Label();
    private final Label detailMeta = new Label();
    private final Label detailStatus = new Label();
    private final Label emptyState = new Label();
    private final Label[] requirementCounts = {new Label(), new Label(), new Label()};
    private final UIElement[] requirementIcons = {new UIElement(), new UIElement(), new UIElement()};
    private final ItemStackTexture[] requirementTextures = {
            new ItemStackTexture(), new ItemStackTexture(), new ItemStackTexture()
    };
    private final ItemStackTexture resultTexture = new ItemStackTexture();
    private final UIElement resultIcon = new UIElement();
    private final Button printButton = new Button();
    private final Button quantityMinusTen = new Button();
    private final Button quantityMinus = new Button();
    private final Button quantityPlus = new Button();
    private final Button quantityPlusTen = new Button();
    private final Label quantityLabel = new Label();
    private int selected = -1;
    private int quantity = 1;
    private int lastDetailedSelection = Integer.MIN_VALUE;
    private int lastDetailedQuantity = Integer.MIN_VALUE;
    private DetailState lastDetailState;

    public VoxelPrintingStationRoot(VoxelPrintingStationMenu menu, int left, int top,
                                    Component title, Component inventoryTitle) {
        this.menu = menu;
        var level = Minecraft.getInstance().level;
        recipes = level == null ? List.of()
                : List.copyOf(level.getRecipeManager().getAllRecipesFor(VoxelPrintingRecipe.TYPE));
        selected = recipes.isEmpty() ? -1 : 0;

        addClass("machine-inventory-screen");
        setAllowHitTest(false);
        layout(layout -> layout.widthPercent(100).heightPercent(100));

        var shell = new UIElement().addClasses("inventory-machine-shell", "voxel-printing-shell");
        shell.setAllowHitTest(false);
        shell.setOverflowVisible(false);
        shell.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(left)
                .top(top)
                .width(PANEL_W)
                .height(PANEL_H));
        shell.addChildren(buildHeader(title), buildRecipePane(), buildDetailPane(), buildQueuePane(),
                buildInventory(inventoryTitle));
        addChild(shell);
        updateSelectedVisual();
        refresh();
    }

    private UIElement buildHeader(Component title) {
        var header = VoxelUiSupport.positioned("machine-inventory-header", 3, 3, 358, 21);
        header.addClass("voxel-machine-header");
        header.addChildren(
                VoxelUiSupport.positioned("voxel-printing-rail", 0, 0, 3, 21),
                VoxelUiSupport.label(title, "machine-inventory-title", 8, 5, 230, 10));

        wallet.addClass("voxel-printing-wallet");
        wallet.setAllowHitTest(false);
        wallet.setOverflowVisible(false);
        wallet.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(250)
                .top(5)
                .width(102)
                .height(10));
        wallet.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        header.addChild(wallet);
        return header;
    }

    private UIElement buildRecipePane() {
        var pane = VoxelUiSupport.positioned("voxel-printing-recipe-pane", 4, 26, 122, 119);
        pane.addChild(VoxelUiSupport.label(
                Component.translatable("gui.starboundmc.voxel_printing.recipes"),
                "voxel-pane-title", 5, 3, 111, 8));

        recipeList.addClass("voxel-recipe-list");
        recipeList.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(4)
                .top(13)
                .width(114)
                .height(102));
        recipeList.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .minScrollPixel(8)
                .maxScrollPixel(20));
        recipeList.viewPort(view -> view
                .layout(layout -> layout.paddingAll(0))
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY)));
        recipeList.viewContainer(view -> view.layout(layout -> layout
                .widthPercent(100)
                .gapAll(2)
                .flexDirection(FlexDirection.COLUMN)));

        if (recipes.isEmpty()) {
            emptyState.setText(Component.translatable("gui.starboundmc.voxel_printing.hint.no_recipe"));
            emptyState.addClass("voxel-recipe-empty");
            emptyState.setAllowHitTest(false);
            emptyState.layout(layout -> layout.widthPercent(100).height(32));
            emptyState.textStyle(style -> style
                    .adaptiveWidth(false)
                    .textAlignHorizontal(Horizontal.CENTER)
                    .textAlignVertical(Vertical.CENTER)
                    .textWrap(TextWrap.WRAP));
            recipeList.addScrollViewChild(emptyState);
        } else {
            for (int index = 0; index < recipes.size(); index++) {
                addRecipeRow(index, recipes.get(index));
            }
        }
        pane.addChild(recipeList);
        return pane;
    }

    private void addRecipeRow(int index, RecipeHolder<VoxelPrintingRecipe> holder) {
        VoxelPrintingRecipe recipe = holder.value();
        ItemStack result = resultStack(holder);
        var button = new Button();
        button.noText();
        button.addClass("voxel-recipe-row");
        button.setOverflowVisible(false);
        button.layout(layout -> layout.widthPercent(100).height(24));
        button.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                selected = index;
                updateSelectedVisual();
                lastDetailState = null;
                refresh();
                event.stopPropagation();
            }
        });

        var icon = new UIElement().addClass("voxel-recipe-icon");
        icon.setAllowHitTest(false);
        icon.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(3)
                .top(4)
                .width(16)
                .height(16));
        icon.style(style -> style.backgroundTexture(new ItemStackTexture(result)));

        var name = VoxelUiSupport.label(result.getHoverName(),
                "voxel-recipe-name", 22, 3, 62, 18);
        var amount = VoxelUiSupport.label(Component.literal("×" + result.getCount()),
                "voxel-recipe-amount", 86, 2, 22, 9);
        VoxelUiSupport.center(amount);
        var cost = VoxelUiSupport.label(Component.translatable(
                        "gui.starboundmc.voxel_printing.cost_short", recipe.voxelCost()),
                "voxel-recipe-cost", 86, 12, 22, 8);
        VoxelUiSupport.center(cost);
        button.addChildren(icon, name, amount, cost);
        button.style(style -> style.tooltips(result.getHoverName()));
        recipeList.addScrollViewChild(button);
        rows.add(new RecipeRow(holder, button));
    }

    private UIElement buildQueuePane() {
        var pane = VoxelUiSupport.positioned("voxel-printing-queue-pane", 246, 26, 114, 205);

        queueTitle.addClass("voxel-pane-title");
        queueTitle.setAllowHitTest(false);
        queueTitle.setOverflowVisible(false);
        queueTitle.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(5)
                .top(3)
                .width(104)
                .height(8));
        queueTitle.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        queueList.addClass("voxel-print-queue-list");
        queueList.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(4)
                .top(14)
                .width(106)
                .height(187));
        queueList.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .minScrollPixel(8)
                .maxScrollPixel(20));
        queueList.viewPort(view -> view
                .layout(layout -> layout.paddingAll(0))
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY)));
        queueList.viewContainer(view -> view.layout(layout -> layout
                .widthPercent(100)
                .gapAll(2)
                .flexDirection(FlexDirection.COLUMN)));

        queueEmpty.setText(Component.translatable("gui.starboundmc.voxel_printing.queue.empty"));
        queueEmpty.addClass("voxel-queue-empty");
        queueEmpty.setAllowHitTest(false);
        queueEmpty.layout(layout -> layout.widthPercent(100).height(28));
        queueEmpty.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.WRAP));
        queueList.addScrollViewChild(queueEmpty);

        pane.addChildren(queueTitle, queueList);
        return pane;
    }

    private void syncQueueRows(SyncPrintQueuePacket snapshot, int activeProgress) {
        List<SyncPrintQueuePacket.Entry> entries = snapshot == null ? List.of() : snapshot.entries();
        int outstanding = snapshot == null ? 0 : snapshot.outstandingCrafts();
        queueTitle.setText(Component.translatable(
                "gui.starboundmc.voxel_printing.queue.title", outstanding,
                VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS));

        Map<UUID, SyncPrintQueuePacket.Entry> incoming = new LinkedHashMap<>();
        for (SyncPrintQueuePacket.Entry entry : entries) {
            incoming.put(entry.id(), entry);
        }
        var stale = queueRows.entrySet().iterator();
        while (stale.hasNext()) {
            var row = stale.next();
            if (!incoming.containsKey(row.getKey())) {
                queueList.removeScrollViewChild(row.getValue().root);
                stale.remove();
            }
        }

        if (incoming.isEmpty()) {
            if (!queueList.hasScrollViewChild(queueEmpty)) {
                queueList.addScrollViewChild(queueEmpty);
            }
            queueRows.clear();
            return;
        }
        queueList.removeScrollViewChild(queueEmpty);
        Map<UUID, QueueRow> ordered = new LinkedHashMap<>();
        int index = 0;
        for (SyncPrintQueuePacket.Entry entry : incoming.values()) {
            QueueRow row = queueRows.get(entry.id());
            if (row == null || !row.entry.equals(entry)) {
                if (row != null) {
                    queueList.removeScrollViewChild(row.root);
                }
                row = createQueueRow(entry);
            }
            updateQueueRowState(row, activeProgress);
            List<UIElement> children = queueList.viewContainer.getChildren();
            if (!queueList.hasScrollViewChild(row.root)) {
                queueList.addScrollViewChildAt(row.root, index);
            } else if (index >= children.size() || children.get(index) != row.root) {
                queueList.removeScrollViewChild(row.root);
                queueList.addScrollViewChildAt(row.root, index);
            }
            ordered.put(entry.id(), row);
            index++;
        }
        queueRows.clear();
        queueRows.putAll(ordered);
    }

    private QueueRow createQueueRow(SyncPrintQueuePacket.Entry entry) {
        var row = new UIElement().addClass("voxel-queue-row");
        if (entry.active()) {
            row.addClass("voxel-queue-row-active");
        }
        row.setOverflowVisible(false);
        row.layout(layout -> layout.widthPercent(100).height(29));

        ItemStack result = new ItemStack(BuiltInRegistries.ITEM.get(entry.resultItemId()),
                entry.resultCount());
        var icon = new UIElement().addClass("voxel-queue-icon");
        icon.setAllowHitTest(false);
        icon.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(3).top(6).width(16).height(16));
        icon.style(style -> style.backgroundTexture(new ItemStackTexture(result)));

        var state = VoxelUiSupport.label(Component.empty(),
                "voxel-queue-state", 22, 3, 56, 10);
        var requester = VoxelUiSupport.label(Component.literal(entry.requesterName()),
                "voxel-queue-requester", 22, 15, 56, 9);

        var cancel = new Button();
        cancel.setText(Component.literal("×"));
        cancel.addClass("voxel-queue-cancel");
        cancel.text.setAllowHitTest(false);
        cancel.text.setOverflowVisible(false);
        cancel.text.layout(layout -> layout
                .widthPercent(100).heightPercent(100).marginHorizontal(0));
        cancel.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(81).top(7).width(17).height(15).paddingAll(1));
        cancel.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        var player = Minecraft.getInstance().player;
        boolean canCancel = !entry.active() && player != null
                && player.getUUID().equals(entry.requesterId());
        cancel.setActive(canCancel);
        cancel.style(style -> style.tooltips(Component.translatable(entry.active()
                ? "gui.starboundmc.voxel_printing.queue.active_no_cancel"
                : canCancel
                ? "gui.starboundmc.voxel_printing.queue.cancel"
                : "gui.starboundmc.voxel_printing.queue.not_owner")));
        cancel.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && cancel.isActive()) {
                ModNetwork.sendToServer(new CancelPrintQueuePacket(menu.blockPos(), entry.id()));
                event.stopPropagation();
            }
        });
        row.style(style -> style.tooltips(Component.translatable(
                "gui.starboundmc.voxel_printing.queue.tooltip",
                result.getHoverName(), entry.requesterName(), entry.crafts())));
        row.addChildren(icon, state, requester, cancel);
        return new QueueRow(entry, row, state);
    }

    private static void updateQueueRowState(QueueRow row, int activeProgress) {
        row.state.setText(row.entry.active()
                ? Component.translatable("gui.starboundmc.voxel_printing.queue.active_progress",
                activeProgress + "%")
                : Component.translatable("gui.starboundmc.voxel_printing.queue.crafts",
                row.entry.crafts()));
    }

    private UIElement buildDetailPane() {
        var pane = VoxelUiSupport.positioned("voxel-printing-detail-pane", 128, 26, 116, 119);
        pane.addChild(VoxelUiSupport.label(
                Component.translatable("gui.starboundmc.voxel_printing.details"),
                "voxel-pane-title", 5, 3, 106, 8));

        resultIcon.addClass("voxel-printing-result-icon");
        resultIcon.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(5)
                .top(12)
                .width(18)
                .height(18));
        resultIcon.style(style -> style.backgroundTexture(resultTexture));

        detailName.addClass("voxel-printing-detail-name");
        detailName.setAllowHitTest(false);
        detailName.setOverflowVisible(false);
        detailName.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(27)
                .top(10)
                .width(83)
                .height(12));
        detailName.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        detailOutput.addClass("voxel-printing-detail-output");
        detailOutput.setAllowHitTest(false);
        detailOutput.setOverflowVisible(false);
        detailOutput.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(27)
                .top(22)
                .width(83)
                .height(8));
        detailOutput.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        for (int i = 0; i < requirementIcons.length; i++) {
            int cardLeft = 5 + i * 32;
            var card = VoxelUiSupport.positioned("voxel-requirement-card", cardLeft, 31, 30, 14);
            var icon = requirementIcons[i];
            var requirementTexture = requirementTextures[i];
            icon.addClass("voxel-requirement-icon");
            icon.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(1)
                    .top(1)
                    .width(12)
                    .height(12));
            icon.style(style -> style.backgroundTexture(requirementTexture));

            var count = requirementCounts[i];
            count.addClass("voxel-requirement-count");
            count.setAllowHitTest(false);
            count.setOverflowVisible(false);
            count.layout(layout -> layout
                    .positionType(TaffyPosition.ABSOLUTE)
                    .left(13)
                    .top(1)
                    .width(16)
                    .height(12));
            count.textStyle(style -> style
                    .adaptiveWidth(false)
                    .textAlignHorizontal(Horizontal.CENTER)
                    .textAlignVertical(Vertical.CENTER)
                    .textWrap(TextWrap.HIDE));
            card.addChildren(icon, count);
            pane.addChild(card);
        }

        for (int i = 0; i < VoxelPrintingStationBlockEntity.MATERIAL_SLOTS; i++) {
            pane.addChild(VoxelUiSupport.slotSocket(
                    "voxel-printing-material-socket", 5 + i * 18, 46));
        }
        pane.addChild(VoxelUiSupport.label(Component.literal("→"),
                "voxel-printing-output-arrow", 61, 49, 24, 12));
        pane.addChild(VoxelUiSupport.slotSocket("voxel-printing-output-socket", 88, 46));

        detailMeta.addClass("voxel-printing-detail-meta");
        detailMeta.setAllowHitTest(false);
        detailMeta.setOverflowVisible(false);
        detailMeta.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(5)
                .top(66)
                .width(106)
                .height(8));
        detailMeta.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        detailStatus.addClass("voxel-printing-detail-status");
        detailStatus.setAllowHitTest(false);
        detailStatus.setOverflowVisible(false);
        detailStatus.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(5)
                .top(74)
                .width(106)
                .height(8));
        detailStatus.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        configureQuantityButton(quantityMinusTen, "−10", 5, 22, -10);
        configureQuantityButton(quantityMinus, "−", 28, 16, -1);
        configureQuantityButton(quantityPlus, "+", 67, 16, 1);
        configureQuantityButton(quantityPlusTen, "+10", 84, 27, 10);
        quantityLabel.addClass("voxel-printing-quantity");
        quantityLabel.setAllowHitTest(false);
        quantityLabel.setOverflowVisible(false);
        quantityLabel.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(45).top(83).width(21).height(14));
        quantityLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        printButton.setText(Component.translatable("gui.starboundmc.voxel_printing.enqueue"));
        printButton.addClass("voxel-machine-action");
        printButton.text.setAllowHitTest(false);
        printButton.text.setOverflowVisible(false);
        printButton.text.layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .marginHorizontal(0));
        printButton.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(5)
                .top(100)
                .width(106)
                .height(16)
                .paddingAll(1));
        printButton.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        printButton.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && printButton.isActive()
                    && selected >= 0 && selected < recipes.size()) {
                ModNetwork.sendToServer(new StartPrintPacket(
                        menu.blockPos(), recipes.get(selected).id(), quantity));
                event.stopPropagation();
            }
        });

        pane.addChildren(resultIcon, detailName, detailOutput, detailMeta, detailStatus,
                quantityMinusTen, quantityMinus, quantityLabel, quantityPlus, quantityPlusTen,
                printButton);
        return pane;
    }

    private void configureQuantityButton(Button button, String text, int left, int width, int delta) {
        button.setText(Component.literal(text));
        button.addClass("voxel-quantity-button");
        button.text.setAllowHitTest(false);
        button.text.setOverflowVisible(false);
        button.text.layout(layout -> layout
                .widthPercent(100).heightPercent(100).marginHorizontal(0));
        button.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(left).top(83).width(width).height(14).paddingAll(1));
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        button.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && button.isActive()) {
                int target = quantity + delta;
                if (delta > 0) {
                    target = Math.min(target, selectedQuantityCeiling());
                }
                quantity = Math.max(1, Math.min(64, target));
                lastDetailState = null;
                refresh();
                event.stopPropagation();
            }
        });
    }

    private UIElement buildInventory(Component inventoryTitle) {
        var section = VoxelUiSupport.positioned("voxel-inventory-section", 38, 147, 172, 84);
        section.addChild(VoxelUiSupport.label(
                inventoryTitle, "machine-inventory-caption", 4, 0, 160, 8));
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
        if (minecraft.level == null) {
            return;
        }
        var machineSnapshot = ClientVoxelMachineState.snapshotAt(menu.blockPos());
        int progress = machineSnapshot != null && machineSnapshot.totalTicks() > 0
                ? Math.max(0, Math.min(100,
                (int) ((machineSnapshot.totalTicks() - machineSnapshot.progress()) * 100L
                / machineSnapshot.totalTicks()))) : 0;
        var queueSnapshot = ClientPrintQueueState.snapshotAt(menu.blockPos());
        syncQueueRows(queueSnapshot, progress);
        int outstanding = queueSnapshot == null ? 0 : queueSnapshot.outstandingCrafts();
        int balance = ClientVoxelWalletState.balance();
        wallet.setText(Component.translatable("gui.starboundmc.voxel_wallet",
                String.format(Locale.ROOT, "%,d", balance)));
        if (selected < 0 || selected >= recipes.size()) {
            showNoSelection();
            return;
        }

        RecipeHolder<VoxelPrintingRecipe> holder = recipes.get(selected);
        VoxelPrintingRecipe recipe = holder.value();
        int materialLimit = maxCraftsForMaterials(recipe, minecraft.level);
        int queueLimit = Math.max(0,
                VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS - outstanding);
        int selectionLimit = Math.min(64, Math.min(materialLimit, queueLimit));
        int quantityCeiling = Math.max(1, selectionLimit);
        if (quantity > quantityCeiling) {
            quantity = quantityCeiling;
            lastDetailState = null;
        }

        for (RecipeRow row : rows) {
            boolean ready = maxCraftsForMaterials(row.holder.value(), minecraft.level) >= quantity
                    && (long) balance >= (long) row.holder.value().voxelCost() * quantity
                    && outstanding + quantity <= VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS;
            row.setReady(ready);
        }

        ItemStack result = resultStack(holder);
        long totalCost = (long) recipe.voxelCost() * quantity;
        boolean materials = materialLimit >= quantity;
        boolean voxels = totalCost <= balance;
        boolean capacity = outstanding + quantity
                <= VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS;
        boolean outputBlocked = outputBlocked(result);
        boolean running = machineSnapshot != null && machineSnapshot.progress() > 0;

        DetailState state = new DetailState(selected, quantity, balance, materials, voxels,
                capacity, outputBlocked, running, progress, outstanding,
                queueSnapshot == null ? 0 : queueSnapshot.hashCode(), slotFingerprint());
        if (state.equals(lastDetailState)) {
            return;
        }
        lastDetailState = state;
        updateStaticDetail(holder);
        updateRequirementCounts(recipe);

        Component reason;
        if (!materials) {
            reason = Component.translatable("gui.starboundmc.voxel_printing.hint.materials");
        } else if (!voxels) {
            reason = Component.translatable("gui.starboundmc.voxel_printing.hint.voxels",
                    totalCost - balance);
        } else if (!capacity) {
            reason = Component.translatable("gui.starboundmc.voxel_printing.hint.queue_full");
        } else if (outputBlocked) {
            reason = Component.translatable("gui.starboundmc.voxel_printing.hint.output_wait");
        } else if (running) {
            reason = Component.translatable(
                    "gui.starboundmc.voxel_printing.hint.enqueue_while_printing", progress + "%");
        } else {
            reason = Component.translatable("gui.starboundmc.voxel_printing.hint.enqueue_ready");
        }
        detailStatus.setText(reason);
        detailStatus.style(style -> style.tooltips(reason));
        boolean canPrint = materials && voxels && capacity;
        printButton.setActive(canPrint);
        printButton.style(style -> style.tooltips(reason));
        quantityLabel.setText(Component.literal("×" + quantity));
        quantityMinus.setActive(quantity > 1);
        quantityMinusTen.setActive(quantity > 1);
        boolean canIncrease = quantity < selectionLimit;
        quantityPlus.setActive(canIncrease);
        quantityPlusTen.setActive(canIncrease);
        quantityMinus.style(style -> style.tooltips(Component.translatable(
                "gui.starboundmc.voxel_printing.quantity.decrease")));
        quantityMinusTen.style(style -> style.tooltips(Component.translatable(
                "gui.starboundmc.voxel_printing.quantity.decrease_ten")));
        Component increaseHint = canIncrease
                ? Component.translatable("gui.starboundmc.voxel_printing.quantity.increase")
                : Component.translatable("gui.starboundmc.voxel_printing.quantity.limit", selectionLimit);
        Component increaseTenHint = canIncrease
                ? Component.translatable("gui.starboundmc.voxel_printing.quantity.increase_ten")
                : Component.translatable("gui.starboundmc.voxel_printing.quantity.limit", selectionLimit);
        quantityPlus.style(style -> style.tooltips(increaseHint));
        quantityPlusTen.style(style -> style.tooltips(increaseTenHint));
    }

    private void updateStaticDetail(RecipeHolder<VoxelPrintingRecipe> holder) {
        if (selected == lastDetailedSelection && quantity == lastDetailedQuantity) {
            return;
        }
        lastDetailedSelection = selected;
        lastDetailedQuantity = quantity;
        VoxelPrintingRecipe recipe = holder.value();
        ItemStack result = resultStack(holder);
        resultTexture.setItems(result);
        resultIcon.style(style -> style.tooltips(result.getHoverName()));
        detailName.setText(result.getHoverName());
        detailName.style(style -> style.tooltips(result.getHoverName()));
        detailOutput.setText(Component.translatable(
                "gui.starboundmc.voxel_printing.output_count", result.getCount() * quantity));
        detailMeta.setText(Component.translatable("gui.starboundmc.voxel_printing.detail_meta",
                (long) recipe.voxelCost() * quantity,
                (long) recipe.printSeconds() * quantity, quantity));

        for (int i = 0; i < requirementTextures.length; i++) {
            ItemStack representative = i < recipe.materials().size()
                    ? representative(recipe.materials().get(i)) : ItemStack.EMPTY;
            requirementTextures[i].setItems(representative);
            requirementIcons[i].style(style -> style.tooltips(representative.isEmpty()
                    ? Component.translatable("gui.starboundmc.voxel_printing.no_material")
                    : representative.getHoverName()));
        }
    }

    private void updateRequirementCounts(VoxelPrintingRecipe recipe) {
        for (int i = 0; i < requirementCounts.length; i++) {
            if (i >= recipe.materials().size()) {
                requirementCounts[i].setText(Component.literal("—"));
                continue;
            }
            VoxelPrintingRecipe.MaterialEntry entry = recipe.materials().get(i);
            int current = 0;
            for (int slot = 0; slot < VoxelPrintingStationBlockEntity.MATERIAL_SLOTS; slot++) {
                ItemStack stack = menu.getSlot(slot).getItem();
                if (entry.ingredient().test(stack)) {
                    current += stack.getCount();
                }
            }
            long required = (long) entry.count() * quantity;
            requirementCounts[i].setText(Component.literal(current + "/" + required));
            requirementCounts[i].removeClass("voxel-requirement-missing");
            if (current < required) {
                requirementCounts[i].addClass("voxel-requirement-missing");
            }
        }
    }

    private int selectedQuantityCeiling() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || selected < 0 || selected >= recipes.size()) {
            return 1;
        }
        var snapshot = ClientPrintQueueState.snapshotAt(menu.blockPos());
        int outstanding = snapshot == null ? 0 : snapshot.outstandingCrafts();
        int queueLimit = Math.max(0,
                VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS - outstanding);
        int materialLimit = maxCraftsForMaterials(recipes.get(selected).value(), minecraft.level);
        return Math.max(1, Math.min(64, Math.min(materialLimit, queueLimit)));
    }

    private int maxCraftsForMaterials(VoxelPrintingRecipe recipe, Level level) {
        ItemStack[] simulated = new ItemStack[VoxelPrintingStationBlockEntity.MATERIAL_SLOTS];
        for (int slot = 0; slot < simulated.length; slot++) {
            simulated[slot] = menu.getSlot(slot).getItem().copy();
        }
        int crafts = 0;
        while (crafts < 64 && reserveOneCraft(recipe, simulated, level)) {
            crafts++;
        }
        return crafts;
    }

    private static boolean reserveOneCraft(
            VoxelPrintingRecipe recipe, ItemStack[] simulated, Level level) {
        if (!recipe.matches(new MachineRecipeInput(simulated), level)) {
            return false;
        }
        for (VoxelPrintingRecipe.MaterialEntry entry : recipe.materials()) {
            int stillNeeded = entry.count();
            for (int slot = 0; slot < simulated.length && stillNeeded > 0; slot++) {
                ItemStack stack = simulated[slot];
                if (stack.isEmpty() || !entry.ingredient().test(stack)) {
                    continue;
                }
                int taken = Math.min(stillNeeded, stack.getCount());
                stack.shrink(taken);
                stillNeeded -= taken;
            }
            if (stillNeeded > 0) {
                return false;
            }
        }
        return true;
    }

    private void showNoSelection() {
        detailName.setText(Component.translatable("gui.starboundmc.voxel_printing.hint.no_recipe"));
        detailOutput.setText(Component.empty());
        detailMeta.setText(Component.empty());
        detailStatus.setText(Component.translatable("gui.starboundmc.voxel_printing.hint.no_recipe"));
        resultTexture.setItems(ItemStack.EMPTY);
        printButton.setActive(false);
        quantityMinusTen.setActive(false);
        quantityMinus.setActive(false);
        quantityPlus.setActive(false);
        quantityPlusTen.setActive(false);
    }

    private void updateSelectedVisual() {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).button.removeClass("voxel-recipe-selected");
            if (i == selected) {
                rows.get(i).button.addClass("voxel-recipe-selected");
            }
        }
    }

    private boolean outputBlocked(ItemStack result) {
        ItemStack output = menu.getSlot(VoxelPrintingStationBlockEntity.OUTPUT_SLOT).getItem();
        return !output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result)
                || output.getCount() + result.getCount() > output.getMaxStackSize());
    }

    private long slotFingerprint() {
        long fingerprint = 1;
        for (int slot = 0; slot < VoxelPrintingStationBlockEntity.TOTAL_SLOTS; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            fingerprint = 31 * fingerprint + ItemStack.hashItemAndComponents(stack);
            fingerprint = 31 * fingerprint + stack.getCount();
        }
        return fingerprint;
    }

    private ItemStack resultStack(RecipeHolder<VoxelPrintingRecipe> holder) {
        var player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY
                : holder.value().getResultItem(player.registryAccess());
    }

    private static ItemStack representative(VoxelPrintingRecipe.MaterialEntry entry) {
        ItemStack[] items = entry.ingredient().getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0].copyWithCount(1);
    }

    private static final class RecipeRow {
        private final RecipeHolder<VoxelPrintingRecipe> holder;
        private final Button button;
        private Boolean ready;

        private RecipeRow(RecipeHolder<VoxelPrintingRecipe> holder, Button button) {
            this.holder = holder;
            this.button = button;
        }

        private void setReady(boolean next) {
            if (ready != null && ready == next) {
                return;
            }
            ready = next;
            button.removeClasses("voxel-recipe-ready", "voxel-recipe-unavailable");
            button.addClass(next ? "voxel-recipe-ready" : "voxel-recipe-unavailable");
        }
    }

    private record DetailState(int selected, int quantity, int balance,
                               boolean materials, boolean voxels, boolean capacity,
                               boolean outputBlocked, boolean running, int progress,
                               int outstanding, int queueHash, long slotFingerprint) {
    }

    private record QueueRow(SyncPrintQueuePacket.Entry entry, UIElement root, Label state) {
    }
}
