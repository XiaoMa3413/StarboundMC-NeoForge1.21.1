package com.starboundmc.client.shipai;

import com.starboundmc.story.NovaTask;
import com.starboundmc.story.NovaTaskProgress;

/** Connection-scoped reading/navigation state. No gameplay state is stored here. */
final class NovaTerminalViewState {
    enum Page { COMMS, TASKS, ARCHIVE }

    private Page page = Page.COMMS;
    private NovaTask selected = NovaTask.CONTACT;
    private NovaTask inquiry;
    private boolean initialized;
    private boolean completedFilter;
    private boolean expanded = true;
    private boolean historyOpen;
    private boolean revealSelection;
    private float taskScroll;
    private float archiveScroll;

    Page page() { return page; }
    NovaTask selected() { return selected; }
    NovaTask inquiry() { return inquiry; }
    boolean completedFilter() { return completedFilter; }
    boolean expanded() { return expanded; }
    boolean historyOpen() { return historyOpen; }
    boolean revealSelection() { return revealSelection; }
    void selectionRevealed() { revealSelection = false; }
    float taskScroll() { return taskScroll; }
    float archiveScroll() { return archiveScroll; }
    void taskScroll(float pixels) { taskScroll = Math.max(0, pixels); }
    void archiveScroll(float pixels) { archiveScroll = Math.max(0, pixels); }
    void historyOpen(boolean open) { historyOpen = open; }

    void synchronize(NovaTaskProgress progress, boolean unlocked) {
        if (!initialized) {
            selected = recommended(progress);
            initialized = true;
        }
        if (!unlocked) page = Page.COMMS;
        if (page == Page.TASKS && progress.completed(selected) != completedFilter) chooseFiltered(progress);
    }

    void show(Page target) { page = target; }

    void select(NovaTask task) {
        revealSelection = false;
        expanded = selected != task || !expanded;
        selected = task;
    }

    void filter(boolean completed, NovaTaskProgress progress) {
        revealSelection = false;
        completedFilter = completed;
        expanded = true;
        taskScroll = 0;
        chooseFiltered(progress);
    }

    void openTask(NovaTask task, NovaTaskProgress progress) {
        initialized = true;
        selected = task;
        completedFilter = progress.completed(task);
        expanded = true;
        page = Page.TASKS;
        revealSelection = true;
    }

    void askSelected() {
        inquiry = selected;
        historyOpen = false;
        page = Page.COMMS;
    }

    void askNext() {
        inquiry = null;
        historyOpen = false;
        page = Page.COMMS;
    }

    void returnToTask(NovaTaskProgress progress) {
        if (inquiry != null) {
            openTask(inquiry, progress);
            revealSelection = false;
        }
    }

    private void chooseFiltered(NovaTaskProgress progress) {
        for (NovaTask task : NovaTask.values()) {
            if (progress.completed(task) != completedFilter) continue;
            selected = task;
            if (!completedFilter || progress.claimable(task)) return;
        }
    }

    static NovaTask recommended(NovaTaskProgress progress) {
        if (progress.trackedTask() >= 0) {
            NovaTask tracked = NovaTask.fromId(progress.trackedTask());
            if (!progress.completed(tracked) && tracked.available(progress.completedMask())) return tracked;
        }
        for (NovaTask task : NovaTask.values()) {
            if (!progress.completed(task) && task.available(progress.completedMask())) return task;
        }
        return NovaTask.LUNAR_SORTIE;
    }
}
