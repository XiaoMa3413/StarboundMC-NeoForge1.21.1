// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.encounter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** One persistent MVP encounter; the full structure snapshot retains inventories and player edits. */
public final class RelayData extends SavedData {
    public enum Phase { UNDISCOVERED, AVAILABLE, ROUTING, APPROACHING, MATERIALIZING, ACTIVE, LEAVING, ERROR }
    public Phase phase = Phase.UNDISCOVERED;
    public String homeBody = "";
    public BlockPos origin = BlockPos.ZERO;
    public int approachTicks;
    public int transaction; // 0 none, 1 materializing, 2 leaving; retained on conflict for operator retry
    public boolean recovered, completed;
    public CompoundTag snapshot = new CompoundTag();
    public final Set<UUID> outsideCrew = new HashSet<>();
    public static RelayData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(RelayData::new, RelayData::load), "starboundmc_relay");
    }
    public static RelayData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new RelayData();
        try { data.phase = Phase.valueOf(tag.getString("Phase")); } catch (IllegalArgumentException ignored) { data.phase = Phase.UNDISCOVERED; }
        data.homeBody = tag.getString("HomeBody"); data.origin = BlockPos.of(tag.getLong("Origin"));
        data.approachTicks = Math.clamp(tag.getInt("ApproachTicks"), 0, RelayEncounter.APPROACH_TICKS);
        data.transaction = Math.clamp(tag.getInt("Transaction"), 0, 2);
        data.recovered = tag.getBoolean("Recovered"); data.completed = tag.getBoolean("Completed");
        data.snapshot = tag.getCompound("Snapshot").copy();
        var crew = tag.getList("OutsideCrew", Tag.TAG_STRING);
        for (int i = 0; i < crew.size(); i++) try { data.outsideCrew.add(UUID.fromString(crew.getString(i))); } catch (IllegalArgumentException ignored) { }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Phase", phase.name()); tag.putString("HomeBody", homeBody); tag.putLong("Origin", origin.asLong());
        tag.putInt("ApproachTicks", approachTicks); tag.putBoolean("Recovered", recovered); tag.putBoolean("Completed", completed);
        tag.putInt("Transaction", transaction);
        tag.put("Snapshot", snapshot.copy());
        var crew = new ListTag(); outsideCrew.forEach(id -> crew.add(StringTag.valueOf(id.toString()))); tag.put("OutsideCrew", crew);
        return tag;
    }
}
