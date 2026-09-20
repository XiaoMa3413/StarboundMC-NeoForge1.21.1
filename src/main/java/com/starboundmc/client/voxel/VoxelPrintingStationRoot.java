package com.starboundmc.client.voxel;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.starboundmc.block.entity.VoxelPrintingStationBlockEntity;
import com.starboundmc.client.ClientPrintQueueState;
import com.starboundmc.client.PrintSubmissionState;
import com.starboundmc.client.ClientVoxelMachineState;
import com.starboundmc.client.ClientVoxelWalletState;
import com.starboundmc.client.ui.components.MaterialRequirementView;
import com.starboundmc.client.ui.components.QuantityStepper;
import com.starboundmc.client.ui.components.RecipeBrowser;
import com.starboundmc.client.ui.components.RecipeListRow;
import com.starboundmc.client.ui.components.SelectedItemView;
import com.starboundmc.menu.VoxelPrintingStationMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.StartPrintPacket;
import com.starboundmc.network.CancelPrintQueuePacket;
import com.starboundmc.network.SyncPrintQueuePacket;
import com.starboundmc.recipe.VoxelPrintingRecipe;
import com.starboundmc.recipe.PrintingCategory;
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

/**
 * Stable LDLib2 tree for list-driven voxel printing.
 *
 * <p>Fabrication spectacle (probes, beams, layer formation) lives in the world-space
 * {@link com.starboundmc.client.VoxelPrintingStationRenderer}. The GUI keeps the real vanilla
 * output socket plus recipe navigation, material requirements, quantity and queue controls.
 * Repeated interaction behaviour is delegated to semantic components under
 * {@code com.starboundmc.client.ui.components}.
 */
public final class VoxelPrintingStationRoot extends UIElement {
    // One panel size. The player inventory the menu fixes at x=148 needs 320px to fit, and a wider
    // panel would only add empty room the detail view cannot use — so there is no second layout.
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 240;
    // 146 + 170 = 316, so the region stops short of the shell's own 1px border instead of painting
    // over it. Every other element keeps a gutter too; this one used to run to the panel edge.
    private static final int WORKSPACE_W = 170;
    private static final int WORKSPACE_H = 120;
    private static final int QUEUE_W = 306;

    private final VoxelPrintingStationMenu menu;
    private final List<RecipeHolder<VoxelPrintingRecipe>> recipes;
    private final List<RecipeEntry> rows = new ArrayList<>();
    private final RecipeBrowser recipeBrowser;
    private final ScrollerView queueList = new ScrollerView();
    private final Map<UUID, QueueRow> queueRows = new LinkedHashMap<>();
    private final Label wallet = new Label();
    private final Label queueTitle = new Label();
    private final Label queueEmpty = new Label();
    private final Label machineStatus = new Label();
    private final SelectedItemView selectedItemView;
    private final Button queueToggle = new Button();
    private final PrintSubmissionState submission;
    private boolean showingQueue;
    private int selected = -1;
    private int quantity = 1;
    private int outstandingCrafts;
    private DetailState lastDetailState;

