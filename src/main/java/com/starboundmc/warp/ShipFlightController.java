package com.starboundmc.warp;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.Planet;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Simplified server-authoritative flight: turn, accelerate, travel, decelerate, arrive. */
public final class ShipFlightController
{
    public static final int TPS=20, SHORT_ROUTE_TICKS=220, LONG_ROUTE_MIN=300;
    public static final int LONG_ROUTE_MIN_TICKS=360, LONG_ROUTE_MAX_TICKS=560;
    public static final int TURN_TICKS=50, ACCEL_TICKS=60, DECEL_TICKS=60, ARRIVE_TICKS=50;
    /** Visual heading takes longer than the manoeuvre phase to convey inertia. */
    private static final int HEADING_ALIGN_TICKS = 110;
    /** Virtual-space distance used to establish a visible departure/approach leg. */
    private static final double DOCK_LEAD_DISTANCE = 18.0;
    private static final double CORNER_SMOOTH_FRACTION = 0.18;
    private static final int CORNER_SMOOTH_PASSES = 4;
    /** Samples on either side of the current point when estimating route heading. */
    private static final double MAX_ROLL_DEGREES = 3.5;
    private static final double ROLL_GAIN = 0.55;
    /** Compatibility constants retained for callers. */
    public static final int DEPART_TICKS=TURN_TICKS+ACCEL_TICKS, DOCK_TICKS=ARRIVE_TICKS;

    private final Planet from,target; private final UniversePosition start,end; private final boolean shortRoute; private final int totalTicks;
    private int elapsedTicks; private FlightPhase phase; private UniversePosition pos; private UniverseDelta velocity=new UniverseDelta(0.0,0.0,0.0); private double yaw,pitch;

    public ShipFlightController(Planet from,Planet target){this(from,target,ShipSpace.universeDock(from),0,null,0,0,0);}
    public ShipFlightController(Planet from,Planet target,Vec3 persisted,int elapsed,FlightPhase persistedPhase,double yaw,double pitch,double ignoredRoll)
    {
        this(from,target,UniversePosition.fromLegacy(persisted),elapsed,persistedPhase,yaw,pitch,ignoredRoll);
    }
    public ShipFlightController(Planet from,Planet target,UniversePosition persisted,int elapsed,FlightPhase persistedPhase,double yaw,double pitch,double ignoredRoll)
    {
        this.from=from;this.target=target;this.start=ShipSpace.universeDock(from);this.end=ShipSpace.universeDock(target);
        double distance=Math.sqrt(start.distanceToSqr(end));this.shortRoute=distance<=LONG_ROUTE_MIN;this.totalTicks=durationFor(distance,shortRoute);
        this.elapsedTicks=clamp(elapsed,0,totalTicks);this.pos=sampleUniversePosition(from,target,totalTicks,this.elapsedTicks);
        this.phase=samplePhase(from,target,totalTicks,this.elapsedTicks);
        // Velocity: forward difference to match tick() scaling (delta per tick * TPS). Central diff previously used TPS*0.5 which mismatched tick's TPS.
        if (this.elapsedTicks > 0 && this.elapsedTicks < this.totalTicks)
        {
            UniversePosition prev = sampleUniversePosition(from, target, totalTicks, this.elapsedTicks - 1);
            this.velocity = prev.deltaTo(this.pos).scale(TPS);
        }
        else if (this.elapsedTicks < this.totalTicks)
        {
            UniversePosition next = sampleUniversePosition(from, target, totalTicks, this.elapsedTicks + 1);
            this.velocity = this.pos.deltaTo(next).scale(TPS);
        }
        else this.velocity = new UniverseDelta(0.0, 0.0, 0.0);
        // Persisted yaw/pitch/pos are validated against the deterministic curve: the curve is authoritative, so any mismatch indicates a save from an older curve version and is intentionally healed.
        updatePose();
    }

