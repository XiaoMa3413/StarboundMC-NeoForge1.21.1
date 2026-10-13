package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.starboundmc.item.ModItems;
import com.starboundmc.item.MatterManipulatorItem;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.ShipAiActionPacket;
import com.starboundmc.story.*;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.starboundmc.client.shipai.NovaTerminalViewState.Page;

/** Three-mode terminal shell. All gameplay actions remain server requests. */
final class NovaCommandLayout extends UIElement {
    private final int containerId;
    private final Consumer<Component> explain;
    private final NovaTerminalViewState reading = ClientShipAiTerminalState.current().view();
    private final Map<Page, UIElement> pages = new EnumMap<>(Page.class);
    private final Map<Page, Button> navigation = new EnumMap<>(Page.class);
    private final Map<NovaTask, Button> tasks = new EnumMap<>(NovaTask.class);
    private final Map<NovaTask, Label> taskStates = new EnumMap<>(NovaTask.class);
    private final Map<NovaTask, UIElement> taskEntries = new EnumMap<>(NovaTask.class);
    private final Map<NovaTask, UIElement> archives = new EnumMap<>(NovaTask.class);
    private final Label contextTitle = label("", "command-context-title");
    private final Button contextOpen = button("gui.starboundmc.tasks.view", this::openContext);
    private final Label taskGuide = label("", "command-copy");
    private final Label taskSteps = label("", "command-muted");
    private final Label taskCost = label("gui.starboundmc.tasks.repair_cost", "command-cost");
    private final Label taskReward = label("", "command-muted");
    private final Label claimed = label("gui.starboundmc.tasks.claimed", "command-complete");
    private final Label feedback = label("", "command-muted");
    private final Label taskEmpty = label("gui.starboundmc.command.empty_tasks", "command-muted");
    private final Label mainTasks = label("gui.starboundmc.tasks.main", "command-section");
    private final Label preparationTasks = label("gui.starboundmc.tasks.preparation", "command-section");
    private final UIElement taskDetails = column("command-task-details");
    private final UIElement rewardLine = row("command-reward-line");
    private final UIElement actions = row("command-actions");
    private final Button track = button("gui.starboundmc.tasks.track", () -> request(false));
    private final Button claim = button("gui.starboundmc.tasks.claim", () -> request(true));
    private final Button ask = button("gui.starboundmc.tasks.ask", this::askSelected);
    private final Button activeFilter = button("gui.starboundmc.tasks.active", () -> filter(false));
    private final Button doneFilter = button("gui.starboundmc.tasks.completed", () -> filter(true));
    private ScrollerView taskScroller;
    private NovaTaskProgress progress = NovaTaskProgress.DEFAULT;
    private boolean unlocked;
    private boolean transmitting;
    private long pending;
    private int pendingTicks;
    private NovaTask pendingTask;
    private boolean pendingReward;
    private int expectedTracked;
    private boolean requestNotApplied;
    private long lastSequence = -1;
    private boolean lastCapacity;
    private int inventoryState;

