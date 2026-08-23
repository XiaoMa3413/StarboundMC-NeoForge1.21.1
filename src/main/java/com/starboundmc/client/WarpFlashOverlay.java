package com.starboundmc.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import com.starboundmc.client.space.SpaceRenderContext;
import com.starboundmc.client.space.SpaceRenderState;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;

/**
 * Short full-screen arrival flash. The hyperspace breakthrough is rendered as
 * a localized core bloom in the level renderer, so it does not wash out the
 * cockpit view.
 */
public class WarpFlashOverlay
{
    private static final long ARRIVAL_FLASH_DURATION_MS = 800L;
    private static final float ARRIVAL_FLASH_PEAK = 0.5F;

    private static boolean wasWarping = false;
    private static long arrivalFlashStartMillis = Long.MIN_VALUE;
    private static int arrivalFlashRgb = 0xFFFFFF;

    public static final IGuiOverlay FLASH = (gui, guiGraphics, partialTick, screenWidth, screenHeight) ->
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
        {
            wasWarping = false;
            return;
        }

        SpaceRenderContext space = SpaceRenderState.capture(mc.level.getGameTime() + partialTick);
        boolean warping = space.warping();
        long now = System.currentTimeMillis();
        if (warping && space.targetBody() != null)
        {
            StarSystem targetSystem = StarSystems.systemOfPlanet(space.targetBody());
            if (targetSystem != null)
                arrivalFlashRgb = targetSystem.getStellarVisual().getCoreColor() & 0xFFFFFF;
        }
        if (wasWarping && !warping)
            arrivalFlashStartMillis = now;
        wasWarping = warping;

        long arrivalElapsed = now - arrivalFlashStartMillis;
        if (arrivalElapsed >= 0 && arrivalElapsed < ARRIVAL_FLASH_DURATION_MS)
        {
            float alpha = ARRIVAL_FLASH_PEAK
                    * (1.0F - arrivalElapsed / (float) ARRIVAL_FLASH_DURATION_MS);
            int color = ((int) (alpha * 255.0F) << 24) | arrivalFlashRgb;
            guiGraphics.fill(0, 0, screenWidth, screenHeight, color);
        }
    };

    private WarpFlashOverlay()
    {
    }
}
