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

/**
 * Client mirror that continuously re-samples the same manoeuvre curve as the
 * server.
 *
 * <p>The ship's location is identified by body <b>entry id</b>, not by the legacy
 * {@code Planet} enum (migration step A6). That matters because the entry id is
 * what the save, the flight packets and the universe registry all agree on,
 * whereas the enum can only ever name the four bodies that existed when it was
 * written.</p>
 */
public final class ClientPlanetState {
    private static String current=BuiltInUniverse.STARTER_BODY_ID,warpTarget;
    private static String warpEntryId,currentEntryId;
    private static int fuel=100,maxFuel=100,elapsedTicks,totalTicks=1; private static List<String> visited=List.of();
    private static long revision=-1,receivedNanos; private static boolean arrivalCue; private static FlightPhase phase=FlightPhase.DOCKED;
    private static Vec3 position=UniverseNavigation.vDock(BuiltInUniverse.STARTER_BODY_ID),velocity=Vec3.ZERO; private static float yaw,pitch,roll;
    private static UniversePosition synchronizedUniversePosition=UniverseNavigation.universeDock(BuiltInUniverse.STARTER_BODY_ID);
    private static final FlightVisualClock VISUAL_CLOCK = new FlightVisualClock();
    private ClientPlanetState(){}

    /** Start a new network session so a restarted server may begin its snapshot revision at zero. */
    public static synchronized void resetConnectionState(){
        revision=-1;receivedNanos=System.nanoTime();arrivalCue=false;phase=FlightPhase.DOCKED;
        warpTarget=null;warpEntryId=null;elapsedTicks=0;totalTicks=1;velocity=Vec3.ZERO;
        VISUAL_CLOCK.reset();
        snapDock(current);
    }
    public static synchronized void setCurrent(String entryId){
        if(entryId==null)return;
        if(phase!=FlightPhase.DOCKED&&!entryId.equals(current))arrivalCue=true;
        current=entryId;
        // The synced entry id is the authoritative identity for the star map, so
        // acknowledge it here instead of waiting for a star-state packet.
        currentEntryId=entryId;
        if(phase==FlightPhase.DOCKED)snapDock(entryId);
    }
    public static synchronized void startWarp(String targetEntryId,int duration,String entry){
        if(targetEntryId==null)return;
        warpTarget=targetEntryId;warpEntryId=entry;totalTicks=Math.max(1,duration);
        // WarpStartPacket can arrive a frame before the first authoritative
        // flight snapshot. Mark the client as entering TURN immediately so
        // the destination is not rendered at its raw far-away coordinate and
        // then hidden again when the snapshot switches phase from DOCKED.
        if (phase == FlightPhase.DOCKED)
        {
            phase=FlightPhase.TURN; elapsedTicks=0; receivedNanos=System.nanoTime();
            velocity=Vec3.ZERO;
            VISUAL_CLOCK.reset();
        }
        preloadPlanetSystem(targetEntryId);
    }
    public static void applyFlightSnapshot(long rev,long serverTick,FlightPhase next,double x,double y,double z,double vx,double vy,double vz,float yv,float pv,float rv,int elapsed,int total,String entry){
        applyFlightSnapshot(rev,serverTick,next,UniversePosition.fromLegacy(new Vec3(x,y,z)),new UniverseDelta(vx,vy,vz),yv,pv,rv,elapsed,total,entry);
    }
    public static synchronized void applyFlightSnapshot(long rev,long serverTick,FlightPhase next,UniversePosition nextPosition,UniverseDelta nextVelocity,float yv,float pv,float rv,int elapsed,int total,String entry){
        if(rev<revision)return;if(phase!=FlightPhase.DOCKED&&next==FlightPhase.DOCKED)arrivalCue=true;
        // Keep client interpolation monotonic: if we have extrapolated ahead of the new packet, don't snap back one frame (causes the planet to flash to front). Instead bias receivedNanos so sampledTicks continues from where we were.
        double prevSample = isWarping() && warpTarget != null ? sampledTicks() : elapsedTicks;
        revision=rev;phase=next;synchronizedUniversePosition=nextPosition;position=new Vec3(nextPosition.localX(),nextPosition.localY(),nextPosition.localZ());velocity=nextVelocity.toVec3();yaw=yv;pitch=pv;roll=rv;elapsedTicks=Math.max(0,elapsed);totalTicks=Math.max(1,total);warpEntryId=entry;
        // The target entry id arrives on the wire, so the destination identity no
        // longer has to be re-derived from the legacy enum.
        if(entry!=null&&!entry.equals(warpTarget))preloadPlanetSystem(entry);
        warpTarget=entry;
        long now = System.nanoTime();
        if (isWarping() && warpTarget != null && prevSample > elapsedTicks)
        {
            // We had extrapolated to prevSample; keep continuity by pretending the packet arrived earlier.
            double ahead = prevSample - elapsedTicks;
            long biasNanos = (long)(Math.min(0.30, ahead / 20.0) * 1e9);
            receivedNanos = now - biasNanos;
        }
        else receivedNanos = now;
    }
    private static void snapDock(String entryId){
        String bodyId = entryId!=null&&UniverseNavigation.isNavigable(entryId)
                ? entryId : BuiltInUniverse.STARTER_BODY_ID;
        synchronizedUniversePosition=UniverseNavigation.universeDock(bodyId);
        position=synchronizedUniversePosition.toLocalVec3();velocity=Vec3.ZERO;
        yaw=(float)UniverseNavigation.yawDock(bodyId);pitch=roll=0;}