    NovaCommandLayout(int containerId, UIElement portraitPane, UIElement communications, Consumer<Component> explain) {
        this.containerId = containerId;
        this.explain = explain;
        addClass("command-shell");
        setOverflowVisible(false);
        layout(l -> l.widthPercent(96).heightPercent(94).maxWidth(560).maxHeight(330)
                .flexDirection(FlexDirection.COLUMN));
        UIElement header = row("command-header");
        header.layout(l -> l.height(32).flexShrink(0).paddingLeft(12));
        Label brand = label("gui.starboundmc.ship_ai.title", "command-brand");
        brand.layout(l -> l.flex(1).minWidth(50).widthAuto());
        header.addChild(brand);
        for (Page value : Page.values()) {
            Button tab = button("gui.starboundmc.command." + value.name().toLowerCase(java.util.Locale.ROOT), () -> show(value));
            tab.addClass("command-nav-button");
            tab.layout(l -> l.width(68).heightPercent(100).flexShrink(0));
            tab.textStyle(s -> s.textAlignHorizontal(Horizontal.CENTER));
            navigation.put(value, tab);
            header.addChild(tab);
        }
        Button close = button("gui.starboundmc.command.close", () -> {
            if (Minecraft.getInstance().screen != null) Minecraft.getInstance().screen.onClose();
        });
        close.addClass("command-link");
        close.layout(l -> l.width(34).heightPercent(100).flexShrink(0));
        close.textStyle(s -> s.textAlignHorizontal(Horizontal.CENTER));
        close.style(s -> s.tooltips(Component.translatable("gui.starboundmc.command.close_tooltip")));
        header.addChild(close);
        UIElement body = column("command-body");
        body.layout(l -> l.flex(1).minHeight(0).widthPercent(100));
        UIElement comms = row("command-comms");
        boolean compact = compact();
        comms.layout(l -> l.widthPercent(100).heightPercent(100).paddingAll(compact ? 10 : 16)
                .gapAll(compact ? 12 : 20).alignItems(AlignItems.STRETCH));
        portraitPane.layout(l -> l.widthPercent(30).heightPercent(100).flexShrink(0));
        UIElement conversation = column("command-conversation");
        conversation.layout(l -> l.flex(1).minWidth(0).minHeight(0).gapAll(compact ? 6 : 10));
        UIElement context = row("command-context");
        context.layout(l -> l.widthPercent(100).minHeight(21).flexShrink(0).gapAll(6));
        contextTitle.layout(l -> l.flex(1).minWidth(0).widthAuto());
        contextOpen.addClass("command-link");
        contextOpen.layout(l -> l.width(compact ? 67 : 84).height(21).flexShrink(0).paddingHorizontal(2));
        divider(context);
        context.addChildren(contextTitle, contextOpen);
        conversation.addChildren(context, communications);
        comms.addChildren(portraitPane, conversation);
        pages.put(Page.COMMS, comms);
        pages.put(Page.TASKS, taskPage());
        pages.put(Page.ARCHIVE, archive());
        for (UIElement view : pages.values()) {
            view.layout(l -> l.widthPercent(100).heightPercent(100).minHeight(0));
            body.addChild(view);
        }
        addChildren(header, body);
        applyPage();
    }

    static boolean compact() {
        var window = Minecraft.getInstance().getWindow();
        return window.getGuiScaledWidth() < 440 || window.getGuiScaledHeight() < 300;
    }

    private UIElement taskPage() {
        UIElement page = column("command-task-page");
        page.layout(l -> l.paddingAll(compact() ? 10 : 16).gapAll(10));
        UIElement heading = row("command-page-heading");
        heading.layout(l -> l.widthPercent(100).minHeight(22).flexShrink(0).gapAll(6));
        Label title = label("gui.starboundmc.command.tasks", "command-title");
        title.layout(l -> l.flex(1).widthAuto().minWidth(0));
        activeFilter.addClass("command-filter"); doneFilter.addClass("command-filter");
        activeFilter.layout(l -> l.width(compact() ? 56 : 64).height(22).flexShrink(0).paddingHorizontal(2));
        doneFilter.layout(l -> l.width(compact() ? 108 : 130).height(22).flexShrink(0).paddingHorizontal(2));
        divider(heading);
        heading.addChildren(title, activeFilter, doneFilter);
        ScrollerView view = scroll();
        taskScroller = view;
        view.layout(l -> l.widthPercent(100).flex(1).minHeight(0));
        bindReadingPosition(view, Page.TASKS, reading::taskScroll, reading::taskScroll);
        for (NovaTask task : NovaTask.values()) {
            if (task == NovaTask.CONTACT) view.addScrollViewChild(mainTasks);
            if (task == NovaTask.LIFE_SUPPORT) view.addScrollViewChild(preparationTasks);
            UIElement entry = column("command-task-entry");
            entry.layout(l -> l.widthPercent(100).flexShrink(0));
            Button b = button(task.translation("title"), () -> {
                reading.select(task);
                requestNotApplied = false;
                refresh();
            });
            b.addClass("command-task-row");
            b.layout(l -> l.widthPercent(100).heightAuto().minHeight(28).paddingAll(7)
                    .gapAll(6).alignItems(AlignItems.CENTER).flexShrink(0));
            b.text.layout(l -> l.flex(1).minWidth(0).heightAuto());
            b.textStyle(s -> s.adaptiveHeight(true));
            Label status = label("", "command-task-state");
            status.layout(l -> l.width(compact() ? 48 : 62).flexShrink(0));
            status.textStyle(s -> s.textAlignHorizontal(Horizontal.RIGHT));
            b.addChild(status);
            tasks.put(task, b); taskStates.put(task, status); taskEntries.put(task, entry);
            entry.addChild(b); view.addScrollViewChild(entry);
        }
        view.addScrollViewChild(taskEmpty);
        taskDetails.layout(l -> l.widthPercent(100).paddingHorizontal(compact() ? 10 : 16)
                .paddingVertical(compact() ? 6 : 8).gapAll(6).flexShrink(0));
        rewardLine.layout(l -> l.widthPercent(100).minHeight(16).gapAll(5).flexShrink(0));
        UIElement icon = new UIElement();
        icon.setAllowHitTest(false);
        icon.layout(l -> l.width(12).height(12).flexShrink(0));
        icon.style(s -> s.backgroundTexture(new ItemStackTexture(new ItemStack(ModItems.MATTER_MANIPULATOR_MODULE.get()))));
        taskReward.layout(l -> l.flex(1).minWidth(0).widthAuto());
        rewardLine.addChildren(icon, taskReward);
        actions.layout(l -> l.widthPercent(100).gapAll(8).minHeight(22).flexShrink(0));
        track.addClass("command-primary");
        track.layout(l -> l.width(compact() ? 110 : 132).height(23).paddingLeft(14).flexShrink(0));
        ask.addClass("command-link");
        ask.layout(l -> l.width(compact() ? 82 : 100).height(23).flexShrink(0));
        claim.addClass("command-primary");
        claim.layout(l -> l.width(compact() ? 110 : 132).height(23).paddingLeft(14).flexShrink(0));
        actions.addChildren(track, ask, claim);
        taskDetails.addChildren(taskGuide, taskSteps, taskCost, rewardLine, claimed, actions, feedback);
        taskEntries.get(reading.selected()).addChild(taskDetails);
        page.addChildren(heading, view);
        return page;
    }

