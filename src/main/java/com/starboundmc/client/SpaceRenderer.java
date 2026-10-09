package com.starboundmc.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.SpaceCoordinateFrame;
import com.starboundmc.client.space.SpaceRenderContext;
import com.starboundmc.client.space.SpaceRenderState;
import com.starboundmc.client.space.SpaceRenderClock;
import com.starboundmc.client.space.StarSystemResolver;
import com.starboundmc.world.ShipDimensions;
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
        SpaceRenderProfiler.begin(SpaceRenderProfiler.Pass.TOTAL);

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        SpaceRenderContext space = SpaceRenderState.capture(
                SpaceRenderClock.animationTicks(level.getGameTime(), partialTick));
        SpaceCoordinateFrame coordinateFrame = new SpaceCoordinateFrame(space);
        SpaceRenderPassState.Snapshot incomingState = SpaceRenderPassState.capture();

        // Every ship-space pass consumes this complete, bobbing-free frame.
        // Minecraft applies walking bob to the projection matrix, so remove that
        // transform there while retaining the active FOV and restore it before
        // other AFTER_SKY listeners run. Also neutralize global ModelView because
        // BufferUploader applies it after vertices were transformed by skyPose.
        var modelViewStack = RenderSystem.getModelViewStack();
        Matrix4f originalProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting originalSorting = RenderSystem.getVertexSorting();
        Matrix4f skyProjection = removeViewBobbing(event.getProjectionMatrix(), mc,
                event.getCamera());
        modelViewStack.pushMatrix();
        boolean isolated = false;
        try
        {
            modelViewStack.identity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(skyProjection, VertexSorting.DISTANCE_TO_ORIGIN);

            PoseStack skyPose = coordinateFrame.stableCameraPose(event.getCamera());
            Matrix4f skyModelView = skyPose.last().pose();
            StarSystemResolver.ResolvedStarField stars = StarSystemResolver.resolve(space);
            isolated = SpaceSceneTarget.begin();
            SpaceRenderProfiler.begin(SpaceRenderProfiler.Pass.BACKGROUND);
            try { SpaceBackgroundRenderer.renderSpaceDome(skyPose, space, stars.environment()); }
            finally { SpaceRenderProfiler.end(SpaceRenderProfiler.Pass.BACKGROUND); }
            SpaceRenderProfiler.begin(SpaceRenderProfiler.Pass.STARFIELD);
            try {
                SpaceBackgroundRenderer.renderStarField(skyPose, level, event.getCamera(), partialTick,
                        space, coordinateFrame, stars.environment());
            } finally { SpaceRenderProfiler.end(SpaceRenderProfiler.Pass.STARFIELD); }

            // Isolated photospheres write universe distance; coronas resolve after opaque bodies.
            SpaceRenderProfiler.begin(SpaceRenderProfiler.Pass.SYSTEM_STARS);
            try {
                if (isolated) SpaceStellarRenderer.render(skyModelView, space, coordinateFrame, stars, false);
            }
            finally { SpaceRenderProfiler.end(SpaceRenderProfiler.Pass.SYSTEM_STARS); }
            SpaceRenderProfiler.begin(SpaceRenderProfiler.Pass.PLANETS);
            try {
                if (isolated) PlanetRenderer.renderVisiblePlanets(skyPose, event.getCamera(), space, coordinateFrame, stars);
            } finally { SpaceRenderProfiler.end(SpaceRenderProfiler.Pass.PLANETS); }
            if (isolated) {
                SpaceRenderProfiler.begin(SpaceRenderProfiler.Pass.CORONA);
                try { SpaceStellarRenderer.render(skyModelView, space, coordinateFrame, stars, true); }
                finally { SpaceRenderProfiler.end(SpaceRenderProfiler.Pass.CORONA); }
                SpaceRenderProfiler.begin(SpaceRenderProfiler.Pass.ATMOSPHERES);
                try { PlanetRenderer.renderTransparentPlanets(skyPose, event.getCamera(), space, coordinateFrame); }
                finally { SpaceRenderProfiler.end(SpaceRenderProfiler.Pass.ATMOSPHERES); }
                SpaceRenderProfiler.begin(SpaceRenderProfiler.Pass.COMPOSITE);
                try { SpaceSceneTarget.finish(); }
                finally { SpaceRenderProfiler.end(SpaceRenderProfiler.Pass.COMPOSITE); }
            }
            SpaceRenderProfiler.begin(SpaceRenderProfiler.Pass.WARP);
            try { WarpRenderer.render(skyPose, event.getCamera(), partialTick, space); }
            finally { SpaceRenderProfiler.end(SpaceRenderProfiler.Pass.WARP); }
        }
        catch (RuntimeException failure)
        {
            if (!isolated) throw failure;
            SpaceSceneTarget.disableAfterFailure(failure);
        }
        finally
        {
            SpaceSceneTarget.abort();
            RenderSystem.setProjectionMatrix(originalProjection, originalSorting);
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            incomingState.restore();
            SpaceRenderProfiler.end(SpaceRenderProfiler.Pass.TOTAL);
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

}
