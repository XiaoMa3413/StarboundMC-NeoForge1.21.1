// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.encounter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Persistent encounter authority. Journal transitions save before world mutation or acknowledgement. */
public final class RelayData extends SavedData {
    public enum Phase {
        UNDISCOVERED(0), AVAILABLE(1), ROUTING(2), APPROACHING(3), MATERIALIZING(4), ACTIVE(5), LEAVING(6), ERROR(7);
        private final int networkId;
        Phase(int networkId) { this.networkId = networkId; }
        public int networkId() { return networkId; }
        public static Phase byNetworkId(int id) {
            return switch (id) {
                case 0 -> UNDISCOVERED; case 1 -> AVAILABLE; case 2 -> ROUTING; case 3 -> APPROACHING;
                case 4 -> MATERIALIZING; case 5 -> ACTIVE; case 6 -> LEAVING; case 7 -> ERROR;
                default -> throw new IllegalArgumentException("Unknown relay phase id " + id);
            };
        }
    }
    private Phase phase = Phase.UNDISCOVERED;
    private String homeBody = "";
    private BlockPos origin = BlockPos.ZERO;
    private int approachTicks;
    private int transaction; // 0 none, 1 materializing, 2 leaving; ERROR retains the journal for retry
    private boolean recovered, completed;
    private CompoundTag snapshot = new CompoundTag();
    private final Set<UUID> outsideCrew = new HashSet<>();

    public Phase phase() { return phase; }
    public String homeBody() { return homeBody; }
    public BlockPos origin() { return origin; }
    public int approachTicks() { return approachTicks; }
    public int transaction() { return transaction; }
    public boolean recovered() { return recovered; }
    public boolean completed() { return completed; }
    public CompoundTag snapshot() { return snapshot.copy(); }
    public Set<UUID> outsideCrew() { return Set.copyOf(outsideCrew); }

    void discover(String body) {
        requirePhase(Phase.UNDISCOVERED);
        if (body == null || body.isBlank() || body.length() > 128) throw new IllegalArgumentException("Invalid relay home body");
        homeBody = body; phase = Phase.AVAILABLE; setDirty();
    }
    void beginRouting() { requirePhase(Phase.AVAILABLE); phase = Phase.ROUTING; setDirty(); }
    void beginApproach(BlockPos site, CompoundTag structure) {
        if (phase != Phase.AVAILABLE && phase != Phase.ROUTING) throw new IllegalStateException("Relay cannot approach from " + phase);
        if (structure.isEmpty()) throw new IllegalArgumentException("Relay approach requires a structure snapshot");
        origin = site.immutable(); snapshot = structure.copy(); approachTicks = 0; phase = Phase.APPROACHING; setDirty();
    }
    void cancelApproach() {
        if (phase != Phase.AVAILABLE && phase != Phase.ROUTING && phase != Phase.APPROACHING)
            throw new IllegalStateException("Relay cannot cancel from " + phase);
        phase = Phase.AVAILABLE; approachTicks = 0; setDirty();
    }
    boolean advanceApproach() {
        requirePhase(Phase.APPROACHING);
        approachTicks = Math.min(RelayEncounter.APPROACH_TICKS, approachTicks + 1); setDirty();
        return approachTicks == RelayEncounter.APPROACH_TICKS;
    }
    void beginMaterialization(MinecraftServer server) {
        requirePhase(Phase.APPROACHING);
        if (approachTicks != RelayEncounter.APPROACH_TICKS) throw new IllegalStateException("Relay approach is incomplete");
        phase = Phase.MATERIALIZING; transaction = 1; persistJournal(server);
    }
    void finishMaterialization(MinecraftServer server) {
        requirePhase(Phase.MATERIALIZING);
        phase = Phase.ACTIVE; transaction = 0; persistJournal(server);
    }
    void beginDeparture(MinecraftServer server, CompoundTag captured) {
        requirePhase(Phase.ACTIVE);
        if (captured.isEmpty()) throw new IllegalArgumentException("Relay departure requires a snapshot");
        snapshot = captured.copy(); phase = Phase.LEAVING; transaction = 2; persistJournal(server);
    }
    void finishDeparture(MinecraftServer server) {
        requirePhase(Phase.LEAVING);
        phase = Phase.AVAILABLE; transaction = 0; approachTicks = 0; persistJournal(server);
    }
    void finishRecovery(MinecraftServer server) {
        if (transaction == 0) throw new IllegalStateException("Relay has no transaction to recover");
        phase = transaction == 2 ? Phase.AVAILABLE : Phase.ACTIVE;
        transaction = 0; approachTicks = 0; persistJournal(server);
    }
    void markRecoveryError(MinecraftServer server) {
        if (transaction == 0) throw new IllegalStateException("Relay error requires a transaction journal");
        phase = Phase.ERROR; persistJournal(server);
    }
    void markRecovered() { requirePhase(Phase.ACTIVE); recovered = true; setDirty(); }
    void markCompleted() {
        requirePhase(Phase.ACTIVE);
        if (!recovered || !outsideCrew.isEmpty()) throw new IllegalStateException("Relay crew has not returned with the core");
        completed = true; setDirty();
    }
    boolean setCrewOutside(UUID id, boolean outside) {
        Objects.requireNonNull(id, "id");
        boolean changed = outside ? outsideCrew.add(id) : outsideCrew.remove(id);
        if (changed) setDirty();
        return changed;
    }
    private void requirePhase(Phase expected) {
        if (phase != expected) throw new IllegalStateException("Expected relay " + expected + ", was " + phase);
    }
    /** Logout safety records must be durable before acknowledging a disconnected crew member. */
    void recordDisconnectedCrew(MinecraftServer server, UUID player, boolean outside) {
        if (setCrewOutside(player, outside) || outside) persistJournal(server);
    }

