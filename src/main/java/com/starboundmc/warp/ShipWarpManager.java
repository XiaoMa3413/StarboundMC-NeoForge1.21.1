package com.starboundmc.warp;

import com.mojang.logging.LogUtils;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.SyncFlightPacket;
import com.starboundmc.network.SyncFuelPacket;
import com.starboundmc.network.SyncStarStatePacket;
import com.starboundmc.network.WarpStartPacket;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.story.ShipEnvironmentService;
import com.starboundmc.world.Stage6TravelService;
import com.starboundmc.world.universe.BuiltInUniverse;
import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.LegacyUniverseCompatibility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/** Server authority for the fixed physical ship's virtual-space flight. */
public final class ShipWarpManager
{
    /** Compatibility constants; flight duration now belongs to each controller. */
    public static final int WARP_TICKS = ShipFlightController.LONG_ROUTE_MIN_TICKS;
    public static final int TURN_TICKS = ShipFlightController.DEPART_TICKS;
    public static final int MAX_FUEL = ShipFuelService.MAX_FUEL;
    public static final int WARP_FUEL_COST = ShipFuelService.WARP_FUEL_COST;
    public static final int CROSS_SYSTEM_FUEL_COST = ShipFuelService.CROSS_SYSTEM_FUEL_COST;
    private static final int SNAPSHOT_INTERVAL = 5;
    private static final Logger LOGGER = LogUtils.getLogger();

    private static ShipStateData state;
    private static ShipFlightController flight;
    private static String targetEntryId;
    private static long revision;
    /** A saved body id the loaded universe cannot resolve; null when all is well. */
    private static String unknownBodyEntryId;
    private static int broadcastAge;
    private static boolean crewHold;

    private ShipWarpManager() {}

    public static void init(MinecraftServer server)
    {
        revision = Math.max(1L, server.overworld().getGameTime());
        broadcastAge = 0;
        crewHold = false;
        state = ShipStateData.get(server);
        resolveCurrentLocation();
        if (state.isFlightActive())
        {
            String targetId = state.getFlightTargetEntryId();
            // Both ends of a route must be placeable: the controller asks for the
            // departure dock as well as the arrival one, so a stranded departure
            // body cannot resume a flight even when the target is fine.
            if (targetId != null && !isStrandedAtUnknownBody()
                    && UniverseNavigation.isNavigable(currentEntryId())
                    && UniverseNavigation.isNavigable(targetId))
            {
                // The curve is addressed by entry id on both ends, so a resumed
                // flight replays exactly what the original one did.
                flight = new ShipFlightController(state.getCurrentEntryId(), targetId,
                        state.getShipUniversePosition(),
                        state.getFlightElapsedTicks(), state.getFlightPhase(), state.getShipYaw(), state.getShipPitch(), state.getShipRoll());
                targetEntryId = targetId;
            }
            else persistDock();
        }
        else persistDock();
    }

    /**
     * Establishes where the ship is, and refuses to guess.
     *
     * <p>Three cases, in order:</p>
     * <ul>
     *   <li>The saved id names a navigable body: use it. Nothing to do.</li>
     *   <li>The saved id is unknown to the loaded universe (a datapack was
     *       removed): keep the raw id, warn, and leave the ship in the ship
     *       dimension. The plan forbids rewriting it to the starter planet,
     *       because that would silently destroy the player's location; keeping it
     *       means restoring the datapack restores their position.</li>
     *   <li>There is no id at all (a save from before the data layer): fall back
     *       to the starter body, which is the only case where guessing is safe.</li>
     * </ul>
     */
    private static void resolveCurrentLocation()
    {
        String saved = state.getCurrentEntryId();
        if (saved == null)
        {
            state.setCurrentEntryId(BuiltInUniverse.STARTER_BODY_ID);
            return;
        }
        if (UniverseNavigation.isNavigable(saved))
            return;

        // Keep it. Travel away is blocked by startWarp; travel back becomes
        // possible again as soon as a datapack provides this id.
        LOGGER.warn("Ship save is at '{}', which the loaded universe does not provide. "
                + "Keeping the id and leaving the ship in the ship dimension; warp travel "
                + "away is disabled until the body exists again.", saved);
        unknownBodyEntryId = saved;
    }

    public static void reset() {
        state = null; flight = null; targetEntryId = null;
        revision = 0; broadcastAge = 0; unknownBodyEntryId = null;
        crewHold = false;
    }

    /**
     * The saved body id that the universe cannot resolve, or null.
     *
     * <p>Exposed so the failure is visible to callers instead of being inferred
     * from a warp mysteriously not starting.</p>
     */
    public static String unknownBodyEntryId() { return unknownBodyEntryId; }

    /** True when the ship is parked at a body the universe no longer provides. */
    public static boolean isStrandedAtUnknownBody() { return unknownBodyEntryId != null; }