    private UIElement archive() {
        UIElement page = column("command-archive-page");
        page.layout(l -> l.paddingAll(compact() ? 10 : 16).gapAll(10));
        Label title = label("gui.starboundmc.command.archive", "command-title");
        ScrollerView view = scroll();
        view.layout(l -> l.widthPercent(100).flex(1).minHeight(0));
        bindReadingPosition(view, Page.ARCHIVE, reading::archiveScroll, reading::archiveScroll);
        for (NovaTask task : NovaTask.values()) {
            UIElement entry = column("command-archive-entry");
            entry.layout(l -> l.widthPercent(100).gapAll(8).paddingVertical(10).flexShrink(0));
            divider(entry);
            entry.addChildren(label(task.translation("archive_title"), "command-accent"), label(task.translation("archive"), "command-copy"));
            Button related = button("gui.starboundmc.tasks.related", () -> {
                reading.taskScroll(0);
                reading.openTask(task, progress);
                refresh();
            });
            related.addClass("command-link");
            related.layout(l -> l.height(22).width(110).paddingHorizontal(2));
            entry.addChild(related); archives.put(task, entry); view.addScrollViewChild(entry);
        }
        page.addChildren(title, view);
        return page;
    }

    void tick(boolean transmitting) {
        revealSelectedTask();
        boolean activityChanged = this.transmitting != transmitting;
        this.transmitting = transmitting;
        if (!ClientShipStoryState.hasSnapshot(containerId)) return;
        var snapshot = ClientShipStoryState.snapshot(containerId);
        progress = ClientShipStoryState.tasks(containerId);
        if (requestNotApplied && (pendingReward ? progress.claimed(pendingTask) : progress.trackedTask() == expectedTracked))
            requestNotApplied = false;
        if (pending > 0 && (ClientShipStoryState.consumeAcknowledgement(containerId, pending) || ++pendingTicks > 100)) {
            requestNotApplied = pendingReward ? !progress.claimed(pendingTask) : progress.trackedTask() != expectedTracked;
            pending = 0;
            lastSequence = -1;
        }
        boolean ready = snapshot.shared().core() == CoreState.ONLINE && snapshot.shared().surfaceMission() != SurfaceMissionState.LOCKED;
        if (ready != unlocked) { unlocked = ready; lastSequence = -1; }
        reading.synchronize(progress, unlocked);
        boolean capacity = rewardCapacity();
        int currentInventory = repairInventoryState();
        if (!activityChanged && snapshot.updateSequence() == lastSequence && capacity == lastCapacity && inventoryState == currentInventory) return;
        inventoryState = currentInventory;
        lastSequence = snapshot.updateSequence(); lastCapacity = capacity;
        refresh();
    }

    private void filter(boolean complete) {
        reading.filter(complete, progress);
        requestNotApplied = false;
        refresh();
    }

