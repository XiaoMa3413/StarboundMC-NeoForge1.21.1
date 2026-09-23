// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.hud.HudBootController;
import com.starboundmc.client.hud.animation.HudAnimationClock;
import com.starboundmc.client.hud.provider.RelayPoiProvider;
import com.starboundmc.client.hud.provider.ShipBeaconProvider;
import com.starboundmc.client.hud.provider.TutorialTargetProvider;
import com.starboundmc.epp.EvaMovement;
import com.starboundmc.epp.EvaState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

/** Independent flat HUD pass. Only providers decide which dimensions/activities supply targets. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class ArWorldRenderer {
    public static final ArWorldRenderer INSTANCE = new ArWorldRenderer(new ArTargetCollector(List.of(
            new TutorialTargetProvider(), new ShipBeaconProvider(), new RelayPoiProvider())));

    private final ArTargetCollector collector;
    private final ArVisualStateCache visuals = new ArVisualStateCache();
    private final ArMarkerRenderer artwork = new ArMarkerRenderer();
    private final HudAnimationClock clock = new HudAnimationClock();
    private final Matrix4f clip = new Matrix4f();
    private final Vector4f projected = new Vector4f();
    private Vec3 eye = Vec3.ZERO;
    private boolean ready;
    private boolean hasTargets;
    private ClientLevel capturedLevel;
    private Entity capturedCamera;

    ArWorldRenderer(ArTargetCollector collector) { this.collector = collector; }

    @SubscribeEvent
    public static void capture(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL)
            INSTANCE.captureFrame(event);
    }

    private void captureFrame(RenderLevelStageEvent event) {
        var level = Minecraft.getInstance().level;
        if (capturedLevel != level) reset();
        if (capturedCamera != event.getCamera().getEntity()) {
            clock.suspend();
            visuals.leaveView();
        }
        capturedCamera = event.getCamera().getEntity();
        clip.set(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        eye = event.getCamera().getPosition();
        capturedLevel = level;
        ready = true;
    }

    public void render(GuiGraphics graphics, DeltaTracker tracker) {
        var mc = Minecraft.getInstance();
        if (!ready || mc.level != capturedLevel || mc.player == null || !mc.player.isAlive()) {
            hasTargets = false;
            suspend();
            return;
        }
        if (mc.isPaused() || mc.screen != null || mc.options.hideGui
                || !mc.options.getCameraType().isFirstPerson()) {
            suspend();
            return;
        }
        var boot = HudBootController.INSTANCE;
        if (!boot.worldArActive()) {
            hasTargets = false;
            suspend();
            return;
        }
        double seconds = clock.advance(System.nanoTime(), true);
        visuals.beginFrame(seconds);
        artwork.beginFrame();
        var context = new ArContext(mc.level.dimension(), mc.player.position(),
                EvaMovement.mode(mc.player) != EvaState.NORMAL);
        var targets = collector.collect(context);
        hasTargets = !targets.isEmpty();
        var attentionOwner = ArVisualStateCache.attentionOwner(targets);
        for (var target : targets) {
            double dx = target.worldPosition().x - eye.x;
            double dy = target.worldPosition().y - eye.y;
            double dz = target.worldPosition().z - eye.z;
            clip.transform(projected.set((float) dx, (float) dy, (float) dz, 1));
            if (!Float.isFinite(projected.x) || !Float.isFinite(projected.y) || !Float.isFinite(projected.w)) continue;
            var point = ArTargetProjection.project(projected.x, projected.y, projected.w,
                    graphics.guiWidth(), graphics.guiHeight());
            var state = visuals.present(target, point.edge());
            boolean selected = target.id().equals(attentionOwner);
            artwork.draw(graphics, target, point, state, boot.visorOpacity(), selected);
        }
    }

    /** Used only to let the visor offer orientation for generic navigation; never gates the AR pass. */
    public boolean hasTargets() { return hasTargets; }
    public void suspend() { clock.suspend(); }

    public void reset() {
        ready = false;
        hasTargets = false;
        capturedLevel = null;
        capturedCamera = null;
        eye = Vec3.ZERO;
        visuals.clear();
        clock.suspend();
    }
}