    /**
     * The ship's current body as an entry id, which is the authoritative
     * identity. May name a body the universe cannot resolve; see
     * {@link #isStrandedAtUnknownBody()}.
     */
    public static String currentEntryId() {
        if (state == null) return BuiltInUniverse.STARTER_BODY_ID;
        String entryId = state.getCurrentEntryId();
        return entryId != null ? entryId : BuiltInUniverse.STARTER_BODY_ID;
    }
    /** Star-map entries visited so far, for the state sync. */
    public static java.util.Set<String> visitedEntries() {
        return state == null ? java.util.Set.of() : state.getVisited();
    }

    public static boolean isWarping() { return flight != null; }
    public static int getFuel() { return state == null ? MAX_FUEL : state.getFuel(); }
    public static int getMaxFuel() { return MAX_FUEL; }

    public static int warpFuelCost(String currentEntryId, String targetEntryId)
    {
        // Ownership comes from the catalog, not from a prefix on the id string.
        return UniverseNavigation.sameSystem(currentEntryId, targetEntryId)
                ? WARP_FUEL_COST : CROSS_SYSTEM_FUEL_COST;
    }

    public static boolean startWarp(ServerPlayer player, String entryId)
    {
        MinecraftServer server = player.getServer();
        if (server == null || flight != null || state == null
                || !player.level().dimension().equals(Stage6TravelService.SHIP_LEVEL)) return false;
        // A ship parked at a body the universe no longer has may not travel: its
        // departure geometry is unknown, so a route could not be built.
        if (isStrandedAtUnknownBody())
        {
            player.displayClientMessage(Component.translatable(
                    "message.starboundmc.warp.unknown_location"), true);
            return false;
        }
        CelestialBodyDefinition entry = UniverseNavigation.body(entryId);
        if (entry == null || !entry.isNavigable() || entryId.equals(state.getCurrentEntryId())) return false;
        boolean sameSystem = UniverseNavigation.sameSystem(state.getCurrentEntryId(), entryId);
        if (!ShipEnvironmentService.isCoreOnline(server))
        {
            player.displayClientMessage(Component.translatable(
                    "message.starboundmc.warp.core_offline"), true);
            return false;
        }
        if (sameSystem && !ShipEnvironmentService.canTravelWithinSystem(server))
        {
            player.displayClientMessage(Component.translatable(
                    "message.starboundmc.warp.sublight_offline"), true);
            return false;
        }
        if (!sameSystem && !ShipEnvironmentService.canTravelBetweenSystems(server))
        {
            player.displayClientMessage(Component.translatable(
                    "message.starboundmc.warp.hyperdrive_offline"), true);
            return false;
        }
        ServerLevel ship = server.getLevel(Stage6TravelService.SHIP_LEVEL);
        if (ship == null) return false;
        if (ShipCrewSafety.hasOutsideCrew(ship.players())) {
            player.displayClientMessage(Component.translatable("message.starboundmc.warp.crew_outside"), true);
            return false;
        }
        int cost = warpFuelCost(state.getCurrentEntryId(), entryId);
        if (getFuel() < cost)
        {
            player.displayClientMessage(Component.translatable("message.starboundmc.warp.no_fuel"), true);
            return false;
        }
        state.setFuel(getFuel() - cost);
        flight = new ShipFlightController(currentEntryId(), entryId);
        targetEntryId = entryId;
        revision++;
        broadcastAge = 0;
        persistFlight();
        ship.playSound(null, Stage6TravelService.SHIP_POS, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.starboundmc.warp.start", Component.translatable(entry.nameKey())), true);
        // A compatibility cue only: snapshots own position and progression.
        ModNetwork.sendToPlayersInDimension(ship,
                new WarpStartPacket(entryId, flight.getTotalTicks()));
        broadcastFlight(ship);
        ModNetwork.sendToPlayersInDimension(ship, new SyncFuelPacket(getFuel(), MAX_FUEL));
        return true;
    }

    public static void tick(MinecraftServer server)
    {
        if (flight == null) return;
        ServerLevel ship = server.getLevel(Stage6TravelService.SHIP_LEVEL);
        if (ship == null) return;
        boolean hold = ShipCrewSafety.hasOutsideCrew(ship.players());
        if (hold != crewHold) {
            crewHold = hold;
            revision++;
            for (var player : ship.players()) player.displayClientMessage(Component.translatable(
                    hold ? "message.starboundmc.warp.crew_hold" : "message.starboundmc.warp.crew_resume"), true);
            broadcastFlight(ship);
        }
        if (hold) {
            if (++broadcastAge >= SNAPSHOT_INTERVAL) { broadcastAge = 0; broadcastFlight(ship); }
            return;
        }
        FlightPhase previous = flight.getPhase();
        flight.tick();
        boolean phaseChanged = previous != flight.getPhase();
        boolean shouldBroadcast = ++broadcastAge >= SNAPSHOT_INTERVAL || phaseChanged;
        boolean landed = flight.isLanded();
        if (shouldBroadcast || landed)
        {
            persistFlight();
            if (shouldBroadcast)
            {
                broadcastAge = 0;
                broadcastFlight(ship);
            }
        }
        if (landed) finishWarp(ship);
    }

    public static void syncToPlayer(ServerPlayer player)
    {
        ModNetwork.sendToPlayer(player, new SyncFuelPacket(getFuel(), MAX_FUEL));
        ModNetwork.sendToPlayer(player, new SyncStarStatePacket(
                new ArrayList<>(state == null ? List.of() : state.getVisited()), state == null ? null : state.getCurrentEntryId()));
        ServerLevel ship = player.getServer() == null ? null
                : player.getServer().getLevel(Stage6TravelService.SHIP_LEVEL);
        if (ship != null) ModNetwork.sendToPlayer(player, packet(ship));
    }

    public static int addFuel(int amount, ServerLevel ship)
    {
        if (state == null) return 0;
        int before = getFuel();
        state.setFuel(before + Math.max(0, amount));
        int added = getFuel() - before;
        if (added > 0 && ship != null)
            ModNetwork.sendToPlayersInDimension(ship, new SyncFuelPacket(getFuel(), MAX_FUEL));
        return added;
    }

    private static void finishWarp(ServerLevel ship)
    {
        String entry = targetEntryId;
        // The entry id is the identity; the legacy planet is derived from it at
        // save time, so there is only ever one authority to update.
        state.markVisited(entry); state.setCurrentEntryId(entry);
        flight = null; targetEntryId = null; revision++; persistDock(); broadcastFlight(ship);
        ModNetwork.sendToPlayersInDimension(ship,
                new SyncStarStatePacket(new ArrayList<>(state.getVisited()), state.getCurrentEntryId()));
        CelestialBodyDefinition arrived = UniverseNavigation.body(entry);
        Component name = arrived == null
                ? Component.translatable("message.starboundmc.warp.unknown_destination")
                : Component.translatable(arrived.nameKey());
        for (ServerPlayer p : ship.players()) p.displayClientMessage(Component.translatable("message.starboundmc.warp.arrive", name), true);
    }

    private static void persistFlight()
    {
        state.setFlight(true, targetEntryId, flight.getElapsedTicks(), flight.getTotalTicks(), flight.getPhase(),
                flight.getUniversePosition(), flight.getUniverseVelocity(),
                flight.getYaw(), flight.getPitch(), flight.getRoll());
    }

    private static void persistDock()
    {
        if (state == null) return;
        state.setFlight(false, null, 0, 0, FlightPhase.DOCKED,
                dockPositionFor(currentEntryId(), isStrandedAtUnknownBody(),
                        state.getShipUniversePosition()),
                new UniverseDelta(0.0, 0.0, 0.0),
                dockYawFor(currentEntryId(), isStrandedAtUnknownBody(), state.getShipYaw()),
                0.0, 0.0);
    }

    /**
     * The dock position to persist, tolerating a body the universe cannot place.
     *
     * <p>A saved body id may name something this build cannot resolve: a datapack
     * was removed, or the world came from a branch that had content this one does
     * not. §25 requires the ship to keep that id and stay in the ship dimension, so
     * there is no dock to snap to and the last stored position is kept.</p>
     *
     * <p>Asking {@link UniverseNavigation#universeDock} for a body it cannot resolve
     * throws, and doing so during {@code init} aborted server start with
     * "Unknown navigable body". This method is the single place that decision is
     * made, and it is unit tested without needing a running server.</p>
     */
    static UniversePosition dockPositionFor(String entryId, boolean stranded,
                                            UniversePosition lastKnown)
    {
        if (stranded)
            return lastKnown;
        return UniverseNavigation.universeDock(entryId);
    }

    /** The dock heading to persist; see {@link #dockPositionFor}. */
    static double dockYawFor(String entryId, boolean stranded, double lastKnown)
    {
        if (stranded)
            return lastKnown;
        return UniverseNavigation.yawDock(entryId);
    }

    private static SyncFlightPacket packet(ServerLevel ship)
    {
        if (flight == null)
        {
            // Docked at a body the universe cannot resolve: report the stored pose
            // rather than aborting the packet with an unknown-body exception.
            boolean stranded = isStrandedAtUnknownBody();
            String entryId = currentEntryId();
            return new SyncFlightPacket(revision, ship.getGameTime(), FlightPhase.DOCKED,
                    dockPositionFor(entryId, stranded, state.getShipUniversePosition()),
                    new UniverseDelta(0.0, 0.0, 0.0),
                    dockYawFor(entryId, stranded, state.getShipYaw()), 0, 0, 0, 0, null);
        }
        return new SyncFlightPacket(revision, ship.getGameTime(), flight.getPhase(),
                flight.getUniversePosition(), crewHold ? new UniverseDelta(0, 0, 0) : flight.getUniverseVelocity(),
                flight.getYaw(), flight.getPitch(), flight.getRoll(), flight.getElapsedTicks(), flight.getTotalTicks(), targetEntryId, crewHold);
    }

    private static void broadcastFlight(ServerLevel ship)
    {
        ModNetwork.sendToPlayersInDimension(ship, packet(ship));
    }

}