    /**
     * Entry id safe to sample a route with.
     *
     * <p>A warp needs both ends navigable. If the warp target is not (a
     * placeholder body, or an id from a datapack the current server does not
     * have), the curve falls back to the current body so a frame cannot sample an
     * undefined route.</p>
     */
    private static boolean isNavigable(String entryId){
        return entryId!=null&&UniverseNavigation.isNavigable(entryId);}
    /** True when a route between the current body and the target is well defined. */
    private static boolean canSampleRoute(){
        return isWarping()&&isNavigable(current)&&isNavigable(warpTarget);}
    private static double sampledTicks(){
        if (!isWarping() || warpTarget == null)
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
        String snapshotTarget = warpTarget;
        boolean snapshotWarping = phase != FlightPhase.DOCKED;
        // Both ends must be real bodies in the current universe. An unknown target
        // (a datapack the server no longer has) must not sample a route at all,
        // which matches the server refusing to fly to an unknown place.
        boolean canSampleCurve = snapshotWarping && isNavigable(current) && isNavigable(snapshotTarget);
        double ticks = canSampleCurve ? sampledTicks() : elapsedTicks;
        String fromId = current;
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
                progress, totalTicks, current, snapshotTarget, currentEntryId, warpEntryId);
    }

    public static synchronized Vec3 getShipPosition(){return getShipUniversePosition().toLocalVec3();}
    public static synchronized UniversePosition getShipUniversePosition(){
        if(!canSampleRoute())return synchronizedUniversePosition;
        return ShipFlightController.sampleUniversePosition(
                current,warpTarget,totalTicks,sampledTicks());
    }
    public static synchronized Vec3 getShipVelocity(){return velocity;}
    public static synchronized double getShipYaw(){
        if(!canSampleRoute())return yaw;
        return ShipFlightController.sampleYaw(
                current,warpTarget,totalTicks,sampledTicks());}
    public static synchronized double getShipPitch(){
        if(!canSampleRoute())return pitch;
        return ShipFlightController.samplePitch(
                current,warpTarget,totalTicks,sampledTicks());}
    public static synchronized double getShipRoll(){
        if(!canSampleRoute())return roll;
        return ShipFlightController.sampleRoll(
                current,warpTarget,totalTicks,sampledTicks());}
    public static synchronized FlightPhase getFlightPhase(){return phase;} public static synchronized boolean isWarping(){return phase!=FlightPhase.DOCKED;}
    public static synchronized boolean consumeArrivalCue(){boolean c=arrivalCue;arrivalCue=false;return c;}
    public static synchronized float warpProgress(){return Mth.clamp((float)(sampledTicks()/totalTicks),0,1);} public static synchronized int getWarpDurationTicks(){return totalTicks;}
    public static synchronized double getShipX(){return getShipPosition().x;}public static synchronized double getShipY(){return getShipPosition().y;}public static synchronized double getShipZ(){return getShipPosition().z;}
    /** The body the ship is docked at or travelling from, as an entry id. */
    public static synchronized String getCurrent(){return current;}
    /** The body being flown to, or null when docked. */
    public static synchronized String getWarpTarget(){return warpTarget;}
    public static synchronized String getWarpEntryId(){return warpEntryId;}
    public static synchronized void setFuel(int f,int m){fuel=f;maxFuel=Math.max(1,m);}public static synchronized int getFuel(){return fuel;}public static synchronized int getMaxFuel(){return maxFuel;}
    public static synchronized void setStarState(List<String> v,String e){visited=List.copyOf(v);currentEntryId=e;}public static synchronized boolean isVisited(String e){return e!=null&&visited.contains(e);}
    /**
     * The authoritative current body.
     *
     * <p>Falls back to the locally tracked body while the star-state packet has
     * not arrived, so a screen opened immediately after joining still shows the
     * right place instead of nothing.</p>
     */
    public static synchronized String getCurrentEntryId(){return currentEntryId!=null?currentEntryId:current;}

    /** Immutable state used to render one frame without mixing network updates. */
    public record VisualSnapshot(Vec3 position, UniversePosition universePosition,
                                 Vec3 velocity, double yaw, double pitch, double roll,
                                 FlightPhase flightPhase, boolean warping,
                                 float warpProgress, int warpDurationTicks,
                                 String currentBody, String targetBody,
                                 String currentEntryId, String targetEntryId) {}

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
        // Every body the renderer draws authors its own texture, so there is no
        // legacy per-planet path to fall back to. A body without one is simply
        // not preloaded, which costs a decode later but never shows wrong art.
        String authored = body.spaceVisual().flatMap(visual -> visual.texture()).orElse(null);
        if (authored != null)
            textures.preload(ResourceLocation.parse(authored), Util.backgroundExecutor());
    }
}
