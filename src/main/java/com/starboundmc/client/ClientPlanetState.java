package com.starboundmc.client;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.universe.BuiltInUniverse;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Connection-scoped flight projection; authoritative snapshots anchor deterministic visual replay. */
public final class ClientPlanetState {
    private static String currentEntryId=BuiltInUniverse.STARTER_BODY_ID,targetEntryId;
    private static int elapsedTicks,totalTicks=1; private static List<String> visited=List.of();
    private static long revision=-1,receivedNanos; private static boolean arrivalCue; private static FlightPhase phase=FlightPhase.DOCKED;
    private static UniverseDelta velocity=new UniverseDelta(0,0,0); private static float yaw,pitch,roll;
    private static UniversePosition synchronizedUniversePosition=UniverseNavigation.universeDock(BuiltInUniverse.STARTER_BODY_ID);
    private static final FlightVisualClock VISUAL_CLOCK = new FlightVisualClock();
    private ClientPlanetState(){}

    private static boolean crewHold;
    /** Start a new network session so a restarted server may begin its snapshot revision at zero. */
    public static synchronized void resetConnectionState(){
        crewHold=false;
        revision=-1;receivedNanos=System.nanoTime();arrivalCue=false;phase=FlightPhase.DOCKED;
        targetEntryId=null;elapsedTicks=0;totalTicks=1;velocity=new UniverseDelta(0,0,0);
        VISUAL_CLOCK.reset();
        currentEntryId=BuiltInUniverse.STARTER_BODY_ID; visited=List.of();
        snapDock(currentEntryId);
    }
    public static synchronized void setCurrent(String entryId){
        java.util.Objects.requireNonNull(entryId, "entryId");
        if(phase!=FlightPhase.DOCKED&&!entryId.equals(currentEntryId))arrivalCue=true;
        currentEntryId=entryId;
        if(phase==FlightPhase.DOCKED)snapDock(entryId);
    }
    public static synchronized void startWarp(String target,int duration){
        java.util.Objects.requireNonNull(target, "target");
        targetEntryId=target;totalTicks=Math.max(1,duration);
        // WarpStartPacket can arrive a frame before the first authoritative
        // flight snapshot. Mark the client as entering TURN immediately so
        // the destination is not rendered at its raw far-away coordinate and
        // then hidden again when the snapshot switches phase from DOCKED.
        if (phase == FlightPhase.DOCKED)
        {
            phase=FlightPhase.TURN; elapsedTicks=0; receivedNanos=System.nanoTime();
            velocity=new UniverseDelta(0,0,0);
            VISUAL_CLOCK.reset();
        }
        preloadPlanetSystem(targetEntryId);
    }
    public static synchronized void applyFlightSnapshot(long rev,long serverTick,FlightPhase next,UniversePosition nextPosition,UniverseDelta nextVelocity,float yv,float pv,float rv,int elapsed,int total,String entry){
        applyFlightSnapshot(rev, serverTick, next, nextPosition, nextVelocity, yv, pv, rv, elapsed, total, entry, false);
    }
    public static synchronized void applyFlightSnapshot(long rev,long serverTick,FlightPhase next,UniversePosition nextPosition,UniverseDelta nextVelocity,float yv,float pv,float rv,int elapsed,int total,String entry,boolean hold){
        if(rev<revision)return;if(phase!=FlightPhase.DOCKED&&next==FlightPhase.DOCKED)arrivalCue=true;
        // Keep client interpolation monotonic: if we have extrapolated ahead of the new packet, don't snap back one frame (causes the planet to flash to front). Instead bias receivedNanos so sampledTicks continues from where we were.
        double prevSample = isWarping() && targetEntryId != null ? sampledTicks() : elapsedTicks;
        if (crewHold != hold) VISUAL_CLOCK.reset();
        crewHold = hold;
        revision=rev;phase=next;synchronizedUniversePosition=nextPosition;velocity=nextVelocity;yaw=yv;pitch=pv;roll=rv;elapsedTicks=Math.max(0,elapsed);totalTicks=Math.max(1,total);
        if(entry!=null&&!entry.equals(targetEntryId))preloadPlanetSystem(entry);
        targetEntryId=entry;
        long now = System.nanoTime();
        if (!crewHold && isWarping() && targetEntryId != null && prevSample > elapsedTicks)
        {
            // We had extrapolated to prevSample; keep continuity by pretending the packet arrived earlier.
            double ahead = prevSample - elapsedTicks;
            long biasNanos = (long)(Math.min(0.30, ahead / 20.0) * 1e9);
            receivedNanos = now - biasNanos;
        }
        else receivedNanos = now;
    }
    private static void snapDock(String entryId){
        if (!isNavigable(entryId)) return;
        synchronizedUniversePosition=UniverseNavigation.universeDock(entryId);
        velocity=new UniverseDelta(0,0,0);
        yaw=(float)UniverseNavigation.yawDock(entryId);pitch=roll=0;
    }