    private void show(Page target) {
        if (!unlocked && target != Page.COMMS) return;
        reading.show(target);
        refresh();
    }

    private void applyPage() {
        Page activePage = unlocked ? reading.page() : Page.COMMS;
        pages.forEach((p, view) -> view.setDisplay(p == activePage));
        navigation.forEach((p, b) -> {
            b.setActive(unlocked || p == Page.COMMS);
            setClass(b, "command-selected", p == activePage);
        });
    }

    private void openContext() {
        if (!unlocked) return;
        if (reading.inquiry() != null) reading.returnToTask(progress);
        else {
            reading.taskScroll(0);
            reading.openTask(NovaTerminalViewState.recommended(progress), progress);
        }
        refresh();
    }

    private void askSelected() {
        var session = ClientShipAiTerminalState.current();
        if (!canAskForGuidance() || session.isTransmitting() || session.pendingRequest() != null) return;
        reading.askSelected();
        refresh();
        explain.accept(guidance(reading.selected(), "help"));
    }

    private void refresh() {
        reading.synchronize(progress, unlocked);
        applyPage();
        NovaTask selected = reading.selected();
        boolean hasSelection = progress.completed(selected) == reading.completedFilter();
        UIElement selectedEntry = taskEntries.get(selected);
        if (taskDetails.getParent() != selectedEntry) selectedEntry.addChild(taskDetails);
        taskDetails.setDisplay(hasSelection && reading.expanded());
        taskEmpty.setDisplay(!hasSelection);
        NovaTask context = reading.inquiry() != null ? reading.inquiry() : NovaTerminalViewState.recommended(progress);
        contextTitle.setText(Component.translatable(unlocked ? context.translation("title") : "gui.starboundmc.ship_ai.log"));
        contextOpen.setDisplay(unlocked);
        contextOpen.setText(Component.translatable(reading.inquiry() == null ? "gui.starboundmc.tasks.view" : "gui.starboundmc.tasks.return"));
        mainTasks.setDisplay(hasFilteredTasks(false));
        preparationTasks.setDisplay(hasFilteredTasks(true));
        tasks.forEach((task, b) -> {
            taskEntries.get(task).setDisplay(progress.completed(task) == reading.completedFilter());
            boolean expanded = task == selected && reading.expanded();
            b.setText(Component.literal(expanded ? "▾  " : "▸  ").append(Component.translatable(task.translation("title"))));
            setClass(b, "command-selected", expanded);
            String status = progress.claimable(task) ? "claimable" : progress.completed(task) ? "done"
                    : !task.available(progress.completedMask()) ? "locked" : progress.trackedTask() == task.id() ? "tracked" : "active";
            taskStates.get(task).setText(Component.translatable("gui.starboundmc.tasks.state." + status));
            setClass(taskStates.get(task), "command-complete", progress.completed(task));
            setClass(taskStates.get(task), "command-cost", progress.claimable(task));
            b.style(s -> s.tooltips(Component.translatable(task.translation("title"))));
        });
        int active = 0, complete = 0, rewards = 0;
        for (NovaTask task : NovaTask.values()) {
            if (progress.completed(task)) complete++; else active++;
            if (progress.claimable(task)) rewards++;
        }
        activeFilter.setText(Component.translatable("gui.starboundmc.tasks.active_count", active));
        doneFilter.setText(Component.translatable(rewards > 0 ? "gui.starboundmc.tasks.completed_rewards_count" : "gui.starboundmc.tasks.completed_count", complete, rewards));
        setClass(activeFilter, "command-selected", !reading.completedFilter());
        setClass(doneFilter, "command-selected", reading.completedFilter());
        boolean completed = progress.completed(selected);
        boolean available = selected.available(progress.completedMask());
        taskGuide.setDisplay(!completed);
        taskGuide.setText(guidance(selected, available ? "next" : "help"));
        boolean repair = selected == NovaTask.REPAIR && available && !completed;
        taskSteps.setDisplay(repair);
        taskSteps.setText(Component.translatable("gui.starboundmc.tasks.repair_steps",
                marker((progress.evidenceMask() & NovaTaskProgress.UPGRADED) != 0), marker((progress.evidenceMask() & NovaTaskProgress.CORE_OBTAINED) != 0)));
        taskCost.setDisplay(repair && ClientShipStoryState.hasSnapshot(containerId)
                && ClientShipStoryState.snapshot(containerId).shared().sublightEngine() == EngineState.DAMAGED);
        rewardLine.setDisplay(selected.reward() > 0 && !progress.claimed(selected));
        taskReward.setText(Component.translatable("gui.starboundmc.tasks.module_reward", selected.reward()));
        claimed.setDisplay(completed && (selected.reward() == 0 || progress.claimed(selected)));
        claimed.setText(Component.translatable(selected.reward() == 0 ? "gui.starboundmc.tasks.contact_restored" : "gui.starboundmc.tasks.claimed"));
        boolean canAct = pending == 0 && unlocked;
        claim.setDisplay(progress.claimable(selected));
        claim.setActive(canAct && progress.claimable(selected) && rewardCapacity());
        track.setDisplay(!completed && available);
        track.setActive(canAct && !completed && available);
        track.setText(Component.translatable(progress.trackedTask() == selected.id() ? "gui.starboundmc.tasks.untrack" : "gui.starboundmc.tasks.track"));
        ask.setDisplay(canAskForGuidance());
        ask.setActive(canAskForGuidance() && !transmitting);
        actions.setDisplay(track.isDisplayed() || ask.isDisplayed() || claim.isDisplayed());
        feedback.setDisplay(pending > 0 || requestNotApplied || progress.claimable(selected) && !rewardCapacity());
        feedback.setText(Component.translatable(pending > 0 ? "gui.starboundmc.tasks.waiting" : requestNotApplied
                ? "gui.starboundmc.tasks.request_not_applied" : "gui.starboundmc.tasks.inventory_full"));
        archives.forEach((task, entry) -> entry.setDisplay(task.available(progress.completedMask())));
    }

