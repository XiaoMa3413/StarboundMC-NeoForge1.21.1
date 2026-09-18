package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.starboundmc.item.ModItems;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.ShipAiActionPacket;
import com.starboundmc.story.*;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/** Retained four-page command terminal. All task actions are server requests; guidance is text only. */
final class NovaCommandLayout extends UIElement {
    private enum Page { OVERVIEW, TASKS, ARCHIVE, COMMS }
    private final int containerId;
    private final Consumer<Component> explain;
    private final Map<Page, UIElement> pages = new EnumMap<>(Page.class);
    private final Map<Page, Button> navigation = new EnumMap<>(Page.class);
    private final Map<NovaTask, Button> tasks = new EnumMap<>(NovaTask.class);
    private final Map<NovaTask, UIElement> taskEntries = new EnumMap<>(NovaTask.class);
    private final Map<NovaTask, UIElement> archives = new EnumMap<>(NovaTask.class);
    private final Label pageTitle = label("gui.starboundmc.command.overview", "command-page-title");
    private final Label overviewTitle = label("", "command-title");
    private final Label overviewGuide = label("", "command-copy");
    private final Label overviewRewards = label("", "command-muted");
    private final Label taskGuide = label("", "command-copy");
    private final Label taskSteps = label("", "command-muted");
    private final Label taskScope = label("", "command-muted");
    private final Label taskStatus = label("", "command-accent");
    private final Label taskReward = label("", "command-copy");
    private final Label feedback = label("", "command-muted");
    private final Label novaContext = label("", "command-copy");
    private final Label taskEmpty = label("gui.starboundmc.command.empty_tasks", "command-muted");
    private final UIElement taskDetails = column("command-task-details");
    private final Map<NovaTask, UIElement> milestones = new EnumMap<>(NovaTask.class);
    private final Button track = button("gui.starboundmc.tasks.track", () -> request(false));
    private final Button claim = button("gui.starboundmc.tasks.claim", () -> request(true));
    private final Button overviewOpen = button("gui.starboundmc.tasks.open", () -> show(Page.TASKS));
    private Page page = Page.COMMS;
    private NovaTask selected = NovaTask.CONTACT;
    private NovaTaskProgress progress = NovaTaskProgress.DEFAULT;
    private boolean unlocked;
    private boolean enteredOverview;
    private boolean completedFilter;
    private boolean taskExpanded = true;
    private long nextRequest = 1_000_000_000L;
    private long pending;
    private int pendingTicks;
    private long lastSequence = -1;
    private boolean lastCapacity;
    private Button ask;
    private final Button activeFilter = button("gui.starboundmc.tasks.active", () -> filter(false));
    private final Button doneFilter = button("gui.starboundmc.tasks.completed", () -> filter(true));

