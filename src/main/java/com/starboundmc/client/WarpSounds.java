package com.starboundmc.client;

import com.starboundmc.warp.FlightPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Phase-driven client warp audio. The flight snapshot, not wall-clock progress, owns timing. */
public final class WarpSounds
{
    private static SoundInstance loopInstance;
    private static boolean endPlayed;
    private static FlightPhase lastPhase = FlightPhase.DOCKED;
    private WarpSounds() {}

    public static void onWarpStarted()
    {
        reset();
    }

    public static void reset()
    {
        stopLoop();
        endPlayed = false;
        lastPhase = FlightPhase.DOCKED;
    }

    public static void onWarpTick(double ignoredProgress)
    {
        FlightPhase phase = ClientPlanetState.getFlightPhase();
        if (phase == FlightPhase.HYPERSPACE && loopInstance == null) playLoop();
        if ((phase == FlightPhase.DECELERATE || phase == FlightPhase.ARRIVE) && lastPhase == FlightPhase.HYPERSPACE)
        {
            stopLoop();
            playEndOnce();
        }
        lastPhase = phase;
    }

    public static void onWarpFinished()
    {
        stopLoop();
        playEndOnce();
        lastPhase = FlightPhase.DOCKED;
    }

    public static boolean isLooping() { return loopInstance != null; }
    public static void stopLoop()
    {
        if (loopInstance != null)
        {
            Minecraft.getInstance().getSoundManager().stop(loopInstance);
            loopInstance = null;
        }
    }

    private static void playLoop()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        loopInstance = SimpleSoundInstance.forMusic(SoundEvents.PORTAL_AMBIENT);
        mc.getSoundManager().play(loopInstance);
    }

    private static void playEndOnce()
    {
        if (endPlayed) return;
        endPlayed = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) mc.getSoundManager().play(SimpleSoundInstance.forAmbientAddition(SoundEvents.PORTAL_TRAVEL));
    }
}
