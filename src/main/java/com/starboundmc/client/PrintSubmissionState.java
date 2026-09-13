package com.starboundmc.client;

/** Screen-owned request latch; survives resize and never treats a timeout as success. */
public final class PrintSubmissionState {
    public enum Status { IDLE, WAITING, ACCEPTED, REJECTED }
    private Status status = Status.IDLE;
    private long changedAt;
    private java.util.Set<java.util.UUID> previousQueueIds = java.util.Set.of();

    public void rememberQueueIds(java.util.Set<java.util.UUID> ids) {
        previousQueueIds = java.util.Set.copyOf(ids);
    }

    public boolean isNewQueueEntry(java.util.UUID id) { return !previousQueueIds.contains(id); }

    public boolean begin(long now) {
        if (status == Status.WAITING) return false;
        status = Status.WAITING;
        changedAt = now;
        return true;
    }

    public void complete(boolean accepted, long now) {
        if (status != Status.WAITING) return;
        status = accepted ? Status.ACCEPTED : Status.REJECTED;
        changedAt = now;
    }

    public Status status(long now) {
        if (status != Status.WAITING && now - changedAt >= 4_000) status = Status.IDLE;
        return status;
    }

    public boolean waiting() { return status == Status.WAITING; }
    public boolean delayed(long now) { return waiting() && now - changedAt >= 5_000; }
}
