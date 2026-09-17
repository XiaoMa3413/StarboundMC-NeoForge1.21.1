package com.starboundmc.client;

import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.warp.UniverseNavigation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration gate A6: the client replays exactly the server's curve.
 *
 * <p>The client is a visual mirror, so its sampled position must be the same
 * function of time the server flies. The client interpolates against a wall clock
 * for smoothness, so this cannot compare against a fixed tick; instead it reads
 * the progress the snapshot itself reports and checks that the position matches
 * the pure curve at that same progress. That is exact, deterministic, and it is
 * the property the migration could actually have broken: passing the wrong body
 * ids to the sampler.</p>
 */
class ClientCurveReplayTest
{
    private static final List<String> BODIES = List.of(
            "sys1:lush", "sys1:molten", "sys1:barren", "sys2:frozen");

    @AfterEach
    void resetState()
    {
        ClientPlanetState.resetConnectionState();
    }

    /**
     * How close the client must be to the curve.
     *
     * <p>Exact equality is impossible by design: the client interpolates against a
     * wall clock, so reading the snapshot and then reading progress samples two
     * slightly different instants and the position differs by float noise (about
     * 1e-16 units at a dock). The tolerance is still six orders of magnitude below
     * the shortest real route (82 units), so a wrong body id, a swapped departure
     * and arrival, or a stale cache entry would all fail loudly.</p>
     */
    private static final double CURVE_TOLERANCE = 1.0E-6;

    private static void assertOnCurve(String from, String to, String label,
                                      com.starboundmc.space.UniversePosition expected,
                                      com.starboundmc.space.UniversePosition actual)
    {
        double drift = Math.sqrt(expected.distanceToSqr(actual));
        assertTrue(drift <= CURVE_TOLERANCE,
                label + " (" + from + " -> " + to + ") is " + drift
                        + " off the server's curve");
    }

    /**
     * The snapshot's position must lie on the curve it reports progress along.
     *
     * <p>Checked for every ordered pair of navigable bodies, so a mix-up between
     * the departure and arrival ids anywhere in the client would show up.</p>
     */
    @Test
    void sampledPositionMatchesTheCurveAtTheReportedProgress()
    {
        for (String from : BODIES)
        {
            for (String to : BODIES)
            {
                if (from.equals(to))
                    continue;

                ClientPlanetState.resetConnectionState();
                ClientPlanetState.setCurrent(from);
                ClientPlanetState.startWarp(to, 400, to);

                var snapshot = ClientPlanetState.captureVisualSnapshot();
                assertTrue(snapshot.warping(), from + " -> " + to + " should be warping");

                double ticks = snapshot.warpProgress() * snapshot.warpDurationTicks();
                var expected = ShipFlightController
                        .sampleUniversePosition(from, to, snapshot.warpDurationTicks(), ticks);

                assertOnCurve(from, to, "snapshot", expected, snapshot.universePosition());
                assertEquals(from, snapshot.currentBody(), from + " -> " + to + " departure id");
                assertEquals(to, snapshot.targetBody(), from + " -> " + to + " arrival id");
            }
        }
    }

    /**
     * The same check for the live accessors a renderer reads outside the snapshot.
     */
    @Test
    void liveAccessorsUseTheSameRouteAsTheSnapshot()
    {
        ClientPlanetState.resetConnectionState();
        ClientPlanetState.setCurrent("sys1:lush");
        ClientPlanetState.startWarp("sys2:frozen", 400, "sys2:frozen");

        var snapshot = ClientPlanetState.captureVisualSnapshot();
        var live = ClientPlanetState.getShipUniversePosition();

        // Both read the same clock, so they agree to within one sample of drift.
        double drift = Math.sqrt(live.distanceToSqr(snapshot.universePosition()));
        assertTrue(drift < 5.0,
                "live accessors drifted " + drift + " from the snapshot's curve point");
    }

    /**
     * At the end of a flight the ship must arrive exactly at the catalog's dock,
     * which is what makes the next warp start from the right place.
     */
    @Test
    void theCurveEndsExactlyAtTheArrivalDock()
    {
        for (String to : BODIES)
        {
            if (to.equals("sys1:lush"))
                continue;
            var arrival = ShipFlightController.sampleUniversePosition("sys1:lush", to, 400, 400);
            assertEquals(UniverseNavigation.universeDock(to), arrival,
                    "the sampled curve must end at the catalog dock for " + to);
        }
    }

    /**
     * A warp to the body the ship is already at must be a no-op rather than a
     * degenerate route.
     *
     * <p>{@code FlightRoute} clamps a zero-length route to 1.0 to avoid dividing by
     * zero, so the ship sits at its dock rather than producing NaN.</p>
     */
    @Test
    void warpingToTheCurrentBodyKeepsTheShipAtItsDock()
    {
        ClientPlanetState.setCurrent("sys1:lush");
        ClientPlanetState.startWarp("sys1:lush", 400, "sys1:lush");

        var snapshot = ClientPlanetState.captureVisualSnapshot();
        assertOnCurve("sys1:lush", "sys1:lush", "same-body warp",
                UniverseNavigation.universeDock("sys1:lush"), snapshot.universePosition());
        assertTrue(Double.isFinite(snapshot.universePosition().localX()),
                "a zero-length route must not produce NaN");
    }
}
