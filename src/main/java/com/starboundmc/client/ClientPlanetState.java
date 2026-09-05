package com.starboundmc.client;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.warp.ShipSpace;
import com.starboundmc.world.Planet;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Client mirror that continuously re-samples the same manoeuvre curve as the server. */
public final class ClientPlanetState {
    private static Planet current=Planet.LUSH,warpTarget; private static String warpEntryId,currentEntryId;
    private static int fuel=100,maxFuel=100,elapsedTicks,totalTicks=1; private static List<String> visited=List.of();
    private static long revision=-1,receivedNanos; private static boolean arrivalCue; private static FlightPhase phase=FlightPhase.DOCKED;
    private static Vec3 position=ShipSpace.vDock(Planet.LUSH),velocity=Vec3.ZERO; private static float yaw,pitch,roll;
    private static UniversePosition synchronizedUniversePosition=UniversePosition.fromLegacy(position);
    private static final FlightVisualClock VISUAL_CLOCK = new FlightVisualClock();
    private ClientPlanetState(){}
    /** Start a new network session so a restarted server may begin its snapshot revision at zero. */
    public static synchronized void resetConnectionState(){
        revision=-1;receivedNanos=System.nanoTime();arrivalCue=false;phase=FlightPhase.DOCKED;
        warpTarget=null;warpEntryId=null;elapsedTicks=0;totalTicks=1;velocity=Vec3.ZERO;
        VISUAL_CLOCK.reset();
        snapDock(current);
    }
    public static synchronized void setCurrent(Planet p){if(phase!=FlightPhase.DOCKED&&p!=current)arrivalCue=true;current=p;if(phase==FlightPhase.DOCKED)snapDock(p);}
    public static synchronized void startWarp(Planet t,int duration,String entry){
        warpTarget=t;warpEntryId=entry;totalTicks=Math.max(1,duration);
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
        preloadPlanetSystem(t);
    }
    public static void applyFlightSnapshot(long rev,long serverTick,FlightPhase next,double x,double y,double z,double vx,double vy,double vz,float yv,float pv,float rv,int elapsed,int total,String entry){
        applyFlightSnapshot(rev,serverTick,next,UniversePosition.fromLegacy(new Vec3(x,y,z)),new UniverseDelta(vx,vy,vz),yv,pv,rv,elapsed,total,entry);
    }
    public static synchronized void applyFlightSnapshot(long rev,long serverTick,FlightPhase next,UniversePosition nextPosition,UniverseDelta nextVelocity,float yv,float pv,float rv,int elapsed,int total,String entry){
        if(rev<revision)return;if(phase!=FlightPhase.DOCKED&&next==FlightPhase.DOCKED)arrivalCue=true;
        // Keep client interpolation monotonic: if we have extrapolated ahead of the new packet, don't snap back one frame (causes the planet to flash to front). Instead bias receivedNanos so sampledTicks continues from where we were.
        double prevSample = isWarping() && warpTarget != null ? sampledTicks() : elapsedTicks;
        revision=rev;phase=next;synchronizedUniversePosition=nextPosition;position=new Vec3(nextPosition.localX(),nextPosition.localY(),nextPosition.localZ());velocity=nextVelocity.toVec3();yaw=yv;pitch=pv;roll=rv;elapsedTicks=Math.max(0,elapsed);totalTicks=Math.max(1,total);warpEntryId=entry;
        var pe=entry==null?null:com.starboundmc.world.starmap.StarSystems.entryById(entry);
        Planet nextTarget=pe==null?null:pe.getDestination();
        if(nextTarget!=null&&nextTarget!=warpTarget)preloadPlanetSystem(nextTarget);
        warpTarget=nextTarget;
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
    private static void snapDock(Planet p){synchronizedUniversePosition=ShipSpace.universeDock(p);position=synchronizedUniversePosition.toLocalVec3();velocity=Vec3.ZERO;yaw=(float)ShipSpace.yawDock(p);pitch=roll=0;}
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
        Planet snapshotTarget = warpTarget;
        boolean snapshotWarping = phase != FlightPhase.DOCKED;
        boolean canSampleCurve = snapshotWarping && snapshotTarget != null;
        double ticks = canSampleCurve ? sampledTicks() : elapsedTicks;
        UniversePosition snapshotUniverse = canSampleCurve
                ? ShipFlightController.sampleUniversePosition(current, snapshotTarget, totalTicks, ticks)
                : synchronizedUniversePosition;
        Vec3 snapshotPosition = snapshotUniverse.toLocalVec3();
        double snapshotYaw = canSampleCurve
                ? ShipFlightController.sampleYaw(current, snapshotTarget, totalTicks, ticks) : yaw;
        double snapshotPitch = canSampleCurve
                ? ShipFlightController.samplePitch(current, snapshotTarget, totalTicks, ticks) : pitch;
        double snapshotRoll = canSampleCurve
                ? ShipFlightController.sampleRoll(current, snapshotTarget, totalTicks, ticks) : roll;
        float progress = Mth.clamp((float) (ticks / Math.max(1, totalTicks)), 0.0F, 1.0F);
        return new VisualSnapshot(snapshotPosition, snapshotUniverse, velocity,
                snapshotYaw, snapshotPitch, snapshotRoll, phase, snapshotWarping,
                progress, totalTicks, current, snapshotTarget, currentEntryId, warpEntryId);
    }