    private boolean canAskForGuidance() {
        NovaTask task = reading.selected();
        if (!unlocked || progress.completed(task) || !task.available(progress.completedMask())) return false;
        return task != NovaTask.REPAIR || ClientShipStoryState.snapshot(containerId).shared().sublightEngine() == EngineState.DAMAGED;
    }

    private boolean hasFilteredTasks(boolean preparation) {
        for (NovaTask task : NovaTask.values())
            if ((task.id() >= NovaTask.LIFE_SUPPORT.id()) == preparation && progress.completed(task) == reading.completedFilter()) return true;
        return false;
    }

    Component currentGuidance() {
        if (progress.completedMask() == NovaTask.KNOWN_MASK)
            return Component.translatable("gui.starboundmc.tasks.chapter_done");
        NovaTask task = reading.inquiry() != null ? reading.inquiry() : NovaTerminalViewState.recommended(progress);
        return guidance(task, "help");
    }

    private Component guidance(NovaTask task, String kind) {
        if (!ClientShipStoryState.hasSnapshot(containerId)) return Component.translatable("gui.starboundmc.ship_ai.prologue.syncing");
        var shared = ClientShipStoryState.snapshot(containerId).shared();
        String stage = NovaTaskGuidance.stage(task, progress, shared.sublightEngine(), shared.mineralScan(), (inventoryState & 1) != 0, (inventoryState & 2) != 0);
        return Component.translatable(NovaTaskGuidance.translation(stage, kind));
    }

    private static int repairInventoryState() {
        var player = Minecraft.getInstance().player;
        if (player == null) return 0;
        int state = 0;
        for (var stacks : java.util.List.of(player.getInventory().items, player.getInventory().offhand)) {
            for (ItemStack stack : stacks) {
                if (stack.getItem() instanceof MatterManipulatorItem && MatterManipulatorItem.getMiningLevel(stack) >= 1) state |= 1;
                if (stack.is(ModItems.SUBLIGHT_IGNITION_CORE.get())) state |= 2;
            }
        }
        return state;
    }

    private void request(boolean reward) {
        NovaTask selected = reading.selected();
        if (pending > 0 || !unlocked || (reward ? !progress.claimable(selected) || !rewardCapacity()
                : progress.completed(selected) || !selected.available(progress.completedMask()))) return;
        pending = ClientShipAiTerminalState.current().nextActionRequestId(); pendingTicks = 0;
        pendingTask = selected; pendingReward = reward; requestNotApplied = false;
        expectedTracked = progress.trackedTask() == selected.id() ? -1 : selected.id();
        ModNetwork.sendToServer(new ShipAiActionPacket(containerId, pending,
                reward ? ShipAiActionPacket.Action.CLAIM_TASK_REWARD : ShipAiActionPacket.Action.TRACK_TASK,
                reward ? selected.id() : expectedTracked));
        refresh();
    }