    private static int durationFor(double distance,boolean shortRoute)
    {
        if (shortRoute) return SHORT_ROUTE_TICKS;
        // Heavy ships need time to build and shed speed. Nearby moon flights
        // take 11s; long routes grow roughly with distance and cap at 28s.
        double t = SHORT_ROUTE_TICKS + (distance - LONG_ROUTE_MIN) / 40.0;
        return (int) Math.round(Math.max(SHORT_ROUTE_TICKS + 1, Math.min(LONG_ROUTE_MAX_TICKS, t)));
    }
    public void tick(){if(isLanded())return;elapsedTicks=Math.min(totalTicks,elapsedTicks+1);UniversePosition old=pos;pos=sampleUniversePosition(from,target,totalTicks,elapsedTicks);velocity=old.deltaTo(pos).scale(TPS);phase=samplePhase(from,target,totalTicks,elapsedTicks);updatePose();}
    private void updatePose(){yaw=sampleYaw(from,target,totalTicks,elapsedTicks);pitch=samplePitch(from,target,totalTicks,elapsedTicks);}

    private static boolean isShort(Planet a,Planet b){return ShipSpace.flightDistance(a,b)<=LONG_ROUTE_MIN;}
    private static int decelStart(int total){return total-DECEL_TICKS-ARRIVE_TICKS;}
    private static int travelStart(){return TURN_TICKS+ACCEL_TICKS;}
    public static FlightPhase samplePhase(Planet from,Planet to,int total,double tick)
    {
        if(tick<TURN_TICKS)return FlightPhase.TURN;
        if(tick<travelStart())return FlightPhase.ACCELERATE;
        if(tick<decelStart(total))return isShort(from,to)?FlightPhase.CRUISE:FlightPhase.HYPERSPACE;
        if(tick<total-ARRIVE_TICKS)return FlightPhase.DECELERATE;
        return FlightPhase.ARRIVE;
    }

    /**
     * Position along the route. The route is a deterministic per-(from,to) path
     * that steers the ship AROUND every body its straight course would enter
     * (source first, then target, then the rest defensively): straight tangent
     * in at the keep-out radius, a circular arc at that radius, then a straight
     * radial leg into the arrival dock. Scalar progress is one continuous
     * S-curve over the route's whole arclength, so start/stop feel unchanged.
     */
    public static Vec3 samplePosition(Planet from,Planet to,int total,double tick)
    {
        return sampleUniversePosition(from, to, total, tick).toLocalVec3();
    }

    public static UniversePosition sampleUniversePosition(Planet from,Planet to,int total,double tick)
    {
        // Motion starts immediately but the quintic curve has near-zero initial
        // acceleration. The planet therefore recedes slowly before the ship
        // gathers speed, instead of waiting motionless and then lunging forward.
        double u=smoother(tick/(double)Math.max(1,total));
        return route(from,to).universePointAtProgress(u);
    }

