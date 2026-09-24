package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.mojang.blaze3d.vertex.PoseStack;
import com.starboundmc.client.space.SpaceCoordinateFrame;
import com.starboundmc.client.space.SpaceRenderContext;
import com.starboundmc.client.space.SpaceRenderState;
import com.starboundmc.client.space.StarSystemResolver;
import com.starboundmc.client.space.StellarLod;
import com.starboundmc.world.ShipDimensions;
import com.starboundmc.world.starmap.StellarVisualProfile;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3d;

/** Coordinates the ship-space passes while keeping their drawing code separate. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class SpaceRenderer
{
    private SpaceRenderer()
    {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level instanceof ClientLevel level) || mc.player == null)
            return;
        if (!level.dimension().equals(ShipDimensions.SHIP_LEVEL))
            return;

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        SpaceRenderContext space = SpaceRenderState.capture(level.getGameTime() + partialTick);
        SpaceCoordinateFrame coordinateFrame = new SpaceCoordinateFrame(space);

        // AFTER_SKY carries view bobbing. Rebuild the camera-only pose so walking
        // around the bridge cannot drag distant bodies across the window.
        PoseStack skyPose = coordinateFrame.stableCameraPose(event.getCamera());

        SpaceBackgroundRenderer.renderSpaceDome(skyPose);
        StarSystemResolver.ResolvedStarField stars = StarSystemResolver.resolve(space);
        SpaceBackgroundRenderer.renderStarField(skyPose, level, event.getCamera(), partialTick,
                space, coordinateFrame, stars.environment());

        // Stellar points and discs precede planets so planetary discs occlude
        // aligned stars in the same way as the existing render path.
        renderSystemStars(skyPose, space, coordinateFrame, stars);
        PlanetRenderer.renderVisiblePlanets(skyPose, event.getCamera(), space,
                coordinateFrame, stars);
        WarpRenderer.render(skyPose, event.getCamera(), partialTick, space);
    }

    private static void renderSystemStars(PoseStack pose, SpaceRenderContext space,
                                          SpaceCoordinateFrame coordinateFrame,
                                          StarSystemResolver.ResolvedStarField stars)
    {
        // LOD-2 points are submitted first in one additive batch. Nearer
        // simplified/full discs render afterwards and can cover aligned points.
        StellarPointBatchRenderer.render(pose, stars, coordinateFrame);

        Vector3d view = new Vector3d();
        for (int i = 0; i < stars.count(); i++)
        {
            StarSystemResolver.VisibleStar star = stars.star(i);
            float simplifiedWeight = star.simplifiedLodWeight();
            float fullWeight = star.fullLodWeight();
            if (simplifiedWeight <= 0.002F && fullWeight <= 0.002F)
                continue;
            coordinateFrame.toViewRelative(star.relativeX(), star.relativeY(), star.relativeZ(), view);
            StellarVisualProfile profile = star.system().stellarVisual();
            float apparentScale = star.projectedRadius() / profile.getApparentRadius();
            if (simplifiedWeight > 0.002F)
                StellarRenderer.render(pose, profile, view.x, view.y, view.z,
                        StellarRenderer.SHIP_SKY_DISTANCE, star.stellarBrightness() * simplifiedWeight,
                        space.animationTicks(), apparentScale, star.coronaDetail(),
                        star.effectDetail(), StellarLod.SIMPLIFIED);
            if (fullWeight > 0.002F)
                StellarRenderer.render(pose, profile, view.x, view.y, view.z,
                        StellarRenderer.SHIP_SKY_DISTANCE, star.stellarBrightness() * fullWeight,
                        space.animationTicks(), apparentScale, star.coronaDetail(),
                        star.effectDetail(), StellarLod.FULL);
        }
    }
}