    private static boolean isNavigable(String entryId){
        return entryId!=null&&UniverseNavigation.isNavigable(entryId);}
    /** True when a route between the current body and the target is well defined. */
    private static boolean canSampleRoute(){
        return isWarping()&&isNavigable(currentEntryId)&&isNavigable(targetEntryId);}
    private static double sampledTicks(){
        if (crewHold || !isWarping() || targetEntryId == null)
        {
            VISUAL_CLOCK.reset();
            return elapsedTicks;
        }
        long now = System.nanoTime();
        double raw = elapsedTicks + Math.min(.30, Math.max(0.0, (now - receivedNanos) / 1e9)) * 20.0;
        return VISUAL_CLOCK.sample(raw, totalTicks, now);
    }
    /**
     * Captures every value consumed by the space renderer at one visual time.
     * Network snapshots can arrive while a frame is being assembled; exposing
     * a single sample prevents position and heading from belonging to two
     * different snapshots and makes the nearby planet appear to jump.
     */
    public static synchronized VisualSnapshot captureVisualSnapshot()
    {
        String snapshotTarget = targetEntryId;
        boolean snapshotWarping = phase != FlightPhase.DOCKED;
        // Both ends must be real bodies in the current universe. An unknown target
        // (a datapack the server no longer has) must not sample a route at all,
        // which matches the server refusing to fly to an unknown place.
        boolean canSampleCurve = snapshotWarping && isNavigable(currentEntryId) && isNavigable(snapshotTarget);
        double ticks = canSampleCurve ? sampledTicks() : elapsedTicks;
        String fromId = currentEntryId;
        String toId = snapshotTarget;
        UniversePosition snapshotUniverse = canSampleCurve
                ? ShipFlightController.sampleUniversePosition(fromId, toId, totalTicks, ticks)
                : synchronizedUniversePosition;
        Vec3 snapshotPosition = snapshotUniverse.toLocalVec3();
        double snapshotYaw = canSampleCurve
                ? ShipFlightController.sampleYaw(fromId, toId, totalTicks, ticks) : yaw;
        double snapshotPitch = canSampleCurve
                ? ShipFlightController.samplePitch(fromId, toId, totalTicks, ticks) : pitch;
        double snapshotRoll = canSampleCurve
                ? ShipFlightController.sampleRoll(fromId, toId, totalTicks, ticks) : roll;
        float progress = Mth.clamp((float) (ticks / Math.max(1, totalTicks)), 0.0F, 1.0F);
        return new VisualSnapshot(snapshotPosition, snapshotUniverse, velocity,
                snapshotYaw, snapshotPitch, snapshotRoll, phase, snapshotWarping,
                progress, totalTicks, currentEntryId, snapshotTarget);
    }