    private void persistJournal(MinecraftServer server) {
        setDirty();
        // Relay-specific crash recovery: placement/removal must not outrun its journal.
        // These transitions and SavedData snapshots run on the server thread. Normal
        // SavedData save queues IO and logs errors; drain older writes before using
        // NeoForge's synchronous atomic writer so stale snapshots cannot overwrite this one.
        net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();
        var envelope = new CompoundTag();
        envelope.put("data", save(new CompoundTag(), server.registryAccess()));
        NbtUtils.addCurrentDataVersion(envelope);
        var path = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("data/starboundmc_relay.dat");
        try {
            java.nio.file.Files.createDirectories(path.getParent());
            net.neoforged.neoforge.common.IOUtilities.writeNbtCompressed(envelope, path);
        } catch (java.io.IOException failure) {
            throw new java.io.UncheckedIOException("Cannot persist relay transaction journal", failure);
        }
        setDirty(false); // This exact server-thread snapshot is already on disk.
    }
    public static RelayData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(RelayData::new, RelayData::load), "starboundmc_relay");
    }
    public static RelayData load(CompoundTag tag, HolderLookup.Provider registries) {
        for (String key : new String[]{"Phase", "HomeBody"}) requireTag(tag, key, Tag.TAG_STRING);
        for (String key : new String[]{"ApproachTicks", "Transaction"}) requireTag(tag, key, Tag.TAG_INT);
        for (String key : new String[]{"Recovered", "Completed"}) requireTag(tag, key, Tag.TAG_BYTE);
        requireTag(tag, "Origin", Tag.TAG_LONG); requireTag(tag, "Snapshot", Tag.TAG_COMPOUND); requireTag(tag, "OutsideCrew", Tag.TAG_LIST);
        var data = new RelayData();
        data.phase = Phase.valueOf(tag.getString("Phase"));
        data.homeBody = tag.getString("HomeBody"); data.origin = BlockPos.of(tag.getLong("Origin"));
        data.approachTicks = tag.getInt("ApproachTicks"); data.transaction = tag.getInt("Transaction");
        data.recovered = tag.getBoolean("Recovered"); data.completed = tag.getBoolean("Completed");
        data.snapshot = tag.getCompound("Snapshot").copy();
        var crew = (ListTag) tag.get("OutsideCrew");
        if (!crew.isEmpty() && crew.getElementType() != Tag.TAG_STRING) throw new IllegalArgumentException("Malformed relay crew list");
        for (int i = 0; i < crew.size(); i++) data.outsideCrew.add(UUID.fromString(crew.getString(i)));
        int expected = switch (data.phase) { case MATERIALIZING -> 1; case LEAVING -> 2; case ERROR -> data.transaction; default -> 0; };
        if (data.transaction != expected || data.transaction < 0 || data.transaction > 2
                || data.phase == Phase.ERROR && data.transaction == 0
                || data.approachTicks < 0 || data.approachTicks > RelayEncounter.APPROACH_TICKS
                || data.completed && !data.recovered
                || data.phase == Phase.UNDISCOVERED && (!data.homeBody.isEmpty() || !data.snapshot.isEmpty()
                    || !data.origin.equals(BlockPos.ZERO) || data.approachTicks != 0
                    || data.recovered || data.completed || !data.outsideCrew.isEmpty())
                || data.homeBody.length() > 128 || data.phase != Phase.UNDISCOVERED && data.homeBody.isBlank()
                || (data.phase == Phase.APPROACHING || data.phase == Phase.MATERIALIZING || data.phase == Phase.ACTIVE
                    || data.phase == Phase.LEAVING || data.phase == Phase.ERROR) && data.snapshot.isEmpty())
            throw new IllegalArgumentException("Inconsistent relay persistent state");
        return data;
    }
    private static void requireTag(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing or malformed relay field: " + key);
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Phase", phase.name()); tag.putString("HomeBody", homeBody); tag.putLong("Origin", origin.asLong());
        tag.putInt("ApproachTicks", approachTicks); tag.putBoolean("Recovered", recovered); tag.putBoolean("Completed", completed);
        tag.putInt("Transaction", transaction); tag.put("Snapshot", snapshot.copy());
        var crew = new ListTag(); outsideCrew.forEach(id -> crew.add(StringTag.valueOf(id.toString()))); tag.put("OutsideCrew", crew);
        return tag;
    }
}