    private static boolean rewardCapacity() {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        ItemStack reward = new ItemStack(ModItems.MATTER_MANIPULATOR_MODULE.get());
        for (ItemStack stack : player.getInventory().items) if (stack.isEmpty()
                || ItemStack.isSameItemSameComponents(stack, reward) && stack.getCount() < stack.getMaxStackSize()) return true;
        return false;
    }

    private void bindReadingPosition(ScrollerView scroller, Page page, Supplier<Float> position, Consumer<Float> save) {
        boolean[] restoring = {false};
        scroller.verticalScroller.setOnValueChanged(value -> {
            if (!restoring[0] && reading.page() == page && displayed(scroller) && scrollRange(scroller) > 0)
                save.accept(value * scrollRange(scroller));
        });
        scroller.viewContainer.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            if (reading.page() != page || !displayed(scroller)) return;
            restoring[0] = true;
            float range = scrollRange(scroller);
            scroller.verticalScroller.setNormalizedValue(range <= 0 ? 0 : Math.min(1, position.get() / range));
            restoring[0] = false;
        });
    }

    private void revealSelectedTask() {
        if (!reading.revealSelection() || reading.page() != Page.TASKS || !displayed(taskScroller)
                || taskScroller.viewPort.getContentHeight() <= 0 || tasks.get(reading.selected()).getSizeHeight() <= 0) return;
        // Adaptive text can reflow repeatedly during layout. Reveal on the following tick,
        // using the completed layout rather than consuming the request on its first pass.
        float top = taskEntries.get(reading.selected()).getLayoutY();
        float range = scrollRange(taskScroller);
        float pixels = reading.taskScroll();
        if (top < pixels || top + tasks.get(reading.selected()).getSizeHeight() > pixels + taskScroller.viewPort.getContentHeight())
            reading.taskScroll(Math.max(0, Math.min(range, top)));
        taskScroller.verticalScroller.setNormalizedValue(range <= 0 ? 0 : Math.min(1, reading.taskScroll() / range));
        reading.selectionRevealed();
    }

    private static boolean displayed(UIElement element) {
        for (UIElement parent = element; parent != null; parent = parent.getParent())
            if (!parent.isDisplayed() || !parent.isVisible()) return false;
        return true;
    }

    private static float scrollRange(ScrollerView scroller) { return Math.max(0, scroller.getContainerHeight() - scroller.viewPort.getContentHeight()); }
    private static String marker(boolean complete) { return complete ? "✓" : "○"; }
    private static void divider(UIElement element) {
        element.style(s -> s.overlay((g, mx, my, x, y, w, h, partial) ->
                g.fill(Math.round(x), Math.round(y + h - 1), Math.round(x + w), Math.round(y + h), 0xff29414b)));
    }
    static UIElement column(String css) { UIElement e = new UIElement().addClass(css); e.setOverflowVisible(false); e.layout(l -> l.flexDirection(FlexDirection.COLUMN)); return e; }
    static UIElement row(String css) { UIElement e = column(css); e.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)); return e; }
    static ScrollerView scroll() {
        ScrollerView view = new ScrollerView(); view.addClass("command-scroll"); view.setOverflowVisible(false);
        view.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL).verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER));
        view.viewPort(v -> v.layout(l -> l.paddingAll(0)).style(s -> s.backgroundTexture(IGuiTexture.EMPTY)));
        view.viewContainer(v -> v.layout(l -> l.widthPercent(100).gapAll(8).flexDirection(FlexDirection.COLUMN)));
        return view;
    }
    static Label label(String key, String css) {
        Label label = new Label(); label.addClass(css); label.setAllowHitTest(false); label.setOverflowVisible(false);
        label.setText(key.isEmpty() ? Component.empty() : Component.translatable(key));
        label.layout(l -> l.widthPercent(100).minHeight(9).flexShrink(0));
        label.textStyle(s -> s.adaptiveWidth(false).adaptiveHeight(true).textWrap(TextWrap.WRAP)
                .textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.TOP).lineSpacing(2));
        return label;
    }
    static NovaTerminalButton button(String key, Runnable action) {
        NovaTerminalButton b = new NovaTerminalButton().onActivate(action);
        b.addClass("command-button"); b.setText(Component.translatable(key));
        b.style(s -> s.tooltips(Component.translatable(key)));
        return b;
    }
    private static void setClass(UIElement element, String css, boolean present) { if (present) element.addClass(css); else element.removeClass(css); }
}