    NovaCommandLayout(int containerId, NovaPortraitElement portrait, UIElement communications, Consumer<Component> explain) {
        this.containerId = containerId;
        this.explain = explain;
        addClass("command-shell");
        setOverflowVisible(false);
        boolean compact = Minecraft.getInstance().getWindow().getGuiScaledWidth() < 440;
        layout(l -> l.widthPercent(96).heightPercent(94).maxWidth(640).maxHeight(340)
                .flexDirection(FlexDirection.COLUMN));
        UIElement header = row("command-header");
        header.layout(l -> l.height(29).flexShrink(0).paddingHorizontal(10).gapAll(10));
        Label brand = label("gui.starboundmc.ship_ai.title", "command-brand");
        brand.layout(l -> l.width(47));
        pageTitle.layout(l -> l.flex(1));
        Button close = button("gui.starboundmc.command.close", () -> {
            if (Minecraft.getInstance().screen != null) Minecraft.getInstance().screen.onClose();
        });
        close.layout(l -> l.width(20).height(16));
        header.addChildren(brand, pageTitle, close);
        UIElement body = row("command-body");
        body.layout(l -> l.flex(1).minHeight(0).alignItems(AlignItems.STRETCH));
        UIElement nav = column("command-nav");
        nav.layout(l -> l.width(compact ? 51 : 74).flexShrink(0).paddingAll(6).gapAll(7));
        for (Page value : Page.values()) {
            Button tab = button("gui.starboundmc.command." + value.name().toLowerCase(java.util.Locale.ROOT), () -> show(value));
            tab.addClass("command-nav-button");
            tab.layout(l -> l.widthPercent(100).height(28).flexShrink(0));
            navigation.put(value, tab); nav.addChild(tab);
        }
        UIElement center = column("command-center");
        center.layout(l -> l.flex(1).minWidth(0).minHeight(0));
        pages.put(Page.OVERVIEW, overview()); pages.put(Page.TASKS, taskPage());
        pages.put(Page.ARCHIVE, archive()); pages.put(Page.COMMS, communications);
        for (UIElement view : pages.values()) { view.layout(l -> l.widthPercent(100).heightPercent(100).minHeight(0)); center.addChild(view); }
        UIElement nova = column("command-nova");
        nova.layout(l -> l.width(compact ? 76 : 110).flexShrink(0).paddingAll(7).gapAll(7));
        Label identity = label("gui.starboundmc.ship_ai.title", "command-accent");
        UIElement portraitStage = column("command-portrait-stage");
        portraitStage.layout(l -> l.widthPercent(100).paddingAll(5).alignItems(AlignItems.CENTER).flexShrink(0));
        portrait.layout(l -> l.widthPercent(100).maxWidth(90).maxHeight(compact ? 80 : 105).aspectRatio(6F / 7F).flexShrink(1));
        portraitStage.addChild(portrait);
        ScrollerView explanation = scroll();
        explanation.layout(l -> l.flex(1).minHeight(20));
        explanation.addScrollViewChild(novaContext);
        ask = button("gui.starboundmc.tasks.ask", () -> {
            explain.accept(Component.translatable(selected.translation("guide"))); show(Page.COMMS);
        });
        ask.layout(l -> l.widthPercent(100).height(19).flexShrink(0));
        nova.addChildren(identity, portraitStage, explanation, ask);
        body.addChildren(nova, nav, center);
        addChildren(header, body);
        applyPage();
    }

    private UIElement overview() {
        ScrollerView view = scroll();
        UIElement hero = column("command-objective");
        hero.layout(l -> l.widthPercent(100).paddingAll(10).gapAll(9).flexShrink(0));
        hero.addChildren(label("gui.starboundmc.command.welcome", "command-eyebrow"), overviewTitle, overviewGuide);
        UIElement route = row("command-route");
        route.layout(l -> l.widthPercent(100).height(3).gapAll(4).flexShrink(0));
        for (NovaTask task : NovaTask.values()) {
            UIElement segment = new UIElement().addClass("command-milestone");
            segment.setAllowHitTest(false);
            segment.layout(l -> l.flex(1).heightPercent(100));
            milestones.put(task, segment); route.addChild(segment);
        }
        hero.addChild(route);
        overviewOpen.addClass("command-primary");
        overviewOpen.layout(l -> l.widthPercent(100).height(22).flexShrink(0));
        hero.addChild(overviewOpen);
        view.addScrollViewChild(hero);
        UIElement rewardPanel = column("command-reward-panel");
        rewardPanel.layout(l -> l.widthPercent(100).paddingAll(9).gapAll(6).flexShrink(0));
        rewardPanel.addChildren(label("gui.starboundmc.tasks.rewards", "command-reward-heading"), overviewRewards);
        Button rewards = button("gui.starboundmc.tasks.review_rewards", () -> { filter(true); show(Page.TASKS); });
        rewards.layout(l -> l.widthPercent(100).height(21)); rewardPanel.addChild(rewards);
        view.addScrollViewChild(rewardPanel);
        return view;
    }