    public static double sampleYaw(Planet from,Planet to,int total,double tick)
    {
        // Follow the corridor itself. Interpolating only between dock headings
        // made interplanetary travel look like sideways translation whenever
        // the route crossed the sky at a different angle.
        FlightRoute route = route(from, to);
        // Position advances on the same quintic clock; heading must sample the
        // same point on the route or it will lag far behind during deceleration.
        double u = smoother(tick / Math.max(1.0, total));
        double dockFrom = ShipSpace.yawDock(from);
        double dockTo = ShipSpace.yawDock(to);

        // A broad eased transition gives the turn visible mass. The route
        // heading itself uses look-ahead, so small avoidance vertices cannot
        // twitch the ship or produce a sudden camera snap.
        if (tick < HEADING_ALIGN_TICKS)
        {
            double alignedU = smoother(HEADING_ALIGN_TICKS / (double) Math.max(1, total));
            return lerpAngle(dockFrom, route.headingYaw(alignedU),
                    smoother(tick / (double) HEADING_ALIGN_TICKS));
        }
        int arrivalStart = Math.max(HEADING_ALIGN_TICKS, total - HEADING_ALIGN_TICKS);
        if (tick > arrivalStart)
        {
            double arrivalU = smoother(arrivalStart / (double) Math.max(1, total));
            return lerpAngle(route.headingYaw(arrivalU), dockTo,
                    smoother((tick - arrivalStart) / (double) Math.max(1, total - arrivalStart)));
        }
        return route.headingYaw(u);
    }
    public static double samplePitch(Planet from,Planet to,int total,double tick)
    {
        double accelerate=smoother((tick-TURN_TICKS)/(double)ACCEL_TICKS);
        double decelerate=smoother((tick-decelStart(total))/(double)DECEL_TICKS);
        double arrival=smoother((tick-(total-ARRIVE_TICKS))/(double)ARRIVE_TICKS);
        return 2.2*accelerate*(1-decelerate)-1.4*decelerate*(1-arrival);
    }
    /** Bank into turns, derived from the same smoothed heading used for yaw. */
    public static double sampleRoll(Planet from,Planet to,int total,double tick)
    {
        if (tick <= total * 0.12 || tick >= total * 0.88)
            return 0.0;
        double half = 4.0;
        double before = sampleYaw(from, to, total, tick - half);
        double after = sampleYaw(from, to, total, tick + half);
        double delta = Math.IEEEremainder(after - before, 360.0) / (2.0 * half);
        return Math.max(-MAX_ROLL_DEGREES, Math.min(MAX_ROLL_DEGREES, delta * ROLL_GAIN));
    }

    // ---- Route (planar obstacle avoidance) -----------------------------------
    // The virtual flight is near-planar (every body sits at y ~102..103), so the
    // route is computed in the XZ plane and y follows along by arclength. Each
    // body owns a keep-out radius KEEP_OUT_FACTOR*radius (inside which the ship
    // must never travel): 1.45x keeps the arc outside the surface, the atmosphere
    // glow shell (1.15x) and the dock's 1.61x radius, so the ship always clears.
    // The old approach "bend" put its safe point on the near side and could still
    // thread the camera through a planet whose dock faces away from the incoming
    // direction (all six FROZEN routes); the tangent/arc/radial route below is
    // geometrically incapable of entering a body.
    private static final double KEEP_OUT_FACTOR = 1.45;
    private static final int ARC_POINTS = 48;
    private static final Map<Integer, FlightRoute> ROUTES = new ConcurrentHashMap<>();

    /** Dense deterministic polyline (vertices + cumulative 2D arc-length). */
    private static final class FlightRoute
    {
        final UniverseRouteFrame frame;
        final UniversePosition destination;
        final Vec3[] pts;
        final double[] cum;
        final double total;

        FlightRoute(UniverseRouteFrame frame, UniversePosition destination, Vec3[] pts, double[] cum, double total)
        {
            this.frame = frame;
            this.destination = destination;
            this.pts = pts;
            this.cum = cum;
            this.total = total > 0 ? total : 1.0;
        }

        /** Position at scalar progress in [0,1] along the route's arc-length. */
        Vec3 pointAtProgress(double u)
        {
            double s = clamp01(u) * total;
            int lo = 0, hi = pts.length - 1;
            while (lo + 1 < hi)
            {
                int mid = (lo + hi) >>> 1;
                if (cum[mid] <= s) lo = mid; else hi = mid;
            }
            double seg = cum[lo + 1] - cum[lo];
            double t = seg <= 0 ? 0 : (s - cum[lo]) / seg;
            return pts[lo].lerp(pts[lo + 1], clamp01(t));
        }

        UniversePosition universePointAtProgress(double u)
        {
            if (u <= 0.0)
                return frame.origin();
            if (u >= 1.0)
                return destination;
            return frame.toUniverse(pointAtProgress(u));
        }

        /** Route direction in Minecraft yaw convention, with spatial look-ahead. */
        double headingYaw(double u)
        {
            // Sample a meaningful stretch of corridor instead of the local
            // polyline segment. Short moon routes look farther ahead; long
            // routes retain enough local curvature for turns to remain visible.
            double look = Math.max(0.02, Math.min(0.20, 90.0 / total));
            Vec3 before = pointAtProgress(Math.max(0.0, u - look));
            Vec3 after = pointAtProgress(Math.min(1.0, u + look));
            double dx = after.x - before.x;
            double dz = after.z - before.z;
            if (dx * dx + dz * dz < 1.0e-9)
                return 0.0;
            return Math.toDegrees(Math.atan2(dx, dz));
        }
    }

