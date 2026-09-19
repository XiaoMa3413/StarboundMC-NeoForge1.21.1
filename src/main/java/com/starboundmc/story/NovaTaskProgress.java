package com.starboundmc.story;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Player-owned achievement/claim ledger. Existing ship story data stays authoritative. */
public record NovaTaskProgress(int schemaVersion, long revision, int completedMask, int claimedMask,
                               int evidenceMask, int trackedTask, String firstSurface) {
    public static final int SCHEMA = 2;
    public static final int UPGRADED = 1, CORE_OBTAINED = 2, NEW_SURFACE = 4, EPP_READY = 8, MOON_VISITED = 16;
    public static final NovaTaskProgress DEFAULT = new NovaTaskProgress(SCHEMA, 0, 0, 0, 0, 0, "");
    public static final Codec<NovaTaskProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("schema", SCHEMA).forGetter(NovaTaskProgress::schemaVersion),
            Codec.LONG.optionalFieldOf("revision", 0L).forGetter(NovaTaskProgress::revision),
            Codec.INT.optionalFieldOf("completed", 0).forGetter(NovaTaskProgress::completedMask),
            Codec.INT.optionalFieldOf("claimed", 0).forGetter(NovaTaskProgress::claimedMask),
            Codec.INT.optionalFieldOf("evidence", 0).forGetter(NovaTaskProgress::evidenceMask),
            Codec.INT.optionalFieldOf("tracked", 0).forGetter(NovaTaskProgress::trackedTask),
            Codec.STRING.optionalFieldOf("first_surface", "").forGetter(NovaTaskProgress::firstSurface)
    ).apply(i, NovaTaskProgress::new));

    public NovaTaskProgress {
        schemaVersion = Math.max(1, schemaVersion);
        revision = Math.max(0, revision);
        // Preserve newer schemas losslessly; they are read-only in this build.
        if (schemaVersion <= SCHEMA) {
            completedMask = Math.max(0, completedMask) & NovaTask.KNOWN_MASK;
            claimedMask = Math.max(0, claimedMask) & completedMask;
            evidenceMask = Math.max(0, evidenceMask) & 31;
            if (trackedTask < -1 || trackedTask >= NovaTask.values().length) trackedTask = -1;
        }
        firstSurface = firstSurface == null ? "" : firstSurface;
    }
    public boolean writable() { return schemaVersion <= SCHEMA; }
    public boolean completed(NovaTask task) { return (completedMask & task.mask()) != 0; }
    public boolean claimed(NovaTask task) { return (claimedMask & task.mask()) != 0; }
    public boolean claimable(NovaTask task) { return writable() && task.reward() > 0 && completed(task) && !claimed(task); }
    public NovaTaskProgress claim(NovaTask task) {
        return claimable(task) ? change(completedMask, claimedMask | task.mask(), evidenceMask, trackedTask, firstSurface) : this;
    }
    public NovaTaskProgress track(int id) {
        if (!writable() || id == trackedTask) return this;
        if (id != -1 && !NovaTask.fromId(id).available(completedMask)) return this;
        return change(completedMask, claimedMask, evidenceMask, id, firstSurface);
    }
    public NovaTaskProgress arrive(String surface, boolean engineOnline) {
        if (!writable() || surface == null || surface.isBlank()) return this;
        if (firstSurface.isEmpty()) return change(completedMask, claimedMask, evidenceMask, trackedTask, surface);
        int evidence = evidenceMask;
        if (engineOnline && !firstSurface.equals(surface)) evidence |= NEW_SURFACE;
        return evidence == evidenceMask ? this : change(completedMask, claimedMask, evidence, trackedTask, firstSurface);
    }
    public NovaTaskProgress observe(boolean contacted, boolean visited, boolean upgraded, boolean core, boolean repaired) {
        if (!writable()) return this;
        int evidence = evidenceMask | (upgraded ? UPGRADED : 0) | (core ? CORE_OBTAINED : 0);
        int completed = completedMask;
        if (contacted) completed |= NovaTask.CONTACT.mask();
        if ((completed & NovaTask.CONTACT.mask()) != 0 && visited) completed |= NovaTask.SURFACE.mask();
        if ((completed & NovaTask.SURFACE.mask()) != 0 && repaired) completed |= NovaTask.REPAIR.mask();
        if ((completed & NovaTask.REPAIR.mask()) != 0 && (evidence & NEW_SURFACE) != 0)
            completed |= NovaTask.EXPLORATION.mask();
        if (completed == completedMask && evidence == evidenceMask) return this;
        int tracked = trackedTask;
        if (tracked >= 0 && (completed & (1 << tracked)) != 0) {
            tracked = -1;
            for (NovaTask task : NovaTask.values()) if ((completed & task.mask()) == 0 && task.available(completed)) {
                tracked = task.id(); break;
            }
        }
        return change(completed, claimedMask, evidence, tracked, firstSurface);
    }
    public NovaTaskProgress observeEpp(boolean ready, boolean onMoon, boolean safelyReturned) {
        if (!writable()) return this;
        int evidence = evidenceMask | (ready ? EPP_READY : 0);
        int completed = completedMask;
        if (NovaTask.LIFE_SUPPORT.available(completed) && (evidence & EPP_READY) != 0) completed |= NovaTask.LIFE_SUPPORT.mask();
        if (ready && onMoon && NovaTask.LUNAR_SORTIE.available(completed)) evidence |= MOON_VISITED;
        if (safelyReturned && (evidence & MOON_VISITED) != 0 && NovaTask.LUNAR_SORTIE.available(completed)) completed |= NovaTask.LUNAR_SORTIE.mask();
        if (evidence == evidenceMask && completed == completedMask) return this;
        int tracked = trackedTask;
        if (tracked >= 0 && (completed & (1 << tracked)) != 0) {
            tracked = -1;
            for (NovaTask task : NovaTask.values()) if ((completed & task.mask()) == 0 && task.available(completed)) { tracked = task.id(); break; }
        }
        return change(completed, claimedMask, evidence, tracked, firstSurface);
    }
    private NovaTaskProgress change(int completed, int claimed, int evidence, int tracked, String surface) {
        return new NovaTaskProgress(SCHEMA, revision == Long.MAX_VALUE ? revision : revision + 1,
                completed, claimed, evidence, tracked, surface);
    }
}
