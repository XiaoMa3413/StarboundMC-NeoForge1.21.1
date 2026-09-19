// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.hud.provider.RelayPoiProvider;
import com.starboundmc.client.hud.provider.ShipBeaconProvider;
import com.starboundmc.client.hud.provider.TutorialTargetProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

/**
 * Flat world-locked AR pass. It knows only generic targets and never applies visor curvature or
 * optical drift.
 */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class ArWorldRenderer {
    public static final ArWorldRenderer INSTANCE = new ArWorldRenderer(new ArTargetCollector(List.of(
            new TutorialTargetProvider(), new ShipBeaconProvider(), new RelayPoiProvider())));

    private final ArTargetCollector collector;
    private final Matrix4f clip = new Matrix4f();
    private Vec3 eye = Vec3.ZERO;
    private boolean ready;
    private ClientLevel capturedLevel;

    ArWorldRenderer(ArTargetCollector collector) {
        this.collector = collector;
    }

    @SubscribeEvent
    public static void capture(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL)
            return;
        INSTANCE.captureFrame(event);
    }

    private void captureFrame(RenderLevelStageEvent event) {
        clip.set(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        eye = event.getCamera().getPosition();
        capturedLevel = Minecraft.getInstance().level;
        ready = true;
    }

    public void render(GuiGraphics graphics, float opacity, boolean evaNavigation) {
        var mc = Minecraft.getInstance();
        if (!ready || opacity <= .001F || mc.level != capturedLevel || mc.player == null)
            return;

        var context = new ArContext(mc.player.level().dimension(), mc.player.position(),
                evaNavigation);
        ArTargetProjection.Point previous = null;
        for (var target : collector.collect(context))
            previous = marker(graphics, target, previous, opacity);
    }

    public void reset() {
        ready = false;
        capturedLevel = null;
        eye = Vec3.ZERO;
    }

    private ArTargetProjection.Point marker(GuiGraphics graphics, ArTarget target,
                                            ArTargetProjection.Point previous, float opacity) {
        int color = Math.round(221 * opacity) << 24 | target.rgb();
        Vec3 delta = target.worldPosition().subtract(eye);
        var clipPosition = clip.transform(new Vector4f((float) delta.x, (float) delta.y,
                (float) delta.z, 1));
        var point = ArTargetProjection.project(clipPosition.x, clipPosition.y, clipPosition.w,
                graphics.guiWidth(), graphics.guiHeight());
        float x = point.x();
        float y = point.y();
        if (point.edge()) {
            double angle = Math.atan2(y - graphics.guiHeight() / 2F,
                    x - graphics.guiWidth() / 2F);
            line(graphics, x, y, x - (float) Math.cos(angle - .55) * 7.5F,
                    y - (float) Math.sin(angle - .55) * 7.5F, color);
            line(graphics, x, y, x - (float) Math.cos(angle + .55) * 7.5F,
                    y - (float) Math.sin(angle + .55) * 7.5F, color);
        } else {
            line(graphics, x, y - 6, x + 6, y, color);
            line(graphics, x + 6, y, x, y + 6, color);
            line(graphics, x, y + 6, x - 6, y, color);
            line(graphics, x - 6, y, x, y - 6, color);
        }

        Component text = target.label().copy();
        if (point.behind())
            text = text.copy().append(" · ").append(Component.translatable(
                    "hud.starboundmc.eva.behind"));
        var font = Minecraft.getInstance().font;
        float scale = Math.min(.68F,
                (graphics.guiWidth() - 20F) / Math.max(1, font.width(text)));
        float half = font.width(text) * scale / 2F;
        float labelX = Math.clamp(x, half + 8, graphics.guiWidth() - half - 8);
        float labelY = y + 10;
        if (previous != null && Math.abs(previous.x() - x) < 140
                && Math.abs(previous.y() - y) < 26)
            labelY += 14;
        labelY = Math.min(labelY, graphics.guiHeight() - 14);
        graphics.pose().pushPose();
        graphics.pose().translate(labelX, labelY, 0);
        graphics.pose().scale(scale, scale, 1);
        if (Math.round(208 * opacity) >= 4)
            graphics.drawCenteredString(font, text, 0, 0,
                    Math.round(208 * opacity) << 24 | target.rgb());
        graphics.pose().popPose();
        return point;
    }

    private static void line(GuiGraphics graphics, float x, float y,
                             float targetX, float targetY, int color) {
        float dx = targetX - x;
        float dy = targetY - y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < .001F)
            return;
        float nx = -dy / length * .55F;
        float ny = dx / length * .55F;
        var buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        var pose = graphics.pose().last().pose();
        buffer.addVertex(pose, x + nx, y + ny, 0).setColor(color);
        buffer.addVertex(pose, targetX + nx, targetY + ny, 0).setColor(color);
        buffer.addVertex(pose, targetX - nx, targetY - ny, 0).setColor(color);
        buffer.addVertex(pose, x - nx, y - ny, 0).setColor(color);
    }
}
