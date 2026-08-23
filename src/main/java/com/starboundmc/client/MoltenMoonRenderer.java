package com.starboundmc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.starboundmc.StarboundMC;
import com.starboundmc.world.Planet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The molten planet is the lush planet's (the overworld's) moon: it appears in
 * the sky at a FIXED position opposite the sun (like the vanilla moon), with
 * the moon phase shown as the classic waxing/waning SHAPE — the lit side
 * rotates around the observer's line of sight as the lunar cycle advances
 * (full → gibbous → half → crescent → new → crescent → half → gibbous), the
 * same way the vanilla moon's phase texture works. The new moon is a dim
 * dark-blue disk, not invisible.
 *
 * <p>Note: because the moon stays opposite the sun, a shape-based phase means
 * the lit side cannot always point at the real sun (at full moon it does; at
 * half moon the terminator is rotated 90° around the line of sight).</p>
 *
 * <p>The vanilla moon itself is disabled by overriding its texture
 * (assets/minecraft/textures/environment/moon_phases.png) with a fully
 * transparent image.</p>
 *
 * <p>Uses the same celestial frame as vanilla's sun/moon rendering
 * ({@code Ry(-90) * Rx(timeOfDay*360)}), with the sun fixed at celestial +Y.</p>
 */
@Mod.EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MoltenMoonRenderer
{
    private static final float MOON_DISTANCE = 100.0F;
    /** Shrunk to 1/4 of the original 42-radius moon, then halved again (5.25). */
    private static final float MOON_SCALE = 5.25F / PlanetRenderer.PLANET_RADIUS;
    /** Fixed brightness: the phase shows as a SHAPE, not a brightness cycle. */
    private static final float BRIGHTNESS = 0.93F;
    private static final Vector3f[] PHASE_SUN_DIRECTIONS = new Vector3f[8];

    static
    {
        for (int phase = 0; phase < PHASE_SUN_DIRECTIONS.length; phase++)
        {
            double angle = phase * Math.PI / 4.0;
            PHASE_SUN_DIRECTIONS[phase] = new Vector3f((float) Math.cos(angle), 0.0F, (float) Math.sin(angle));
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel))
            return;
        ClientLevel level = (ClientLevel) mc.level;
        if (!level.dimension().equals(Level.OVERWORLD))
            return;

        float timeAngle = level.getTimeOfDay(event.getPartialTick()) * 360.0F;

        // Lunar phase: 0 = full moon, 4 = new moon. The moon stays fixed
        // opposite the sun (celestial -Y); the phase rotates the lit side
        // around the sphere-local Y axis (the observer's line of sight after
        // the ZP(90°) roll), so the terminator sweeps across the disk: full →
        // gibbous → half → crescent → new (dim dark disk) → crescent → …
        Vector3f sunLocal = PHASE_SUN_DIRECTIONS[level.getMoonPhase() & 7];

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(timeAngle));
        // Moon position in the celestial frame: fixed opposite the sun (+Y).
        pose.translate(0.0F, -MOON_DISTANCE, 0.0F);
        // Roll the sphere about its own center so its EQUATOR faces the viewer
        // instead of a pole: without this, the camera looks straight down the
        // sphere's pole axis and the equirectangular texture pinches in the
        // middle of the disk.
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        Matrix4f matrix = pose.last().pose();

        PlanetRenderer.drawPlanetSphere(pose, matrix, Planet.MOLTEN.texture(),
                0.0F, 0.0F, 0.0F, MOON_SCALE, sunLocal, BRIGHTNESS, 1.0F);

        pose.popPose();
    }
}
