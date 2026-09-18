package com.starboundmc.story;

/** Stable task IDs are persisted and sent over the wire; never reorder/reuse IDs. */
public enum NovaTask {
    CONTACT(0, "contact", 0), SURFACE(1, "surface", 1),
    REPAIR(2, "repair", 1), EXPLORATION(3, "exploration", 1);

    public static final int KNOWN_MASK = 15;
    private final int id;
    private final String key;
    private final int reward;
    NovaTask(int id, String key, int reward) { this.id = id; this.key = key; this.reward = reward; }
    public int id() { return id; }
    public int mask() { return 1 << id; }
    public int reward() { return reward; }
    public String key() { return key; }
    public String translation(String suffix) { return "gui.starboundmc.task." + key + "." + suffix; }
    public boolean available(int completed) { return id == 0 || (completed & (1 << (id - 1))) != 0; }
    public static NovaTask fromId(int id) {
        for (NovaTask task : values()) if (task.id == id) return task;
        throw new IllegalArgumentException("Unknown NOVA task " + id);
    }
}