    public static synchronized UniversePosition getShipUniversePosition(){
        if(!canSampleRoute())return synchronizedUniversePosition;
        return ShipFlightController.sampleUniversePosition(
                currentEntryId,targetEntryId,totalTicks,sampledTicks());
    }
    public static synchronized UniverseDelta getShipVelocity(){return velocity;}
    public static synchronized double getShipYaw(){
        if(!canSampleRoute())return yaw;
        return ShipFlightController.sampleYaw(
                currentEntryId,targetEntryId,totalTicks,sampledTicks());}
    public static synchronized double getShipPitch(){
        if(!canSampleRoute())return pitch;
        return ShipFlightController.samplePitch(
                currentEntryId,targetEntryId,totalTicks,sampledTicks());}
    public static synchronized double getShipRoll(){
        if(!canSampleRoute())return roll;
        return ShipFlightController.sampleRoll(
                currentEntryId,targetEntryId,totalTicks,sampledTicks());}
    public static synchronized FlightPhase getFlightPhase(){return phase;} public static synchronized boolean isWarping(){return phase!=FlightPhase.DOCKED;}
    /** Exhaust uses the same clock as navigation and shuts off during a crew safety hold. */
    public static synchronized float thrusterIntensity() {
        return ThrusterVisualState.intensity(phase, sampledTicks(), totalTicks, crewHold);
    }
    public static synchronized boolean consumeArrivalCue(){boolean c=arrivalCue;arrivalCue=false;return c;}
    public static synchronized float warpProgress(){return Mth.clamp((float)(sampledTicks()/totalTicks),0,1);} public static synchronized int getWarpDurationTicks(){return totalTicks;}
    /** The body the ship is docked at or travelling from, as an entry id. */
    public static synchronized String getCurrent(){return currentEntryId;}
    /** The body being flown to, or null when docked. */
    public static synchronized String getWarpTarget(){return targetEntryId;}
    /** The star map and route replay share the same authoritative current body id. */
    public static synchronized void setStarState(List<String> v,String e){
        visited=List.copyOf(v);
        setCurrent(e);

    }

    public static synchronized boolean isVisited(String e){return e!=null&&visited.contains(e);}
    /** Immutable state used to render one frame without mixing network updates. */
    public record VisualSnapshot(Vec3 position, UniversePosition universePosition,
                                 UniverseDelta velocity, double yaw, double pitch, double roll,
                                 FlightPhase flightPhase, boolean warping,
                                 float warpProgress, int warpDurationTicks,
                                 String currentBody, String targetBody) {}

    /** Decode destination textures off-thread while the ship is still in transit. */
    private static void preloadPlanetSystem(String entryId)
    {
        if (entryId == null)
            return;
        var body = UniverseNavigation.body(entryId);
        if (body == null)
            return;
        // Preloading is a smoothness optimisation. The texture manager does not
        // exist before the client is up (and never in a headless test), so a
        // missing client must not be able to stop a warp from starting.
        Minecraft client = Minecraft.getInstance();
        if (client == null)
            return;
        var textures = client.getTextureManager();
        preload(textures, body);
        // Bodies sharing this one's sky, so a primary and its moon do not decode
        // one after the other and pop in separately.
        for (var companion : UniverseNavigation.companionBodies(entryId))
            preload(textures, companion);
    }

    /**
     * Warms one body's ship-window texture.
     *
     * <p>Only bodies that author a sprite are preloaded. A body without one simply
     * decodes later, which costs a hitch but never shows the wrong art.</p>
     */
    private static void preload(net.minecraft.client.renderer.texture.TextureManager textures,
                                com.starboundmc.world.universe.CelestialBodyDefinition body)
    {
        // Only preload authored textures; missing assets remain visible as texture errors.
        String authored = body.spaceVisual().flatMap(visual -> visual.texture()).orElse(null);
        if (authored != null)
            textures.preload(ResourceLocation.parse(authored), Util.backgroundExecutor());
    }
}
