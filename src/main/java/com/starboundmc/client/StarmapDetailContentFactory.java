package com.starboundmc.client;

import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarSystem;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Builds semantic detail content from existing navigation data and client state. */
public final class StarmapDetailContentFactory
{
    private static final StarmapDetailContent SELECTION_HINT = new StarmapDetailContent(
            Component.translatable("gui.starboundmc.starmap.select_star"),
            Component.empty(), Component.empty(), List.of());

    private Kind cachedKind;
    private Object cachedTarget;
    private String cachedCurrentEntryId;
    private boolean cachedState;
    private StarmapDetailContent cachedContent;

    public StarmapDetailContent galaxy(StarSystem system)
    {
        if (system == null)
            return SELECTION_HINT;
        String currentEntryId = ClientPlanetState.getCurrentEntryId();
        boolean dockedHere = containsEntry(system, currentEntryId);
        if (matches(Kind.GALAXY, system, currentEntryId, dockedHere))
            return cachedContent;
        return cache(Kind.GALAXY, system, currentEntryId, dockedHere,
                buildGalaxy(system, dockedHere));
    }

    public StarmapDetailContent star(StarSystem system)
    {
        if (system == null)
            return SELECTION_HINT;
        if (matches(Kind.STAR, system, null, false))
            return cachedContent;
        return cache(Kind.STAR, system, null, false, buildStar(system));
    }

    public StarmapDetailContent entry(PlanetEntry entry)
    {
        if (entry == null)
            return SELECTION_HINT;
        String currentEntryId = ClientPlanetState.getCurrentEntryId();
        boolean visited = ClientPlanetState.isVisited(entry.getEntryId());
        if (matches(Kind.ENTRY, entry, currentEntryId, visited))
            return cachedContent;
        int fuelCost = entry.isReachable()
                ? ShipWarpManager.warpFuelCost(currentEntryId, entry.getEntryId()) : 0;
        return cache(Kind.ENTRY, entry, currentEntryId, visited,
                buildEntry(entry, currentEntryId, visited, fuelCost));
    }

    static StarmapDetailContent buildGalaxy(StarSystem system, boolean dockedHere)
    {
        Objects.requireNonNull(system, "system");
        List<StarmapDetailSection> sections = new ArrayList<>();
        sections.add(StarmapDetailSection.labeled("system",
                Component.translatable("gui.starboundmc.starmap.detail.system"),
                StarmapDetailLine.of(
                        Component.translatable("gui.starboundmc.starmap.bodies",
                                system.getEntries().size()),
                        StarmapDetailLine.Tone.ATTENTION)));
        if (dockedHere)
        {
            sections.add(statusSection(StarmapDetailLine.of(
                    Component.translatable("gui.starboundmc.starmap.current"),
                    StarmapDetailLine.Tone.CURRENT)));
        }
        return new StarmapDetailContent(
                Component.translatable(system.getNameKey()),
                Component.translatable(system.getStarTypeKey()),
                Component.translatable(system.getDescriptionKey()), sections);
    }

    static StarmapDetailContent buildStar(StarSystem system)
    {
        Objects.requireNonNull(system, "system");
        return new StarmapDetailContent(
                Component.translatable(system.getNameKey()),
                Component.translatable(system.getStarTypeKey()),
                Component.translatable(system.getDescriptionKey()),
                List.of(StarmapDetailSection.labeled("scan",
                        Component.translatable("gui.starboundmc.starmap.detail.scan"),
                        StarmapDetailLine.of(
                                Component.translatable(
                                        "gui.starboundmc.starmap.detail.threat_unknown"),
                                StarmapDetailLine.Tone.ATTENTION))));
    }

    static StarmapDetailContent buildEntry(PlanetEntry entry, String currentEntryId,
                                           boolean visited, int fuelCost)
    {
        Objects.requireNonNull(entry, "entry");
        List<StarmapDetailSection> sections = new ArrayList<>();
        Component threat = entry.isReachable()
                ? Component.translatable("gui.starboundmc.starmap.detail.threat",
                        entry.getThreatLevel())
                : Component.translatable("gui.starboundmc.starmap.detail.threat_unknown");
        sections.add(StarmapDetailSection.labeled("scan",
                Component.translatable("gui.starboundmc.starmap.detail.scan"),
                StarmapDetailLine.of(threat, StarmapDetailLine.Tone.ATTENTION)));

        if (entry.isReachable())
        {
            boolean crossSystem = fuelCost >= ShipWarpManager.CROSS_SYSTEM_FUEL_COST;
            sections.add(StarmapDetailSection.labeled("navigation",
                    Component.translatable("gui.starboundmc.starmap.detail.navigation"),
                    StarmapDetailLine.of(Component.translatable(
                            crossSystem ? "gui.starboundmc.starmap.detail.fuel_cross"
                                    : "gui.starboundmc.starmap.detail.fuel_local", fuelCost),
                            StarmapDetailLine.Tone.FUEL)));
        }

        StarmapDetailLine status = null;
        if (!entry.isReachable())
        {
            status = StarmapDetailLine.of(
                    Component.translatable("gui.starboundmc.starmap.locked"),
                    StarmapDetailLine.Tone.DANGER);
        }
        else if (entry.getEntryId().equals(currentEntryId))
        {
            status = StarmapDetailLine.of(
                    Component.translatable("gui.starboundmc.starmap.current"),
                    StarmapDetailLine.Tone.CURRENT);
        }
        else if (visited)
        {
            status = StarmapDetailLine.of(
                    Component.translatable("gui.starboundmc.starmap.visited"),
                    StarmapDetailLine.Tone.VISITED);
        }
        if (status != null)
            sections.add(statusSection(status));

        return new StarmapDetailContent(
                Component.translatable(entry.getNameKey()),
                Component.translatable(entry.getTypeKey()),
                Component.translatable(entry.getDescriptionKey()), sections);
    }

    private static StarmapDetailSection statusSection(StarmapDetailLine line)
    {
        return StarmapDetailSection.labeled("status",
                Component.translatable("gui.starboundmc.starmap.detail.status"), line);
    }

    private static boolean containsEntry(StarSystem system, String entryId)
    {
        if (entryId == null)
            return false;
        for (PlanetEntry entry : system.getEntries())
        {
            if (entryId.equals(entry.getEntryId()))
                return true;
        }
        return false;
    }

    private boolean matches(Kind kind, Object target, String currentEntryId, boolean state)
    {
        return cachedKind == kind && cachedTarget == target
                && Objects.equals(cachedCurrentEntryId, currentEntryId)
                && cachedState == state;
    }

    private StarmapDetailContent cache(Kind kind, Object target,
                                       String currentEntryId, boolean state,
                                       StarmapDetailContent content)
    {
        cachedKind = kind;
        cachedTarget = target;
        cachedCurrentEntryId = currentEntryId;
        cachedState = state;
        cachedContent = content;
        return content;
    }

    private enum Kind
    {
        GALAXY,
        STAR,
        ENTRY
    }
}