    private UIElement taskPage() {
        ScrollerView view = scroll();
        UIElement filters = row("command-filters"); filters.layout(l -> l.height(19).gapAll(5).flexShrink(0));
        activeFilter.layout(l -> l.flex(1).heightPercent(100)); doneFilter.layout(l -> l.flex(1).heightPercent(100));
        filters.addChildren(activeFilter, doneFilter); view.addScrollViewChild(filters);
        for (NovaTask task : NovaTask.values()) {
            UIElement entry = column("command-task-entry");
            entry.layout(l -> l.widthPercent(100).flexShrink(0));
            Button b = button(task.translation("title"), () -> {
                taskExpanded = selected != task || !taskExpanded;
                selected = task;
                refresh();
            });
            b.addClass("command-task-row");
            b.layout(l -> l.widthPercent(100).height(24).flexShrink(0));
            tasks.put(task, b);
            taskEntries.put(task, entry);
            entry.addChild(b);
            view.addScrollViewChild(entry);
        }
        view.addScrollViewChild(taskEmpty);
        taskDetails.layout(l -> l.widthPercent(100).paddingAll(9).gapAll(8).flexShrink(0));
        taskReward.addClass("command-reward-heading");
        taskDetails.addChildren(taskStatus, taskScope, taskGuide, taskSteps, taskReward);
        UIElement actions = row("command-actions"); actions.layout(l -> l.height(22).gapAll(5).flexShrink(0));
        track.layout(l -> l.widthPercent(48).maxWidth(110).heightPercent(100));
        claim.layout(l -> l.widthPercent(48).maxWidth(125).heightPercent(100));
        claim.addClass("command-primary"); actions.addChildren(track, claim); taskDetails.addChild(actions);
        taskDetails.addChild(feedback);
        taskEntries.get(selected).addChild(taskDetails);
        return view;
    }

    private UIElement archive() {
        ScrollerView view = scroll();
        view.addScrollViewChild(label("gui.starboundmc.command.archive_intro", "command-muted"));
        for (NovaTask task : NovaTask.values()) {
            UIElement entry = column("command-archive-entry");
            entry.layout(l -> l.widthPercent(100).gapAll(8).paddingAll(10).flexShrink(0));
            entry.addChildren(label(task.translation("archive_title"), "command-accent"),
                    label(task.translation("archive"), "command-copy"));
            Button related = button("gui.starboundmc.tasks.related", () -> { selected = task; completedFilter = progress.completed(task); show(Page.TASKS); });
            related.layout(l -> l.height(19).widthPercent(100)); entry.addChild(related);
            archives.put(task, entry); view.addScrollViewChild(entry);
        }
        return view;
    }

    void tick(boolean transmitting) {
        ask.setActive(unlocked && !transmitting);
        if (pending > 0) {
            pendingTicks++;
            if (ClientShipStoryState.consumeAcknowledgement(containerId, pending) || pendingTicks > 100) {
                pending = 0; lastSequence = -1;
            }
        }
        if (!ClientShipStoryState.hasSnapshot(containerId)) return;
        var snapshot = ClientShipStoryState.snapshot(containerId);
        boolean ready = snapshot.shared().schemaSupported() && snapshot.player().schemaSupported()
                && snapshot.shared().core() == CoreState.ONLINE
                && snapshot.shared().surfaceMission() != SurfaceMissionState.LOCKED;
        if (ready != unlocked) { unlocked = ready; if (!ready) page = Page.COMMS; lastSequence = -1; }
        if (unlocked && !enteredOverview && !transmitting) { enteredOverview = true; page = Page.OVERVIEW; lastSequence = -1; }
        boolean capacity = rewardCapacity();
        if (snapshot.updateSequence() == lastSequence && capacity == lastCapacity) return;
        lastSequence = snapshot.updateSequence(); lastCapacity = capacity;
        progress = ClientShipStoryState.tasks(containerId);
        if (page == Page.OVERVIEW) selected = recommended();
        refresh();
    }