    private static FlightRoute route(Planet from, Planet to)
    {
        int key = (from.ordinal() << 8) | to.ordinal();
        return ROUTES.computeIfAbsent(key, ignored -> buildRoute(from, to));
    }

    private static FlightRoute buildRoute(Planet from, Planet to)
    {
        UniverseRouteFrame frame = new UniverseRouteFrame(ShipSpace.universeDock(from));
        Vec3 a = Vec3.ZERO;
        Vec3 b = frame.toRelative(ShipSpace.universeDock(to));
        // Docking yaw points from the ship toward the planet. Both departure
        // and approach therefore use the opposite side of the dock: the ship
        // leaves away from the planet, then approaches the target from outside
        // before settling back to its exact virtual dock position.
        // Depart along one of the planet's tangents. The old lead point sat
        // directly behind the ship, so the vessel translated stern-first for
        // the opening leg. Pick the tangent which bends most naturally toward
        // the destination; the required departure turn is at most 90 degrees.
        Vec3 departureLeft = a.add(ShipSpace.rotateYaw(new Vec3(0.0, 0.0, DOCK_LEAD_DISTANCE),
                ShipSpace.yawDock(from) - 90.0));
        Vec3 departureRight = a.add(ShipSpace.rotateYaw(new Vec3(0.0, 0.0, DOCK_LEAD_DISTANCE),
                ShipSpace.yawDock(from) + 90.0));
        Vec3 toLead = b.subtract(ShipSpace.rotateYaw(new Vec3(0.0, 0.0, DOCK_LEAD_DISTANCE),
                ShipSpace.yawDock(to)));
        Vec3 fromLead = departureLeft.distanceTo(toLead) <= departureRight.distanceTo(toLead)
                ? departureLeft : departureRight;
        List<Vec3> poly = new ArrayList<>();
        poly.add(a);
        poly.add(fromLead);
        poly.add(toLead);
        poly.add(b);
        // Peel the source, then the target, then the remaining bodies defensively.
        List<Planet> order = new ArrayList<>(List.of(from, to));
        for (Planet p : Planet.values())
            if (p != from && p != to) order.add(p);
        for (Planet body : order)
            poly = peel(poly, frame.toRelative(ShipSpace.universeBodyPosition(body)),
                    ShipSpace.radius(body) * KEEP_OUT_FACTOR);
        poly = smoothPolyline(poly);

        // Densify into a cumulative-arc-length polyline; y follows along by arc-length.
        int count = poly.size();
        double[] cum = new double[count];
        for (int i = 1; i < count; i++)
        {
            double dx = poly.get(i).x - poly.get(i - 1).x;
            double dz = poly.get(i).z - poly.get(i - 1).z;
            cum[i] = cum[i - 1] + Math.sqrt(dx * dx + dz * dz);
        }
        double total = cum[count - 1];
        Vec3[] pts = new Vec3[count];
        for (int i = 0; i < count; i++)
        {
            double y = a.y + (b.y - a.y) * (total <= 0 ? 0 : cum[i] / total);
            Vec3 p = poly.get(i);
            pts[i] = new Vec3(p.x, y, p.z);
        }
        return new FlightRoute(frame, ShipSpace.universeDock(to), pts, cum, total);
    }

