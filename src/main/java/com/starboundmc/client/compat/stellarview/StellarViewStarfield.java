package com.starboundmc.client.compat.stellarview;

import com.mojang.logging.LogUtils;
import com.starboundmc.client.StarfieldClientConfig;
import com.starboundmc.client.space.SpaceRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.fml.ModList;
import org.joml.Matrix4f;
import org.slf4j.Logger;

/** Optional-mod boundary. Space renderers do not load Stellar View classes directly. */
public final class StellarViewStarfield
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "stellarview";
    private static boolean failedThisSession;

    private StellarViewStarfield() {}

    public static boolean render(ClientLevel level, Camera camera, float partialTick,
                                 Matrix4f modelView, Matrix4f projection,
                                 SpaceRenderContext space, float brightness)
    {
        if (failedThisSession || !StarfieldClientConfig.STELLAR_VIEW_BACKGROUND_STARS.get()
                || !ModList.get().isLoaded(MOD_ID))
            return false;

        try
        {
            return StellarViewBackend.render(level, camera, partialTick, modelView, projection, space, brightness);
        }
        catch (LinkageError | RuntimeException error)
        {
            failedThisSession = true;
            LOGGER.warn("Stellar View background stars failed; using StarboundMC stars for this session", error);
            return false;
        }
    }

    public static void resetSession()
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
        failedThisSession = false;
    }
}