    private NovaTask recommended() {
        if (progress.trackedTask() >= 0 && progress.trackedTask() < NovaTask.values().length) return NovaTask.fromId(progress.trackedTask());
        for (NovaTask task : NovaTask.values()) if (!progress.completed(task) && task.available(progress.completedMask())) return task;
        return NovaTask.LUNAR_SORTIE;
    }
    private void filter(boolean complete) {
        completedFilter = complete;
        taskExpanded = true;
        for (NovaTask task : NovaTask.values()) if (progress.completed(task) == complete) {
            selected = task;
            if (progress.claimable(task) || !complete) break;
        }
        refresh();
    }
    private void show(Page target) {
        if (!unlocked && target != Page.COMMS) return;
        page = target;
        if (target == Page.OVERVIEW) selected = recommended();
        if (target == Page.TASKS) {
            completedFilter = progress.completed(selected);
            taskExpanded = true;
        }
        refresh();
    }
    private void applyPage() {
        pages.forEach((p, view) -> view.setDisplay(p == page));
        navigation.forEach((p, b) -> { b.setActive(unlocked || p == Page.COMMS); setClass(b, "command-selected", p == page); });
        pageTitle.setText(Component.translatable("gui.starboundmc.command." + page.name().toLowerCase(java.util.Locale.ROOT)));
    }
    private void refresh() {
        applyPage();
        if (page == Page.TASKS && progress.completed(selected) != completedFilter) {
            for (NovaTask task : NovaTask.values()) if (progress.completed(task) == completedFilter) {
                selected = task; break;
            }
        }
        boolean hasSelection = progress.completed(selected) == completedFilter;
        UIElement selectedEntry = taskEntries.get(selected);
        if (taskDetails.getParent() != selectedEntry) selectedEntry.addChild(taskDetails);
        taskDetails.setDisplay(hasSelection && taskExpanded);
        taskEmpty.setDisplay(!hasSelection);
        NovaTask current = recommended();
        milestones.forEach((task, segment) -> {
            setClass(segment, "command-milestone-done", progress.completed(task));
            setClass(segment, "command-milestone-current", task == current && !progress.completed(task));
        });
        overviewTitle.setText(Component.translatable(current.translation("title")));
        overviewGuide.setText(Component.translatable(progress.completedMask() == NovaTask.KNOWN_MASK
                ? "gui.starboundmc.tasks.chapter_done" : current.translation("guide")));
        int pendingRewards = 0;
        for (NovaTask task : NovaTask.values()) if (progress.claimable(task)) pendingRewards += task.reward();
        overviewRewards.setText(Component.translatable("gui.starboundmc.tasks.pending_rewards", pendingRewards));
        tasks.forEach((task, b) -> {
            taskEntries.get(task).setDisplay(progress.completed(task) == completedFilter);
            b.setActive(task.available(progress.completedMask()));
            Component name = Component.literal((task == selected && taskExpanded ? "−  " : "+  ")
                    + (progress.completed(task) ? "✓  " : "0" + (task.id() + 1) + "  "))
                    .append(Component.translatable(task.translation("title")));
            b.setText(name); setClass(b, "command-selected", task == selected && taskExpanded);
        });
        setClass(activeFilter, "command-selected", !completedFilter); setClass(doneFilter, "command-selected", completedFilter);
        taskScope.setText(Component.translatable(selected.translation("scope")));
        String status = progress.completed(selected) ? "complete" : selected.available(progress.completedMask()) ? "in_progress" : "locked";
        taskStatus.setText(Component.translatable("gui.starboundmc.tasks." + status));
        taskGuide.setText(Component.translatable(selected.translation("guide")));
        taskSteps.setDisplay(selected == NovaTask.REPAIR && !progress.completed(selected));
        taskSteps.setText(Component.translatable("gui.starboundmc.tasks.repair_steps",
                marker((progress.evidenceMask() & NovaTaskProgress.UPGRADED) != 0),
                marker((progress.evidenceMask() & NovaTaskProgress.CORE_OBTAINED) != 0)));
        taskReward.setText(Component.translatable(selected.reward() == 0 ? "gui.starboundmc.tasks.unlock_reward" : "gui.starboundmc.tasks.module_reward", selected.reward()));
        boolean canAct = pending == 0 && progress.writable() && unlocked;
        claim.setDisplay(selected.reward() > 0);
        claim.setActive(canAct && progress.claimable(selected) && rewardCapacity());
        claim.setText(Component.translatable(progress.claimed(selected) ? "gui.starboundmc.tasks.claimed" : "gui.starboundmc.tasks.claim"));
        claim.style(s -> s.tooltips(Component.translatable(progress.claimed(selected) ? "gui.starboundmc.tasks.claimed" : "gui.starboundmc.tasks.claim")));
        track.setActive(canAct && !progress.completed(selected) && selected.available(progress.completedMask()));
        track.setText(Component.translatable(progress.trackedTask() == selected.id() ? "gui.starboundmc.tasks.untrack" : "gui.starboundmc.tasks.track"));
        track.style(s -> s.tooltips(Component.translatable(progress.trackedTask() == selected.id() ? "gui.starboundmc.tasks.untrack" : "gui.starboundmc.tasks.track")));
        feedback.setDisplay(!progress.writable() || pending > 0 || progress.claimable(selected) && !rewardCapacity());
        feedback.setText(Component.translatable(!progress.writable() ? "gui.starboundmc.ship_ai.prologue.incompatible"
                : pending > 0 ? "gui.starboundmc.tasks.waiting" : progress.claimable(selected) && !rewardCapacity()
                ? "gui.starboundmc.tasks.inventory_full" : "gui.starboundmc.tasks.claim_hint"));
        novaContext.setText(Component.translatable(page == Page.COMMS ? "gui.starboundmc.command.comms_hint"
                : (page == Page.OVERVIEW ? current : selected).translation("hint")));
        archives.forEach((task, entry) -> entry.setDisplay(task.available(progress.completedMask())));
    }
    private void request(boolean reward) {
        if (pending > 0 || !progress.writable()) return;
        pending = nextRequest++; pendingTicks = 0;
        ModNetwork.sendToServer(new ShipAiActionPacket(containerId, pending,
                reward ? ShipAiActionPacket.Action.CLAIM_TASK_REWARD : ShipAiActionPacket.Action.TRACK_TASK,
                reward || progress.trackedTask() != selected.id() ? selected.id() : -1));
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
    private static String marker(boolean complete) { return complete ? "✓" : "○"; }
    private static UIElement column(String css) { UIElement e = new UIElement().addClass(css); e.setOverflowVisible(false); e.layout(l -> l.flexDirection(FlexDirection.COLUMN)); return e; }
    private static UIElement row(String css) { UIElement e = new UIElement().addClass(css); e.setOverflowVisible(false); e.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)); return e; }
    private static ScrollerView scroll() {
        ScrollerView view = new ScrollerView(); view.addClass("command-scroll"); view.setOverflowVisible(false);
        view.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL).verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER));
        view.viewPort(v -> {
            v.layout(l -> l.paddingAll(9));
            v.style(s -> s.backgroundTexture(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY));
        });
        view.viewContainer(v -> v.layout(l -> l.widthPercent(100).gapAll(9).flexDirection(FlexDirection.COLUMN)));
        return view;
    }
    private static Label label(String key, String css) {
        Label label = new Label(); label.addClass(css); label.setAllowHitTest(false); label.setOverflowVisible(false);
        label.setText(key.isEmpty() ? Component.empty() : Component.translatable(key));
        label.layout(l -> l.widthPercent(100).minHeight(9).flexShrink(0));
        label.textStyle(s -> s.adaptiveWidth(false).adaptiveHeight(true).textWrap(TextWrap.WRAP)
                .textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.TOP).lineSpacing(2));
        return label;
    }
    private static Button button(String key, Runnable action) {
        Button b = new NovaTerminalButton(); b.addClass("command-button"); b.setText(Component.translatable(key)); b.setOverflowVisible(false);
        b.text.setAllowHitTest(false); b.text.setOverflowVisible(false);
        b.textStyle(s -> s.adaptiveWidth(false).textWrap(TextWrap.HIDE).textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER));
        b.style(s -> s.tooltips(Component.translatable(key)));
        b.addEventListener(UIEvents.CLICK, e -> { if (e.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && b.isActive()) { action.run(); e.stopPropagation(); } });
        return b;
    }
    private static void setClass(UIElement element, String css, boolean present) { if (present) element.addClass(css); else element.removeClass(css); }
}