    public VoxelPrintingStationRoot(VoxelPrintingStationMenu menu, int left, int top,
                                    Component title, Component inventoryTitle,
                                    PrintSubmissionState submission) {
        this.menu = menu;
        this.submission = submission;
        selectedItemView = new SelectedItemView(
                WORKSPACE_W, WORKSPACE_H, QuantityStepper.Mode.COMPACT);
        recipeBrowser = new RecipeBrowser(128, 151);
        recipeBrowser.setEmptyHint(
                Component.translatable("gui.starboundmc.voxel_printing.hint.no_recipe"));
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
        var recipesPane = buildRecipePane();
        var workspacePane = buildWorkspacePane();
        var queuePane = buildQueuePane();
        // Queue is a secondary mode: opening it swaps the body (recipe browser + workspace)
        // for queue management and restores them on exit. It never holds items of its own.
        queuePane.setDisplay(false);
        queueToggle.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                showingQueue = !showingQueue;
                recipesPane.setDisplay(!showingQueue);
                workspacePane.setDisplay(!showingQueue);
                queuePane.setDisplay(showingQueue);
                updateQueueToggleText();
                event.stopPropagation();
            }
        });
        // No machine readout and no standing queue panel: the output frame carries the printing
        // state, the count line carries the queue amount, and the world renderer carries the machine.
        shell.addChildren(buildHeader(title), recipesPane, workspacePane, queuePane,
                buildInventory(inventoryTitle));
        addChild(shell);
        filterRecipes(PrintingCategory.SURVIVAL);
    }

    private UIElement buildHeader(Component title) {
        var header = VoxelUiSupport.positioned("machine-inventory-header", 4, 4, 280, 22);
        header.addChildren(VoxelUiSupport.positioned("voxel-printing-rail", 0, 0, 2, 22),
                VoxelUiSupport.label(title, "machine-inventory-title", 8, 1, 205, 11));
        configureLabel(machineStatus, "machine-status", 8, 13, 215, 8);
        // Narrower than the pre-toggle layout so the right-aligned wallet text cannot
        // run underneath the queue mode button sitting at the header's right edge. The header is
        // 280px wide, so the wallet ends where the button begins.
        configureLabel(wallet, "voxel-printing-wallet", 106, 5, 108, 12);
        wallet.textStyle(style -> style.textAlignHorizontal(Horizontal.RIGHT));
        configureButton(queueToggle, Component.translatable("gui.starboundmc.voxel_printing.tab.queue"),
                218, 2, 58, 18);
        // Its own class: this is a page-mode switch, not one of the retired quantity buttons.
        queueToggle.addClass("voxel-queue-toggle");
        // No room for a second status line beside the wallet, and the output frame already reports
        // whether the machine is working.
        machineStatus.setDisplay(false);
        header.addChildren(machineStatus, wallet, queueToggle);
        return header;
    }

    private UIElement buildRecipePane() {
        var pane = VoxelUiSupport.positioned("voxel-printing-recipe-pane", 6, 28, 136, 207);
        pane.addChild(VoxelUiSupport.label(
                Component.translatable("gui.starboundmc.voxel_printing.recipes"),
                "voxel-pane-title", 5, 3, 95, 8));

        var category = new Selector<PrintingCategory>();
        category.setCandidateUIProvider(VoxelPrintingStationRoot::categoryOption);
        category.setCandidates(List.of(PrintingCategory.values()));
        category.setSelected(PrintingCategory.SURVIVAL, false);
        category.setOnValueChanged(this::filterRecipes);
        category.addClass("voxel-printing-category");
        category.dialog.addClass("voxel-category-dialog");
        category.selectorStyle(style -> style.maxItemCount(4));
        category.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(4).top(14).width(128).height(18));
        pane.addChild(category);

        // The browser owns the list, its scrolling and its empty state; the page only feeds it the
        // catalogue and says which entries are relevant.
        recipeBrowser.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(4).top(36).width(128).height(151));
        // The browser builds one row per entry; the page keeps the row beside the recipe it stands
        // for, so availability and selection can be written back to it later.
        List<RecipeBrowser.Entry> entries = new ArrayList<>(recipes.size());
        for (int index = 0; index < recipes.size(); index++) {
            int slot = index;
            entries.add(new RecipeBrowser.Entry(resultStack(recipes.get(index)),
                    resultStack(recipes.get(index)).getCount(), () -> selectRecipe(slot)));
        }
        recipeBrowser.setEntries(entries);
        for (int index = 0; index < recipes.size(); index++) {
            rows.add(new RecipeEntry(recipes.get(index), recipeBrowser.row(index)));
        }
        pane.addChild(recipeBrowser);
        return pane;
    }

    private static UIElement categoryOption(PrintingCategory value) {
        var category = value == null ? PrintingCategory.SURVIVAL : value;
        var option = new UIElement().addClass("voxel-category-option");
        option.layout(layout -> layout.widthPercent(100).height(14));
        var icon = VoxelUiSupport.positioned("voxel-category-icon", 1, 1, 12, 12);
        var texture = switch (category) {
            case SURVIVAL -> Icons.WIDGET_SETTING;
            case MATERIALS -> Icons.RESOURCE;
            case MACHINES -> Icons.PROJECT;
            case BUILDING -> Icons.WIDGET_GROUP;
        };
        icon.style(style -> style.backgroundTexture(texture));
        var text = new Label().setText(Component.translatable(category.translationKey()));
        text.addClass("voxel-category-label");
        text.setAllowHitTest(false);
        text.setOverflowVisible(false);
        text.layout(layout -> layout.marginLeft(17).height(14));
        text.textStyle(style -> style.fontSize(7).adaptiveWidth(false)
                .textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        option.addChildren(icon, text);
        return option;
    }

    private void filterRecipes(PrintingCategory category) {
        // The page owns the rule for what matches; the browser owns applying it and rewinding.
        int first = recipeBrowser.setVisible(
                index -> category.matches(resultStack(recipes.get(index))));
        boolean selectionVisible = selected >= 0 && selected < rows.size()
                && rows.get(selected).view.isDisplayed();
        if (!selectionVisible) {
            selected = first;
            quantity = 1;
        }
        updateSelectedVisual();
        lastDetailState = null;
        refresh();
    }

    /** One catalogue click: select the recipe by its index in the catalogue. */
    private void selectRecipe(int index) {
        selected = index;
        updateSelectedVisual();
        lastDetailState = null;
        refresh();
    }

    private UIElement buildQueuePane() {
        // The queue page uses the whole body width: it replaces the recipe browser and the
        // fabrication workspace while it is open.
        int qw = QUEUE_W;
        var pane = VoxelUiSupport.positioned("voxel-printing-queue-pane", 6, 28, qw, WORKSPACE_H);

        queueTitle.addClasses("voxel-pane-title", "voxel-queue-title");
        queueTitle.setAllowHitTest(false);
        queueTitle.setOverflowVisible(false);
        queueTitle.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(5)
                .top(3)
                .width(qw - 10)
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
                .width(qw - 8)
                .height(WORKSPACE_H - 16));
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
        outstandingCrafts = outstanding;
        updateQueueToggleText();

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
        // The queue pane spans the whole body, so rows can use nearly its full inner width.
        int contentWidth = QUEUE_W - 26;
        var icon = VoxelUiSupport.positioned("voxel-queue-icon", 3, 4, 16, 16);
        icon.style(style -> style.backgroundTexture(new ItemStackTexture(result)));
        var name = VoxelUiSupport.label(result.getHoverName(), "voxel-queue-name", 23, 3, contentWidth - 19, 10);
        var state = VoxelUiSupport.label(Component.empty(), "voxel-queue-state", 23, 15, contentWidth - 19, 9);
        var requester = VoxelUiSupport.label(Component.literal(entry.requesterName()),
                "voxel-queue-requester", 4, 32, contentWidth - 22, 12);
        var track = new ProgressBar().setRange(0, 100);
        track.addClass("voxel-queue-progress");
        track.setAllowHitTest(false);
        track.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(4).top(26).width(contentWidth).height(4));
        track.barContainer.layout(layout -> layout.paddingAll(1));
        track.label.setDisplay(false);
        track.setDisplay(entry.active());
        var cancel = new Button();
        configureButton(cancel, Component.literal("×"), contentWidth - 14, 31, 18, 18);
        cancel.noText();
        var cancelIcon = VoxelUiSupport.positioned("voxel-cancel-icon", 4, 4, 10, 10);
        cancelIcon.style(style -> style.backgroundTexture(Icons.CLOSE));
        cancel.addChild(cancelIcon);
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
        return new QueueRow(entry, row, state, track);
    }

    /** Queue entry action carries the live queue count so the secondary mode stays discoverable. */
    private void updateQueueToggleText() {
        queueToggle.setText(showingQueue
                ? Component.translatable("gui.starboundmc.voxel_printing.tab.recipes")
                : Component.translatable("gui.starboundmc.voxel_printing.tab.queue.count",
                outstandingCrafts, VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS));
    }

    private static void updateQueueRowState(QueueRow row, int activeProgress) {
        row.progress.setProgress(activeProgress);
        row.state.setText(row.entry.active()
                ? Component.translatable("gui.starboundmc.voxel_printing.queue.active_progress",
                activeProgress + "%")
                : Component.translatable("gui.starboundmc.voxel_printing.queue.crafts",
                row.entry.crafts()));
    }

    /**
     * The detail region: one composition root owned by {@link SelectedItemView}. The page decides
     * where the region sits — it has to line up with the menu's slot layout — and wires the two
     * interactive children, and that is all it does here.
     */
    private UIElement buildWorkspacePane() {
        int dw = WORKSPACE_W;
        selectedItemView.addClass("voxel-printing-detail-pane");
        selectedItemView.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(146).top(28).width(dw).height(WORKSPACE_H));

        selectedItemView.quantityStepper().onChanged(target -> {
            quantity = target;
            lastDetailState = null;
            refresh();
        });

        var craftButton = selectedItemView.craftButton();
        craftButton.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && craftButton.isActive()
                    && selected >= 0 && selected < recipes.size()
                    && submission.begin(net.minecraft.Util.getMillis())) {
                submission.rememberQueueIds(queueRows.keySet());
                craftButton.setActive(false);
                ModNetwork.sendToServer(new StartPrintPacket(
                        menu.blockPos(), recipes.get(selected).id(), quantity));
                lastDetailState = null;
                refresh();
                event.stopPropagation();
            }
        });
        return selectedItemView;
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
        int outstanding = queueSnapshot == null ? 0 : queueSnapshot.outstandingCrafts();
        int balance = ClientVoxelWalletState.balance();
        Component stateText = Component.translatable(activeMachine
                ? "gui.starboundmc.voxel_printing.device.printing" : "gui.starboundmc.voxel_printing.device.ready");
        machineStatus.setText(stateText);
        wallet.setText(Component.translatable("gui.starboundmc.voxel_wallet",
                String.format(Locale.ROOT, "%,d", balance)));
        // The item on the bed, which the output frame previews while it is being made.
        ItemStack printItem = ItemStack.EMPTY;
        if (activeMachine && queueSnapshot != null && !queueSnapshot.entries().isEmpty()) {
            SyncPrintQueuePacket.Entry activeEntry = queueSnapshot.entries().getFirst();
            printItem = new ItemStack(BuiltInRegistries.ITEM.get(activeEntry.resultItemId()),
                    activeEntry.resultCount());
        }


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
        selectedItemView.quantityStepper().setValueAndMaximum(quantity, selectionLimit);

        for (RecipeEntry entry : rows) {
            boolean ready = maxCraftsForRequirements(entry.holder.value()) >= quantity
                    && outstanding + quantity <= VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS;
            if (entry.craftable == null || entry.craftable != ready) {
                entry.craftable = ready;
                entry.view.setCraftable(ready);
            }
        }

        boolean hasOutput = !menu.getSlot(VoxelPrintingStationBlockEntity.OUTPUT_SLOT).getItem().isEmpty();
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
        updateRequirements(recipe);
        updateItemIdentity(holder);
        // The output frame is the single place the item is shown: it previews the selected item while
        // idle, reveals colour as the craft progresses, and steps aside for the real item once there
        // is one — so the frame tracks the slot as well as the selection and is refreshed on every
        // state change, not only when the selection moves.
        selectedItemView.setOutput(resultStack(holder), progress, running, hasOutput,
                resultStack(holder).getHoverName().copy()
                        .append("\n").append(itemDescription(resultStack(holder))));
        // The count line doubles as the queue amount: the quantity being asked for before the craft,
        // how many are still to come while it runs.
        selectedItemView.setPrinting(running, outstanding);

        // Why the action is available or blocked. This travels as the action button's tooltip: the
        // button is the control the player is reaching for, so the explanation belongs with it.
        Component reasonTooltip;
        if (submissionStatus == PrintSubmissionState.Status.WAITING) {
            reasonTooltip = Component.translatable(submission.delayed(now)
                    ? "gui.starboundmc.voxel_printing.submit.delayed" : "gui.starboundmc.voxel_printing.submit.waiting");
        } else if (submissionStatus == PrintSubmissionState.Status.ACCEPTED) {
            reasonTooltip = Component.translatable("gui.starboundmc.voxel_printing.submit.accepted");
        } else if (submissionStatus == PrintSubmissionState.Status.REJECTED) {
            reasonTooltip = Component.translatable("gui.starboundmc.voxel_printing.submit.rejected");
        } else if (!materials) {
            reasonTooltip = Component.translatable("gui.starboundmc.voxel_printing.hint.materials");
        } else if (!capacity) {
            reasonTooltip = Component.translatable("gui.starboundmc.voxel_printing.hint.queue_full");
        } else if (outputBlocked) {
            reasonTooltip = Component.translatable("gui.starboundmc.voxel_printing.hint.output_wait");
        } else if (running) {
            reasonTooltip = Component.translatable(
                    "gui.starboundmc.voxel_printing.hint.enqueue_auto", progress + "%");
        } else {
            reasonTooltip = Component.translatable("gui.starboundmc.voxel_printing.hint.auto_materials");
        }
        boolean canPrint = materials && capacity && !submission.waiting();
        craftButton().setActive(canPrint);
        craftButton().setText(Component.translatable(submission.waiting()
                ? "gui.starboundmc.voxel_printing.submit.waiting" : "gui.starboundmc.voxel_printing.enqueue"));
        // The action button is where a blocked craft explains itself: it is the control the player
        // is reaching for, so the reason travels with it rather than in a separate readout.
        craftButton().style(style -> style.tooltips(reasonTooltip));
    }

    private Button craftButton() {
        return selectedItemView.craftButton();
    }

    /**
     * The item header states what is being made and how much this craft yields. Yield is shown for
     * the current quantity, so the number matches what the action will actually deliver, and the
     * item's own description plus the craft time travel in its tooltip.
     */
    private void updateItemIdentity(RecipeHolder<VoxelPrintingRecipe> holder) {
        VoxelPrintingRecipe recipe = holder.value();
        ItemStack result = resultStack(holder);
        Component tooltip = result.getHoverName().copy()
                .append("\n").append(itemDescription(result))
                .append("\n").append(Component.translatable(
                        "gui.starboundmc.voxel_printing.info.time",
                        (long) recipe.printSeconds() * quantity));
        selectedItemView.setItem(result.getHoverName(), result.getCount() * quantity, tooltip);
    }

    /** Crafts affordable right now, given materials, voxel balance and remaining queue room. */
    private int selectionLimit(VoxelPrintingRecipe recipe) {
        int outstanding = 0;
        var queueSnapshot = ClientPrintQueueState.snapshotAt(menu.blockPos());
        if (queueSnapshot != null) {
            outstanding = queueSnapshot.outstandingCrafts();
        }
        int queueLimit = Math.max(0,
                VoxelPrintingStationBlockEntity.MAX_OUTSTANDING_CRAFTS - outstanding);
        return Math.min(64, Math.min(maxCraftsForRequirements(recipe), queueLimit));
    }

    private void updateRequirements(VoxelPrintingRecipe recipe) {
        List<ItemStack> available = availableMaterialStacks();
        List<MaterialRequirementView.Line> lines = new ArrayList<>(recipe.materials().size());
        for (VoxelPrintingRecipe.MaterialEntry entry : recipe.materials()) {
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
            ItemStack representative = representative(entry);
            Component name = representative.getHoverName();
            Component tooltip = Component.translatable("gui.starboundmc.voxel_printing.requirement",
                    name, required, current);
            lines.add(new MaterialRequirementView.Line(representative, name, current, required, tooltip));
        }
        selectedItemView.setRequirements(lines);
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
        selectedItemView.clearRequirements();
        selectedItemView.clearItem(
                Component.translatable("gui.starboundmc.voxel_printing.hint.no_recipe"));
        craftButton().setActive(false);
        selectedItemView.quantityStepper().setValueAndMaximum(1, 1);
    }

    private void updateSelectedVisual() {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).view.setSelectedState(i == selected);
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

    private static final class RecipeEntry {
        private final RecipeHolder<VoxelPrintingRecipe> holder;
        private final RecipeListRow view;
        // Guard so the craftable rail state is only written when it actually changes,
        // mirroring the previous RecipeRow optimization on a hot per-tick path.
        private Boolean craftable;

        private RecipeEntry(RecipeHolder<VoxelPrintingRecipe> holder, RecipeListRow view) {
            this.holder = holder;
            this.view = view;
        }
    }

    private record DetailState(int selected, int quantity, int balance,
                               boolean materials, boolean capacity,
                               boolean outputBlocked, boolean running, int progress,
                               int outstanding, int queueHash, long slotFingerprint,
                               PrintSubmissionState.Status submissionStatus, boolean delayed) {
    }

    private record QueueRow(SyncPrintQueuePacket.Entry entry, UIElement root, Label state, ProgressBar progress) {
    }
}
