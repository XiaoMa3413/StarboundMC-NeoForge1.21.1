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
import com.starboundmc.client.PrintSubmissionState;
import com.starboundmc.client.ClientVoxelMachineState;
import com.starboundmc.client.ClientVoxelWalletState;
import com.starboundmc.menu.VoxelPrintingStationMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.StartPrintPacket;
import com.starboundmc.network.CancelPrintQueuePacket;
import com.starboundmc.network.SyncPrintQueuePacket;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.lwjgl.glfw.GLFW;

/** Stable LDLib2 tree for list-driven voxel printing. */
public final class VoxelPrintingStationRoot extends UIElement {
    private static final int PANEL_W = 440;
    private static final int COMPACT_W = 320;
    private static final int PANEL_H = 240;
    private static final int QUANTITY_ROW_Y = 85;
    private static final int QUANTITY_CONTROL_H = 13;
    private static final int WIDE_REQUIREMENT_CARD_W = 58;
    private static final int COMPACT_REQUIREMENT_CARD_W = 52;
    private static final int REQUIREMENT_CARD_H = 16;

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
    private final Label detailDescription = new Label();
    private final Label machineStatus = new Label();
    // Six visible requirement slots. Voxel entries use the same cards as every
    // other material entry; their available count comes from the wallet.
    private final Label[] requirementNames = {
            new Label(), new Label(), new Label(), new Label(), new Label(), new Label()
    };
    private final Button quantityMax = new Button();
    private final PrintSubmissionState submission;
    private final boolean compact;
    private final Button queueToggle = new Button();
    private boolean showingQueue;
    private final Label detailMeta = new Label();
    private final Label detailStatus = new Label();
    private final Label emptyState = new Label();
    private final Label[] requirementCounts = {
            new Label(), new Label(), new Label(), new Label(), new Label(), new Label()
    };
    private final UIElement[] requirementCards = new UIElement[6];
    private final UIElement[] requirementIcons = {
            new UIElement(), new UIElement(), new UIElement(), new UIElement(), new UIElement(), new UIElement()
    };
    private final ItemStackTexture[] requirementTextures = {
            new ItemStackTexture(), new ItemStackTexture(), new ItemStackTexture(),
            new ItemStackTexture(), new ItemStackTexture(), new ItemStackTexture()
    };
    private final ItemStackTexture ghostResultTexture = new ItemStackTexture().setColor(0x66FFFFFF);
    private final UIElement outputPreview = new UIElement();
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
                                    Component title, Component inventoryTitle, PrintSubmissionState submission,
                                    boolean compact) {
        this.menu = menu;
        this.submission = submission;
        this.compact = compact;
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
                .width(compact ? COMPACT_W : PANEL_W)
                .height(PANEL_H));
        var recipesPane = buildRecipePane();
        var detailPane = buildDetailPane();
        var queuePane = buildQueuePane();
        queuePane.setDisplay(!compact);
        queueToggle.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                showingQueue = !showingQueue;
                recipesPane.setDisplay(!showingQueue);
                detailPane.setDisplay(!showingQueue);
                queuePane.setDisplay(showingQueue);
                queueToggle.setText(Component.translatable(showingQueue
                        ? "gui.starboundmc.voxel_printing.tab.recipes" : "gui.starboundmc.voxel_printing.tab.queue"));
                event.stopPropagation();
            }
        });
        shell.addChildren(buildHeader(title), recipesPane, detailPane, queuePane,
                buildInventory(inventoryTitle));
        addChild(shell);
        updateSelectedVisual();
        refresh();
    }

    private UIElement buildHeader(Component title) {
        var header = VoxelUiSupport.positioned("machine-inventory-header", 4, 4, compact ? 280 : 432, 22);
        header.addChildren(VoxelUiSupport.positioned("voxel-printing-rail", 0, 0, 2, 22),
                VoxelUiSupport.label(title, "machine-inventory-title", 8, 1, 205, 11));
        configureLabel(machineStatus, "machine-status", 8, 13, 215, 8);
        configureLabel(wallet, "voxel-printing-wallet", 270, 5, 154, 12);
        wallet.textStyle(style -> style.textAlignHorizontal(Horizontal.RIGHT));
        if (compact) {
            machineStatus.setDisplay(false);
            wallet.layout(l -> l.left(8).top(13).width(205).height(9));
            wallet.textStyle(style -> style.textAlignHorizontal(Horizontal.LEFT));
            configureButton(queueToggle, Component.translatable("gui.starboundmc.voxel_printing.tab.queue"),
                    218, 2, 58, 18);
            queueToggle.addClass("voxel-quantity-button");
            header.addChild(queueToggle);
        }
        header.addChildren(machineStatus, wallet);
        return header;
    }

    private UIElement buildRecipePane() {
        var pane = VoxelUiSupport.positioned("voxel-printing-recipe-pane", 6, 28, 136, 120);
        pane.addChild(VoxelUiSupport.label(
                Component.translatable("gui.starboundmc.voxel_printing.recipes"),
                "voxel-pane-title", 5, 3, 95, 8));

        recipeList.addClass("voxel-recipe-list");
        recipeList.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(4)
                .top(13)
                .width(128)
                .height(103));
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
                "voxel-recipe-name", 23, 3, 66, 10);
        var amount = VoxelUiSupport.label(Component.literal("×" + result.getCount()),
                "voxel-recipe-amount", 23, 14, 66, 8);

        button.addChildren(icon, name, amount);
        button.style(style -> style.tooltips(result.getHoverName()));
        recipeList.addScrollViewChild(button);
        rows.add(new RecipeRow(holder, button));
    }

    private UIElement buildQueuePane() {
        var pane = VoxelUiSupport.positioned("voxel-printing-queue-pane", 340, 28, 94, 207);
        if (compact) pane.layout(l -> l.left(6).width(278).height(120));

        queueTitle.addClasses("voxel-pane-title", "voxel-queue-title");
        queueTitle.setAllowHitTest(false);
        queueTitle.setOverflowVisible(false);
        queueTitle.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(5)
                .top(3)
                .width(94)
                .height(10));
        queueTitle.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        queueList.addClass("voxel-print-queue-list");
        queueList.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(4)
                .top(14)
                .width(86)
                .height(187));
        queueList.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .minScrollPixel(8)
                .maxScrollPixel(20));
        if (compact) queueList.layout(l -> l.width(270).height(102));
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
        row.setOverflowVisible(false);
        row.layout(layout -> layout.widthPercent(100).height(52));
        if (entry.active()) row.addClass("voxel-queue-row-active");
        ItemStack result = new ItemStack(BuiltInRegistries.ITEM.get(entry.resultItemId()), entry.resultCount());
        var icon = VoxelUiSupport.positioned("voxel-queue-icon", 3, 4, 16, 16);
        icon.style(style -> style.backgroundTexture(new ItemStackTexture(result)));
        int contentWidth = compact ? 258 : 84;
        var name = VoxelUiSupport.label(result.getHoverName(), "voxel-queue-name", 23, 3, contentWidth - 19, 10);
        var state = VoxelUiSupport.label(Component.empty(), "voxel-queue-state", 23, 15, contentWidth - 19, 9);
        var requester = VoxelUiSupport.label(Component.literal(entry.requesterName()),
                "voxel-queue-requester", 4, 32, contentWidth - 22, 12);
        var track = VoxelUiSupport.positioned("queue-progress-track", 4, 26, contentWidth, 2);
        var fill = VoxelUiSupport.positioned("queue-progress-fill", 0, 0, 0, 2);
        track.addChild(fill);
        track.setDisplay(entry.active());
        var cancel = new Button();
        configureButton(cancel, Component.literal("×"), contentWidth - 14, 31, 18, 18);
        cancel.addClass("voxel-queue-cancel");
        var player = Minecraft.getInstance().player;
        boolean canCancel = !entry.active() && player != null && player.getUUID().equals(entry.requesterId());
        cancel.setActive(canCancel);
        cancel.style(style -> style.tooltips(Component.translatable(entry.active()
                ? "gui.starboundmc.voxel_printing.queue.active_no_cancel"
                : canCancel ? "gui.starboundmc.voxel_printing.queue.cancel"
                : "gui.starboundmc.voxel_printing.queue.not_owner")));
        cancel.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && cancel.isActive()) {
                ModNetwork.sendToServer(new CancelPrintQueuePacket(menu.blockPos(), entry.id()));
                event.stopPropagation();
            }
        });
        row.style(style -> style.tooltips(Component.translatable("gui.starboundmc.voxel_printing.queue.tooltip",
                result.getHoverName(), entry.requesterName(), entry.crafts())));
        row.addChildren(icon, name, state, requester, track, cancel);
        return new QueueRow(entry, row, state, fill, contentWidth);
    }

    private static void updateQueueRowState(QueueRow row, int activeProgress) {
        row.fill.layout(layout -> layout.width(row.progressWidth * activeProgress / 100.0F));
        row.state.setText(row.entry.active()
                ? Component.translatable("gui.starboundmc.voxel_printing.queue.active_progress",
                activeProgress + "%")
                : Component.translatable("gui.starboundmc.voxel_printing.queue.crafts",
                row.entry.crafts()));
    }

    private UIElement buildDetailPane() {
        int detailWidth = compact ? 174 : 188;
        var pane = VoxelUiSupport.positioned("voxel-printing-detail-pane", 146, 28, detailWidth, 120);
        outputPreview.addClass("voxel-printing-output-preview");
        outputPreview.setAllowHitTest(false);
        outputPreview.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(1).top(1).width(16).height(16));
        outputPreview.style(style -> style.backgroundTexture(ghostResultTexture));
        configureLabel(detailName, "voxel-printing-detail-name", 30, 2, detailWidth - 35, 10);
        configureLabel(detailOutput, "voxel-printing-detail-output", 30, 12, detailWidth - 35, 9);
        configureLabel(detailDescription, "voxel-printing-detail-description", 5, 21,
                detailWidth - 10, 8);
        // Keep the output slot beside the selected result. The quantity controls occupy the
        // bottom action row, so placing the socket there would put the -10 button over it.
        var outputSocket = VoxelUiSupport.slotSocket("voxel-printing-output-socket", 4, 2);
        outputSocket.addChild(outputPreview);
        int cardWidth = compact ? COMPACT_REQUIREMENT_CARD_W : WIDE_REQUIREMENT_CARD_W;
        int cardGap = compact ? 4 : 2;
        int cardTextWidth = cardWidth - 17;
        for (int i = 0; i < requirementCards.length; i++) {
            int x = 5 + (i % 3) * (cardWidth + cardGap);
            int y = 30 + (i / 3) * 19;
            var card = VoxelUiSupport.positioned("voxel-requirement-card", x, y,
                    cardWidth, REQUIREMENT_CARD_H);
            requirementCards[i] = card;
            card.setAllowHitTest(true);
            card.setDisplay(false);
            var icon = requirementIcons[i];
            icon.setAllowHitTest(false);
            icon.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(1).top(2).width(12).height(12));
            var texture = requirementTextures[i];
            icon.style(style -> style.backgroundTexture(texture));
            configureLabel(requirementNames[i], "voxel-requirement-name", 15, 0, cardTextWidth, 7);
            requirementNames[i].setAllowHitTest(false);
            configureLabel(requirementCounts[i], "voxel-requirement-count", 15, 7, cardTextWidth, 8);
            requirementCounts[i].setAllowHitTest(false);
            card.addChildren(icon, requirementNames[i], requirementCounts[i]);
            pane.addChild(card);
        }
        configureLabel(detailMeta, "voxel-printing-detail-meta", 5, 67, detailWidth - 10, 8);
        configureLabel(detailStatus, "voxel-printing-detail-status", 5, 76, detailWidth - 10, 9);
        int quantityLeft = compact ? 14 : 16;
        int quantityLabelWidth = compact ? 34 : 42;
        int quantityMaxWidth = compact ? 30 : 32;
        configureQuantityButton(quantityMinusTen, "−10", quantityLeft, 20, -10);
        configureQuantityButton(quantityMinus, "−", quantityLeft + 22, 15, -1);
        configureLabel(quantityLabel, "voxel-printing-quantity", quantityLeft + 39, QUANTITY_ROW_Y,
                quantityLabelWidth, QUANTITY_CONTROL_H);
        quantityLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        configureQuantityButton(quantityPlus, "+", quantityLeft + 41 + quantityLabelWidth, 15, 1);
        configureQuantityButton(quantityPlusTen, "+10", quantityLeft + 58 + quantityLabelWidth, 21, 10);
        configureButton(quantityMax, Component.translatable("gui.starboundmc.voxel_printing.quantity.max"),
                quantityLeft + 81 + quantityLabelWidth, QUANTITY_ROW_Y, quantityMaxWidth,
                QUANTITY_CONTROL_H);
        quantityMax.addClass("voxel-quantity-button");
        quantityMax.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && quantityMax.isActive()) {
                quantity = selectedQuantityCeiling();
                lastDetailState = null;
                refresh();
                event.stopPropagation();
            }
        });
        configureButton(printButton, Component.translatable("gui.starboundmc.voxel_printing.enqueue"),
                5, 101, detailWidth - 10, 16);
        printButton.addClass("voxel-printing-action");
        printButton.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && printButton.isActive()
                    && selected >= 0 && selected < recipes.size()
                    && submission.begin(net.minecraft.Util.getMillis())) {
                submission.rememberQueueIds(queueRows.keySet());
                printButton.setActive(false);
                ModNetwork.sendToServer(new StartPrintPacket(menu.blockPos(), recipes.get(selected).id(), quantity));
                lastDetailState = null;
                refresh();
                event.stopPropagation();
            }
        });
        pane.addChildren(detailName, detailOutput, detailDescription, detailMeta, detailStatus, outputSocket,
                quantityMinusTen, quantityMinus, quantityLabel, quantityPlus, quantityPlusTen, quantityMax, printButton);
        return pane;
    }

    private static void configureLabel(Label label, String styleClass, int x, int y, int width, int height) {
        label.addClass(styleClass);
        label.setOverflowVisible(false);
        label.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(width).height(height));
        label.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false)
                .textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
    }

    private static void configureButton(Button button, Component text, int x, int y, int width, int height) {
        button.setText(text);
        button.text.setAllowHitTest(false);
        button.text.setOverflowVisible(false);
        button.text.layout(l -> l.widthPercent(100).heightPercent(100).marginHorizontal(0));
        button.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y)
                .width(width).height(height).paddingAll(1));
        button.textStyle(style -> style.adaptiveWidth(false).textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
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
                .left(left).top(QUANTITY_ROW_Y).width(width).height(QUANTITY_CONTROL_H).paddingAll(1));
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
        // The vanilla item is drawn at menu slot.x/y. The 18px socket surrounds it by one pixel.
        var section = VoxelUiSupport.positioned("voxel-inventory-section", 144, 151, 172, 84);
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
        long now = net.minecraft.Util.getMillis();
        var submissionStatus = submission.status(now);
        for (QueueRow row : queueRows.values()) {
            row.root.removeClass("queue-recent");
            if (submissionStatus == PrintSubmissionState.Status.ACCEPTED && minecraft.player != null
                    && row.entry.requesterId().equals(minecraft.player.getUUID())
                    && submission.isNewQueueEntry(row.entry.id())) row.root.addClass("queue-recent");
        }
        boolean activeMachine = machineSnapshot != null && machineSnapshot.progress() > 0;
        machineStatus.setText(Component.translatable(activeMachine
                ? "gui.starboundmc.voxel_printing.device.printing" : "gui.starboundmc.voxel_printing.device.ready"));
        boolean hasOutput = !menu.getSlot(VoxelPrintingStationBlockEntity.OUTPUT_SLOT).getItem().isEmpty();
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
        int materialLimit = maxCraftsForRequirements(recipe);
        int queueLimit = Math.max(0,
                VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS - outstanding);
        int selectionLimit = Math.min(64, Math.min(materialLimit, queueLimit));
        int quantityCeiling = Math.max(1, selectionLimit);
        if (quantity > quantityCeiling) {
            quantity = quantityCeiling;
            lastDetailState = null;
        }

        for (RecipeRow row : rows) {
            boolean ready = maxCraftsForRequirements(row.holder.value()) >= quantity
                    && outstanding + quantity <= VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS;
            row.setReady(ready);
        }

        ItemStack result = resultStack(holder);
        boolean materials = materialLimit >= quantity;
        boolean capacity = outstanding + quantity
                <= VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS;
        boolean outputBlocked = queueSnapshot != null && !queueSnapshot.entries().isEmpty()
                && outputBlocked(new ItemStack(BuiltInRegistries.ITEM.get(queueSnapshot.entries().getFirst().resultItemId()),
                queueSnapshot.entries().getFirst().resultCount()));
        boolean running = machineSnapshot != null && machineSnapshot.progress() > 0;

        DetailState state = new DetailState(selected, quantity, balance, materials,
                capacity, outputBlocked, running, progress, outstanding,
                queueSnapshot == null ? 0 : queueSnapshot.hashCode(), slotFingerprint(), submissionStatus, submission.delayed(now));
        if (state.equals(lastDetailState)) {
            return;
        }
        lastDetailState = state;
        updateStaticDetail(holder);
        updateRequirementCounts(recipe);
        outputPreview.setVisible(!hasOutput);

        Component reason;
        Component reasonTooltip;
        if (submissionStatus == PrintSubmissionState.Status.WAITING) {
            reason = Component.translatable(submission.delayed(now)
                    ? "gui.starboundmc.voxel_printing.submit.delayed" : "gui.starboundmc.voxel_printing.submit.waiting");
            reasonTooltip = reason;
        } else if (submissionStatus == PrintSubmissionState.Status.ACCEPTED) {
            reason = Component.translatable("gui.starboundmc.voxel_printing.submit.accepted");
            reasonTooltip = reason;
        } else if (submissionStatus == PrintSubmissionState.Status.REJECTED) {
            reason = Component.translatable("gui.starboundmc.voxel_printing.submit.rejected");
            reasonTooltip = reason;
        } else if (!materials) {
            reason = Component.translatable("gui.starboundmc.voxel_printing.hint.materials");
            reasonTooltip = reason;
        } else if (!capacity) {
            reason = Component.translatable("gui.starboundmc.voxel_printing.hint.queue_full");
            reasonTooltip = reason;
        } else if (outputBlocked) {
            reason = Component.translatable("gui.starboundmc.voxel_printing.hint.output_wait");
            reasonTooltip = reason;
        } else if (running) {
            reason = Component.translatable(
                    "gui.starboundmc.voxel_printing.hint.enqueue_while_printing", progress + "%");
            reasonTooltip = Component.translatable(
                    "gui.starboundmc.voxel_printing.hint.enqueue_auto", progress + "%");
        } else {
            reason = Component.translatable("gui.starboundmc.voxel_printing.hint.enqueue_ready");
            reasonTooltip = Component.translatable(
                    "gui.starboundmc.voxel_printing.hint.auto_materials");
        }
        detailStatus.setText(reason);
        detailStatus.style(style -> style.tooltips(reasonTooltip));
        boolean canPrint = materials && capacity && !submission.waiting();
        printButton.setActive(canPrint);
        printButton.setText(Component.translatable(submission.waiting()
                ? "gui.starboundmc.voxel_printing.submit.waiting" : "gui.starboundmc.voxel_printing.enqueue"));
        detailStatus.removeClasses("status-warning", "status-success");
        detailStatus.addClass((!materials || !capacity || submissionStatus == PrintSubmissionState.Status.REJECTED)
                && submissionStatus != PrintSubmissionState.Status.ACCEPTED ? "status-warning" : "status-success");
        printButton.style(style -> style.tooltips(reasonTooltip));
        quantityLabel.setText(Component.literal("×" + quantity));
        quantityMinus.setActive(quantity > 1);
        quantityMinusTen.setActive(quantity > 1);
        boolean canIncrease = quantity < selectionLimit;
        quantityPlus.setActive(canIncrease);
        quantityPlusTen.setActive(canIncrease);
        quantityMax.setActive(canIncrease);
        quantityMax.style(style -> style.tooltips(Component.translatable(
                "gui.starboundmc.voxel_printing.quantity.maximum", selectionLimit)));
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
        ghostResultTexture.setItems(result.copyWithCount(1));
        detailName.setText(result.getHoverName());

        detailOutput.setText(Component.translatable(
                "gui.starboundmc.voxel_printing.output_count", result.getCount() * quantity));
        Component description = itemDescription(result);
        Component detailTooltip = result.getHoverName().copy().append("\n").append(description);
        detailName.style(style -> style.tooltips(detailTooltip));
        detailDescription.setText(description);
        detailDescription.style(style -> style.tooltips(detailTooltip));
        outputPreview.style(style -> style.tooltips(detailTooltip));
        detailMeta.setText(Component.translatable("gui.starboundmc.voxel_printing.detail_meta",
                (long) recipe.printSeconds() * quantity, quantity));

        for (int i = 0; i < requirementTextures.length; i++) {
            ItemStack representative = i < recipe.materials().size()
                    ? representative(recipe.materials().get(i)) : ItemStack.EMPTY;
            requirementTextures[i].setItems(representative);
            requirementCards[i].setDisplay(i < recipe.materials().size());
        }
    }

    private void updateRequirementCounts(VoxelPrintingRecipe recipe) {
        List<ItemStack> available = availableMaterialStacks();
        int cardTextWidth = (compact ? COMPACT_REQUIREMENT_CARD_W : WIDE_REQUIREMENT_CARD_W) - 17;
        for (int i = 0; i < requirementCounts.length; i++) {
            requirementCounts[i].removeClass("voxel-requirement-missing");
            if (i >= recipe.materials().size()) {
                requirementNames[i].setText(Component.empty());
                requirementCounts[i].setText(Component.empty());
                continue;
            }
            VoxelPrintingRecipe.MaterialEntry entry = recipe.materials().get(i);
            long current;
            if (entry.isVoxel()) {
                current = ClientVoxelWalletState.balance();
            } else {
                current = 0;
                for (ItemStack stack : available) {
                    if (entry.ingredient().test(stack)) {
                        current += stack.getCount();
                    }
                }
            }
            long required = (long) entry.count() * quantity;
            Component name = representative(entry).getHoverName();
            String count = Long.toString(required);
            String prefix = current < required ? "! " : "× ";
            // Exact values remain available in the tooltip for very large custom recipes.
            if (Minecraft.getInstance().font.width(prefix + count) * (6.0F / 9.0F) > cardTextWidth) {
                count = "…";
            }
            requirementNames[i].setText(name);
            requirementCounts[i].setText(Component.literal(prefix + count));
            Component tooltip = Component.translatable("gui.starboundmc.voxel_printing.requirement",
                    name, required, current);
            requirementCards[i].style(style -> style.tooltips(tooltip));
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
        int materialLimit = maxCraftsForRequirements(recipes.get(selected).value());
        return Math.max(1, Math.min(64, Math.min(materialLimit, queueLimit)));
    }

    private int maxCraftsForRequirements(VoxelPrintingRecipe recipe) {
        List<ItemStack> simulated = availableMaterialStacks();
        // A recipe without a voxel material does not limit the quantity by wallet balance.
        long voxelMaterialCount = recipe.voxelMaterialCount();
        int voxelLimit = voxelMaterialCount > 0
                ? (int) Math.min(64L, ClientVoxelWalletState.balance() / voxelMaterialCount) : 64;
        int craftLimit = Math.min(64, voxelLimit);
        int crafts = 0;
        while (crafts < craftLimit && recipe.reserveMaterials(simulated).isPresent()) {
            crafts++;
        }
        return crafts;
    }

    private List<ItemStack> availableMaterialStacks() {
        var player = Minecraft.getInstance().player;
        List<ItemStack> available = new ArrayList<>(
                player == null ? 0 : player.getInventory().items.size());
        if (player != null) {
            for (ItemStack stack : player.getInventory().items) {
                available.add(stack.copy());
            }
        }
        return available;
    }

    private void showNoSelection() {
        for (UIElement card : requirementCards) {
            card.setDisplay(false);
        }
        detailName.setText(Component.translatable("gui.starboundmc.voxel_printing.hint.no_recipe"));
        detailOutput.setText(Component.empty());
        detailDescription.setText(Component.empty());

        detailMeta.setText(Component.empty());
        detailStatus.setText(Component.translatable("gui.starboundmc.voxel_printing.hint.no_recipe"));
        ghostResultTexture.setItems(ItemStack.EMPTY);
        outputPreview.setVisible(false);
        printButton.setActive(false);
        quantityMinusTen.setActive(false);
        quantityMinus.setActive(false);
        quantityPlus.setActive(false);
        quantityPlusTen.setActive(false);
        quantityMax.setActive(false);
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
        for (int slot = 0; slot < menu.slots.size(); slot++) {
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

    private static Component itemDescription(ItemStack stack) {
        var minecraft = Minecraft.getInstance();
        List<Component> lines = stack.getTooltipLines(
                Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL);
        if (lines.size() <= 1) {
            return Component.translatable("gui.starboundmc.voxel_printing.description.empty");
        }
        MutableComponent description = Component.empty();
        for (int line = 1; line < lines.size(); line++) {
            if (line > 1) {
                description.append("\n");
            }
            description.append(lines.get(line));
        }
        return description;
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
                               boolean materials, boolean capacity,
                               boolean outputBlocked, boolean running, int progress,
                               int outstanding, int queueHash, long slotFingerprint,
                               PrintSubmissionState.Status submissionStatus, boolean delayed) {
    }

    private record QueueRow(SyncPrintQueuePacket.Entry entry, UIElement root, Label state, UIElement fill,
                            int progressWidth) {
    }
}
