package com.starboundmc.client.compat.stellarview;

import com.mojang.logging.LogUtils;
import com.starboundmc.client.StarfieldClientConfig;
import com.starboundmc.client.space.SpaceRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

/** Optional-mod boundary. Space renderers do not load Stellar View classes directly. */
public final class StellarViewStarfield
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "stellarview";
    private static boolean failedThisSession;
    private static boolean hasObservedEnabledSetting;
    private static boolean lastEnabledSetting;
    private static long linearDraws, displayDraws;

    private StellarViewStarfield() {}

    public static boolean render(ClientLevel level, Camera camera, float partialTick,
                                 Matrix4f modelView, Matrix4f projection,
                                 SpaceRenderContext space, float brightness,
                                 float convergence, Vector3f convergenceForward, boolean linear)
    {
        boolean enabled = StarfieldClientConfig.stellarViewStarsEnabled();
        if (hasObservedEnabledSetting && enabled != lastEnabledSetting)
            resetBackend();
        hasObservedEnabledSetting = true;
        lastEnabledSetting = enabled;

        if (failedThisSession || !enabled || !ModList.get().isLoaded(MOD_ID))
            return false;

        try
        {
            boolean rendered = StellarViewBackend.render(level, camera, partialTick, modelView, projection,
                    space, brightness, convergence, convergenceForward, linear);
            if (rendered) { if (linear) linearDraws++; else displayDraws++; }
            return rendered;
        }
        catch (LinkageError | RuntimeException error)
        {
            failedThisSession = true;
            LOGGER.warn("Stellar View background stars failed; using StarboundMC stars for this session", error);
            return false;
        }
    }

    public static boolean linearRadianceAvailable() {
        if (failedThisSession || !StarfieldClientConfig.stellarViewStarsEnabled()
                || !ModList.get().isLoaded(MOD_ID)) return false;
        try { return StellarViewBackend.supportsLinear(); }
        catch (LinkageError | RuntimeException unavailable) { return false; }
    }

    public static String diagnostics() {
        return "stellarViewLinearDraws=" + linearDraws + " stellarViewDisplayDraws=" + displayDraws
                + " stellarViewFailed=" + failedThisSession;
    }

    public static void resetSession()
    {
        resetBackend();
        hasObservedEnabledSetting = false;
        failedThisSession = false;
    }

    private static void resetBackend()
    {
        try
        {
            if (ModList.get().isLoaded(MOD_ID))
                StellarViewBackend.reset();
        }
        catch (LinkageError | RuntimeException error)
        {
            LOGGER.warn("Could not reset the Stellar View starfield cleanly", error);
        }
    }
}
