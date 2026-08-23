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
    private ClientPlanetState(){}
    /** Start a new network session so a restarted server may begin its snapshot revision at zero. */
    public static void resetConnectionState(){
        revision=-1;receivedNanos=System.nanoTime();arrivalCue=false;phase=FlightPhase.DOCKED;
        warpTarget=null;warpEntryId=null;elapsedTicks=0;totalTicks=1;velocity=Vec3.ZERO;
        snapDock(current);
    }
    public static void setCurrent(Planet p){if(phase!=FlightPhase.DOCKED&&p!=current)arrivalCue=true;current=p;if(phase==FlightPhase.DOCKED)snapDock(p);}
    public static void startWarp(Planet t,int duration,String entry){warpTarget=t;warpEntryId=entry;totalTicks=Math.max(1,duration);preloadPlanetSystem(t);}
    public static void applyFlightSnapshot(long rev,long serverTick,FlightPhase next,double x,double y,double z,double vx,double vy,double vz,float yv,float pv,float rv,int elapsed,int total,String entry){
        applyFlightSnapshot(rev,serverTick,next,UniversePosition.fromLegacy(new Vec3(x,y,z)),new UniverseDelta(vx,vy,vz),yv,pv,rv,elapsed,total,entry);
    }
    public static void applyFlightSnapshot(long rev,long serverTick,FlightPhase next,UniversePosition nextPosition,UniverseDelta nextVelocity,float yv,float pv,float rv,int elapsed,int total,String entry){
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
    private static double sampledTicks(){return Math.min(totalTicks,elapsedTicks+Math.min(.30,(System.nanoTime()-receivedNanos)/1e9)*20);}
    public static Vec3 getShipPosition(){return getShipUniversePosition().toLocalVec3();}
    public static UniversePosition getShipUniversePosition(){
        return !isWarping()||warpTarget==null?synchronizedUniversePosition
                :ShipFlightController.sampleUniversePosition(current,warpTarget,totalTicks,sampledTicks());
    }
    public static Vec3 getShipVelocity(){return velocity;}
    public static double getShipYaw(){return !isWarping()||warpTarget==null?yaw:ShipFlightController.sampleYaw(current,warpTarget,totalTicks,sampledTicks());}
    public static double getShipPitch(){return !isWarping()||warpTarget==null?pitch:ShipFlightController.samplePitch(current,warpTarget,totalTicks,sampledTicks());}
    public static double getShipRoll(){return !isWarping()||warpTarget==null?roll:ShipFlightController.sampleRoll(current,warpTarget,totalTicks,sampledTicks());}
    public static FlightPhase getFlightPhase(){return phase;} public static boolean isWarping(){return phase!=FlightPhase.DOCKED;}
    public static boolean consumeArrivalCue(){boolean c=arrivalCue;arrivalCue=false;return c;}
    public static float warpProgress(){return Mth.clamp((float)(sampledTicks()/totalTicks),0,1);} public static int getWarpDurationTicks(){return totalTicks;}
    public static double getShipX(){return getShipPosition().x;}public static double getShipY(){return getShipPosition().y;}public static double getShipZ(){return getShipPosition().z;}
    public static Planet getCurrent(){return current;}public static Planet getWarpTarget(){return warpTarget;}public static String getWarpEntryId(){return warpEntryId;}
    public static void setFuel(int f,int m){fuel=f;maxFuel=Math.max(1,m);}public static int getFuel(){return fuel;}public static int getMaxFuel(){return maxFuel;}
    public static void setStarState(List<String> v,String e){visited=List.copyOf(v);currentEntryId=e;}public static boolean isVisited(String e){return e!=null&&visited.contains(e);}public static String getCurrentEntryId(){return currentEntryId;}

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