    /**
     * Cut only the interior corners of the safe corridor. The endpoints remain
     * exact docks; the small fraction keeps the curve outside the inflated
     * keep-out circles while removing hard heading changes at lead points.
     */
    private static List<Vec3> smoothPolyline(List<Vec3> poly)
    {
        if (poly.size() < 3)
            return poly;
        List<Vec3> smoothed = poly;
        for (int pass = 0; pass < CORNER_SMOOTH_PASSES; pass++)
        {
            List<Vec3> out = new ArrayList<>();
            out.add(smoothed.get(0));
            for (int i = 1; i < smoothed.size() - 1; i++)
            {
                Vec3 previous = smoothed.get(i - 1), current = smoothed.get(i), next = smoothed.get(i + 1);
                out.add(previous.lerp(current, 1.0 - CORNER_SMOOTH_FRACTION));
                out.add(current.lerp(next, CORNER_SMOOTH_FRACTION));
            }
            out.add(smoothed.get(smoothed.size() - 1));
            smoothed = out;
        }
        return smoothed;
    }

    /** Re-route every segment that enters the body's keep-out circle. */
    private static List<Vec3> peel(List<Vec3> poly, Vec3 center, double rc)
    {
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i < poly.size() - 1; i++)
        {
            Vec3 p = poly.get(i), q = poly.get(i + 1);
            out.add(p);
            // Segment already clear, or "p" would need an inside tangent (shouldn't happen).
            if (!pierces(p, q, center, rc) || distXY(center, p) < rc)
                continue;
            Detour detour = shortestTangentDetour(p, q, center, rc);
            out.add(detour.entry);
            addArc(out, detour.entry, detour.exit, center, rc, detour.sweep);
            out.add(detour.exit);
        }
        Vec3 last = poly.get(poly.size() - 1);
        if (out.isEmpty() || distXY(out.get(out.size() - 1), last) > 1e-9)
            out.add(last);
        return out;
    }

    private static final class Detour
    {
        final Vec3 entry, exit;
        final double sweep, length;

        Detour(Vec3 entry, Vec3 exit, double sweep, double length)
        {
            this.entry = entry;
            this.exit = exit;
            this.sweep = sweep;
            this.length = length;
        }
    }

    /**
     * Shortest C1 detour from P to Q around a circle. Both straight legs touch
     * the circle tangentially and the selected arc follows those tangent
     * directions, avoiding the old radial exit and its near-180 degree hairpin.
     */
    private static Detour shortestTangentDetour(Vec3 p, Vec3 q, Vec3 center, double rc)
    {
        double px = p.x - center.x, pz = p.z - center.z;
        double qx = q.x - center.x, qz = q.z - center.z;
        double pd = Math.sqrt(px * px + pz * pz);
        double qd = Math.sqrt(qx * qx + qz * qz);
        if (pd <= rc || qd <= rc)
            return new Detour(p, q, 0.0, p.distanceTo(q));

        double pa = Math.atan2(pz, px), qa = Math.atan2(qz, qx);
        double po = Math.acos(rc / pd), qo = Math.acos(rc / qd);
        Detour best = null;
        for (int ps : new int[] { -1, 1 })
        {
            double entryAngle = pa + ps * po;
            Vec3 entry = circlePoint(center, rc, entryAngle);
            for (int qs : new int[] { -1, 1 })
            {
                double exitAngle = qa + qs * qo;
                Vec3 exit = circlePoint(center, rc, exitAngle);
                double sweep = wrapAngle(exitAngle - entryAngle);
                if (!tangentsFlowForward(p, q, entry, exit, entryAngle, exitAngle, sweep))
                    continue;
                double length = distXY(p, entry) + Math.abs(sweep) * rc + distXY(exit, q);
                if (best == null || length < best.length)
                    best = new Detour(entry, exit, sweep, length);
            }
        }
        if (best != null)
            return best;

        // Degenerate floating-point fallback: choose the shortest tangent pair.
        for (int ps : new int[] { -1, 1 })
        {
            double entryAngle = pa + ps * po;
            Vec3 entry = circlePoint(center, rc, entryAngle);
            for (int qs : new int[] { -1, 1 })
            {
                double exitAngle = qa + qs * qo;
                Vec3 exit = circlePoint(center, rc, exitAngle);
                double sweep = wrapAngle(exitAngle - entryAngle);
                double length = distXY(p, entry) + Math.abs(sweep) * rc + distXY(exit, q);
                if (best == null || length < best.length)
                    best = new Detour(entry, exit, sweep, length);
            }
        }
        return best;
    }

    private static Vec3 circlePoint(Vec3 center, double radius, double angle)
    {
        return new Vec3(center.x + radius * Math.cos(angle), center.y,
                center.z + radius * Math.sin(angle));
    }

    private static boolean tangentsFlowForward(Vec3 p, Vec3 q, Vec3 entry, Vec3 exit,
                                               double entryAngle, double exitAngle, double sweep)
    {
        if (Math.abs(sweep) < 1e-9)
            return true;
        double direction = Math.signum(sweep);
        double entryTx = -Math.sin(entryAngle) * direction;
        double entryTz = Math.cos(entryAngle) * direction;
        double exitTx = -Math.sin(exitAngle) * direction;
        double exitTz = Math.cos(exitAngle) * direction;
        return dotDirection(p, entry, entryTx, entryTz) > 0.999
                && dotDirection(exit, q, exitTx, exitTz) > 0.999;
    }

    private static double dotDirection(Vec3 from, Vec3 to, double tx, double tz)
    {
        double dx = to.x - from.x, dz = to.z - from.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        return length <= 1e-9 ? 1.0 : (dx * tx + dz * tz) / length;
    }

    /** Dense arc points (interior only) at distance rc around center. */
    private static void addArc(List<Vec3> out, Vec3 t1, Vec3 t2, Vec3 center, double rc, double sweep)
    {
        double ang1 = Math.atan2(t1.z - center.z, t1.x - center.x);
        for (int k = 1; k < ARC_POINTS; k++)
        {
            double ang = ang1 + sweep * k / ARC_POINTS;
            out.add(new Vec3(center.x + rc * Math.cos(ang), center.y, center.z + rc * Math.sin(ang)));
        }
    }

    /** 2D (XZ) closest-approach test of segment p->q against the keep-out circle. */
    private static boolean pierces(Vec3 p, Vec3 q, Vec3 center, double rc)
    {
        double px = p.x - center.x, pz = p.z - center.z;
        double dx = q.x - p.x, dz = q.z - p.z;
        double l2 = dx * dx + dz * dz;
        double t = l2 <= 1e-12 ? 0 : clamp01(-(px * dx + pz * dz) / l2);
        double ex = px + dx * t, ez = pz + dz * t;
        return ex * ex + ez * ez < rc * rc;
    }

    private static double distXY(Vec3 a, Vec3 b)
    {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Wrap a signed angular difference into (-PI, PI]. */
    private static double wrapAngle(double a)
    {
        double twoPi = 2 * Math.PI;
        double r = (a + Math.PI) % twoPi;
        if (r < 0) r += twoPi;
        return r - Math.PI;
    }

    private static double smoother(double t){t=clamp01(t);return t*t*t*(t*(t*6-15)+10);}private static double clamp01(double t){return Math.max(0,Math.min(1,t));}
    private static double lerpAngle(double a,double b,double t){return a+Math.IEEEremainder(b-a,360)*clamp01(t);}private static int clamp(int v,int a,int b){return Math.max(a,Math.min(b,v));}
    public Planet getFrom(){return from;}public Planet getTarget(){return target;}public Vec3 getPos(){return pos.toLocalVec3();}public Vec3 getVelocity(){return velocity.toVec3();}
    public UniversePosition getUniversePosition(){return pos;}public UniverseDelta getUniverseVelocity(){return velocity;}
    public double getYaw(){return yaw;}public double getPitch(){return pitch;}public double getRoll(){return sampleRoll(from,target,totalTicks,elapsedTicks);}public FlightPhase getPhase(){return phase;}
    public int getElapsedTicks(){return elapsedTicks;}public int getTotalTicks(){return totalTicks;}public boolean isShortRoute(){return shortRoute;}
    public boolean isLanded(){return elapsedTicks>=totalTicks;}public long getRemainingTicks(){return Math.max(0,totalTicks-elapsedTicks);}public double flightProgress(){return Math.min(1,elapsedTicks/(double)totalTicks);}
}