    public static synchronized Vec3 getShipPosition(){return getShipUniversePosition().toLocalVec3();}
    public static synchronized UniversePosition getShipUniversePosition(){
        return !isWarping()||warpTarget==null?synchronizedUniversePosition
                :ShipFlightController.sampleUniversePosition(current,warpTarget,totalTicks,sampledTicks());
    }
    public static synchronized Vec3 getShipVelocity(){return velocity;}
    public static synchronized double getShipYaw(){return !isWarping()||warpTarget==null?yaw:ShipFlightController.sampleYaw(current,warpTarget,totalTicks,sampledTicks());}
    public static synchronized double getShipPitch(){return !isWarping()||warpTarget==null?pitch:ShipFlightController.samplePitch(current,warpTarget,totalTicks,sampledTicks());}
    public static synchronized double getShipRoll(){return !isWarping()||warpTarget==null?roll:ShipFlightController.sampleRoll(current,warpTarget,totalTicks,sampledTicks());}
    public static synchronized FlightPhase getFlightPhase(){return phase;} public static synchronized boolean isWarping(){return phase!=FlightPhase.DOCKED;}
    public static synchronized boolean consumeArrivalCue(){boolean c=arrivalCue;arrivalCue=false;return c;}
    public static synchronized float warpProgress(){return Mth.clamp((float)(sampledTicks()/totalTicks),0,1);} public static synchronized int getWarpDurationTicks(){return totalTicks;}
    public static synchronized double getShipX(){return getShipPosition().x;}public static synchronized double getShipY(){return getShipPosition().y;}public static synchronized double getShipZ(){return getShipPosition().z;}
    public static synchronized Planet getCurrent(){return current;}public static synchronized Planet getWarpTarget(){return warpTarget;}public static synchronized String getWarpEntryId(){return warpEntryId;}
    public static synchronized void setFuel(int f,int m){fuel=f;maxFuel=Math.max(1,m);}public static synchronized int getFuel(){return fuel;}public static synchronized int getMaxFuel(){return maxFuel;}
    public static synchronized void setStarState(List<String> v,String e){visited=List.copyOf(v);currentEntryId=e;}public static synchronized boolean isVisited(String e){return e!=null&&visited.contains(e);}public static synchronized String getCurrentEntryId(){return currentEntryId;}

    /** Immutable state used to render one frame without mixing network updates. */
    public record VisualSnapshot(Vec3 position, UniversePosition universePosition,
                                 Vec3 velocity, double yaw, double pitch, double roll,
                                 FlightPhase flightPhase, boolean warping,
                                 float warpProgress, int warpDurationTicks,
                                 Planet currentBody, Planet targetBody,
                                 String currentEntryId, String targetEntryId) {}

    /** Decode destination textures off-thread while the ship is still in transit. */
    private static void preloadPlanetSystem(Planet planet)
    {
        if (planet == null)
            return;
        var textures = Minecraft.getInstance().getTextureManager();
        textures.preload(planet.texture(), Util.backgroundExecutor());
        Planet companion = planet == Planet.LUSH ? Planet.MOLTEN
                : planet == Planet.MOLTEN ? Planet.LUSH : null;
        if (companion != null)
            textures.preload(companion.texture(), Util.backgroundExecutor());
    }
}
