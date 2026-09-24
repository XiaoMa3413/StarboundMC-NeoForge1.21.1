package com.starboundmc.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.starboundmc.StarboundMC;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
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

        // Every ship-space pass consumes this complete, bobbing-free frame.
        // Minecraft applies walking bob to the projection matrix, so remove that
        // transform there while retaining the active FOV and restore it before
        // other AFTER_SKY listeners run. Also neutralize global ModelView because
        // BufferUploader applies it after vertices were transformed by skyPose.
        var modelViewStack = RenderSystem.getModelViewStack();
        Matrix4f originalProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f skyProjection = removeViewBobbing(event.getProjectionMatrix(), mc,
                event.getCamera());
        modelViewStack.pushMatrix();
        try
        {
            modelViewStack.identity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(skyProjection, VertexSorting.DISTANCE_TO_ORIGIN);

            PoseStack skyPose = coordinateFrame.stableCameraPose(event.getCamera());
            Matrix4f skyModelView = skyPose.last().pose();
            SpaceBackgroundRenderer.renderSpaceDome(skyPose);
            StarSystemResolver.ResolvedStarField stars = StarSystemResolver.resolve(space);
            SpaceBackgroundRenderer.renderStarField(skyPose, level, event.getCamera(), partialTick,
                    space, coordinateFrame, stars.environment());

            // Stellar points and discs precede planets so planetary discs occlude
            // aligned stars in the same way as the existing render path.
            renderSystemStars(skyPose, skyModelView, space, coordinateFrame, stars);
            PlanetRenderer.renderVisiblePlanets(skyPose, event.getCamera(), space,
                    coordinateFrame, stars);
            WarpRenderer.render(skyPose, event.getCamera(), partialTick, space);
        }
        finally
        {
            RenderSystem.setProjectionMatrix(originalProjection, VertexSorting.DISTANCE_TO_ORIGIN);
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /** Removes the exact vanilla walking-bob transform from the active projection. */
    private static Matrix4f removeViewBobbing(Matrix4f projection, Minecraft minecraft, Camera camera)
    {
        if (!minecraft.options.bobView().get()
                || !(minecraft.getCameraEntity() instanceof Player player))
            return new Matrix4f(projection);

        // GameRenderer.bobView mutates the projection as P * (T * Rz * Rx).
        // Right-multiply by the inverse walking transform to retain P (including
        // FOV) and the preceding hurt-camera transform without walking sway.
        float partialTick = camera.getPartialTickTime();
        float walkDelta = player.walkDist - player.walkDistO;
        float walkPhase = -(player.walkDist + walkDelta * partialTick);
        float bob = Mth.lerp(partialTick, player.oBob, player.bob);
        float sin = Mth.sin(walkPhase * (float) Math.PI);
        float cos = Mth.cos(walkPhase * (float) Math.PI);

        PoseStack walkBob = new PoseStack();
        walkBob.translate(sin * bob * 0.5F, -Math.abs(cos * bob), 0.0F);
        walkBob.mulPose(Axis.ZP.rotationDegrees(sin * bob * 3.0F));
        walkBob.mulPose(Axis.XP.rotationDegrees(
                Math.abs(Mth.cos(walkPhase * (float) Math.PI - 0.2F) * bob) * 5.0F));

        Matrix4f inverseWalkBob = new Matrix4f(walkBob.last().pose()).invert();
        return new Matrix4f(projection).mul(inverseWalkBob);
    }

    private static void renderSystemStars(PoseStack pose, Matrix4f skyModelView,
                                          SpaceRenderContext space,
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
                StellarRenderer.renderWithModelView(skyModelView, profile, view.x, view.y, view.z,
                        StellarRenderer.SHIP_SKY_DISTANCE, star.stellarBrightness() * simplifiedWeight,
                        space.animationTicks(), apparentScale, star.coronaDetail(),
                        star.effectDetail(), StellarLod.SIMPLIFIED);
            if (fullWeight > 0.002F)
                StellarRenderer.renderWithModelView(skyModelView, profile, view.x, view.y, view.z,
                        StellarRenderer.SHIP_SKY_DISTANCE, star.stellarBrightness() * fullWeight,
                        space.animationTicks(), apparentScale, star.coronaDetail(),
                        star.effectDetail(), StellarLod.FULL);
        }
    }
}
