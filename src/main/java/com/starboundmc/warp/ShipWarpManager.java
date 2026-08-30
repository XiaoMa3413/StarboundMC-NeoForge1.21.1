package com.starboundmc.warp;

import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.SyncFlightPacket;
import com.starboundmc.network.SyncFuelPacket;
import com.starboundmc.network.SyncPlanetPacket;
import com.starboundmc.network.SyncStarStatePacket;
import com.starboundmc.network.WarpStartPacket;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.story.ShipEnvironmentService;
import com.starboundmc.world.Planet;
import com.starboundmc.world.Stage6TravelService;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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

    private static ShipStateData state;
    private static ShipFlightController flight;
    private static String targetEntryId;
    private static long revision;
    private static int broadcastAge;

    private ShipWarpManager() {}

    public static void init(MinecraftServer server)
    {
        revision = Math.max(1L, server.overworld().getGameTime());
        broadcastAge = 0;
        state = ShipStateData.get(server);
        if (state.getCurrentEntryId() == null) state.setCurrentEntryId(defaultEntryIdFor(state.getPlanet()));
        if (state.isFlightActive())
        {
            PlanetEntry target = StarSystems.entryById(state.getFlightTargetEntryId());
            if (target != null && target.isReachable())
            {
                flight = new ShipFlightController(state.getPlanet(), target.getDestination(),
                        state.getShipUniversePosition(),
                        state.getFlightElapsedTicks(), state.getFlightPhase(), state.getShipYaw(), state.getShipPitch(), state.getShipRoll());
                targetEntryId = target.getEntryId();
            }
            else persistDock();
        }
        else persistDock();
    }

    public static void reset() { state = null; flight = null; targetEntryId = null; revision = 0; broadcastAge = 0; }
    public static Planet getCurrentPlanet() { return state == null ? Planet.LUSH : state.getPlanet(); }
    public static boolean isWarping() { return flight != null; }
    public static int getFuel() { return state == null ? MAX_FUEL : state.getFuel(); }
    public static int getMaxFuel() { return MAX_FUEL; }

    public static int warpFuelCost(String currentEntryId, String targetEntryId)
    {
        String fromSystem = StarSystems.systemIdOfEntry(currentEntryId);
        String toSystem = StarSystems.systemIdOfEntry(targetEntryId);
        return fromSystem != null && fromSystem.equals(toSystem) ? WARP_FUEL_COST : CROSS_SYSTEM_FUEL_COST;
    }

    public static boolean startWarp(ServerPlayer player, String entryId)
    {
        MinecraftServer server = player.getServer();
        if (server == null || flight != null || state == null
                || !player.level().dimension().equals(Stage6TravelService.SHIP_LEVEL)) return false;
        PlanetEntry entry = StarSystems.entryById(entryId);
        if (entry == null || !entry.isReachable() || entry.getDestination() == getCurrentPlanet()) return false;
        String currentSystem = StarSystems.systemIdOfEntry(state.getCurrentEntryId());
        String targetSystem = StarSystems.systemIdOfEntry(entryId);
        boolean sameSystem = currentSystem != null && currentSystem.equals(targetSystem);
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
        int cost = warpFuelCost(state.getCurrentEntryId(), entryId);
        if (getFuel() < cost)
        {
            player.displayClientMessage(Component.translatable("message.starboundmc.warp.no_fuel"), true);
            return false;
        }
        state.setFuel(getFuel() - cost);
        flight = new ShipFlightController(getCurrentPlanet(), entry.getDestination());
        targetEntryId = entryId;
        revision++;
        broadcastAge = 0;
        persistFlight();
        ship.playSound(null, Stage6TravelService.SHIP_POS, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.starboundmc.warp.start", Component.translatable(entry.getNameKey())), true);
        // A compatibility cue only: snapshots own position and progression.
        ModNetwork.sendToPlayersInDimension(ship,
                new WarpStartPacket(entry.getDestination(), flight.getTotalTicks(), entryId));
        broadcastFlight(ship);
        ModNetwork.sendToPlayersInDimension(ship, new SyncFuelPacket(getFuel(), MAX_FUEL));
        return true;
    }

    public static void tick(MinecraftServer server)
    {
        if (flight == null) return;
        ServerLevel ship = server.getLevel(Stage6TravelService.SHIP_LEVEL);
        if (ship == null) return;
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
        ModNetwork.sendToPlayer(player, new SyncPlanetPacket(getCurrentPlanet()));
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
        Planet target = flight.getTarget(); String entry = targetEntryId;
        state.setPlanet(target); state.markVisited(entry); state.setCurrentEntryId(entry);
        flight = null; targetEntryId = null; revision++; persistDock(); broadcastFlight(ship);
        ModNetwork.sendToPlayersInDimension(ship, new SyncPlanetPacket(target));
        ModNetwork.sendToPlayersInDimension(ship,
                new SyncStarStatePacket(new ArrayList<>(state.getVisited()), state.getCurrentEntryId()));
        PlanetEntry arrived = StarSystems.entryById(entry);
        Component name = arrived == null ? Component.translatable(target.translationKey()) : Component.translatable(arrived.getNameKey());
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
        Planet planet = state.getPlanet();
        state.setFlight(false, null, 0, 0, FlightPhase.DOCKED,
                ShipSpace.universeDock(planet), new UniverseDelta(0.0, 0.0, 0.0),
                ShipSpace.yawDock(planet), 0.0, 0.0);
    }

    private static SyncFlightPacket packet(ServerLevel ship)
    {
        if (flight == null)
        {
            return new SyncFlightPacket(revision, ship.getGameTime(), FlightPhase.DOCKED,
                    ShipSpace.universeDock(getCurrentPlanet()), new UniverseDelta(0.0, 0.0, 0.0),
                    ShipSpace.yawDock(getCurrentPlanet()), 0, 0, 0, 0, null);
        }
        return new SyncFlightPacket(revision, ship.getGameTime(), flight.getPhase(),
                flight.getUniversePosition(), flight.getUniverseVelocity(),
                flight.getYaw(), flight.getPitch(), flight.getRoll(), flight.getElapsedTicks(), flight.getTotalTicks(), targetEntryId);
    }

    private static void broadcastFlight(ServerLevel ship)
    {
        ModNetwork.sendToPlayersInDimension(ship, packet(ship));
    }

    private static String defaultEntryIdFor(Planet planet)
    {
        for (StarSystem system : StarSystems.all()) for (PlanetEntry entry : system.getEntries())
            if (entry.getDestination() == planet) return entry.getEntryId();
        return StarSystems.SYS_MAIN + ":lush";
    }
}
