package com.starboundmc.client.epp;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.RelayClientState;
import com.starboundmc.encounter.RelayGeometry;
import com.starboundmc.world.ShipStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class ArNavigationHud {
    private static final Matrix4f clip = new Matrix4f();
    private static Vec3 eye = Vec3.ZERO;
    private static boolean ready;
    private static net.minecraft.client.multiplayer.ClientLevel capturedLevel;
    private ArNavigationHud() { }
    @SubscribeEvent public static void capture(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        clip.set(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        eye = event.getCamera().getPosition();
        capturedLevel = Minecraft.getInstance().level;
        ready = true;
    }
    static void reset() { ready = false; capturedLevel = null; }
    static void compass(GuiGraphics g) {
        var mc = Minecraft.getInstance();
        float heading = Mth.positiveModulo(mc.gameRenderer.getMainCamera().getYRot() + 180, 360);
        for (int degree = 0; degree < 360; degree += 10) {
            float offset = Mth.wrapDegrees(degree - heading);
            if (Math.abs(offset) > 80) continue;
            int x = Math.round(136 + offset * 1.5f);
            int alpha = Math.round(210 * (1 - Math.abs(offset) / 105));
            int color = alpha << 24 | 0x95E8E2;
            g.fill(x, 19, x + 1, degree % 30 == 0 ? 26 : 23, color);
            if (degree % 30 == 0) {
                String label = switch (degree) { case 0 -> "N"; case 90 -> "E"; case 180 -> "S"; case 270 -> "W"; default -> Integer.toString(degree); };
                g.drawCenteredString(mc.font, label, x, 7, color);
            }
        }
        g.fill(135, 26, 138, 30, 0xEE95E8E2);
        g.drawCenteredString(mc.font, Math.round(heading) % 360 + "°", 136, 34, 0xCC95E8E2);
    }
    static void targets(GuiGraphics g) {
        var mc = Minecraft.getInstance();
        if (!ready || mc.level != capturedLevel || mc.player == null) return;
        var ship = Vec3.atBottomCenterOf(ShipStructure.SHIP_TELEPORTER_POS).add(0, 1, 0);
        var first = marker(g, ship, "hud.starboundmc.eva.beacon", 0x95E8E2, null);
        var relay = RelayClientState.snapshot;
        if (RelayClientState.local() && relay != null)
            marker(g, RelayGeometry.center(relay.origin()), "hud.starboundmc.relay.beacon", 0xFFD17C, first);
    }
    private static ArTargetProjection.Point marker(GuiGraphics g, Vec3 target, String key, int rgb, ArTargetProjection.Point previous) {
        var mc = Minecraft.getInstance();
        Vec3 delta = target.subtract(eye);
        var v = clip.transform(new Vector4f((float)delta.x, (float)delta.y, (float)delta.z, 1));
        var p = ArTargetProjection.project(v.x, v.y, v.w, g.guiWidth(), g.guiHeight());
        float x = p.x(), y = p.y();
        if (p.edge()) {
            double angle = Math.atan2(y - g.guiHeight() / 2f, x - g.guiWidth() / 2f);
            line(g, x, y, x - (float)Math.cos(angle - .55) * 7.5f, y - (float)Math.sin(angle - .55) * 7.5f, rgb);
            line(g, x, y, x - (float)Math.cos(angle + .55) * 7.5f, y - (float)Math.sin(angle + .55) * 7.5f, rgb);
        } else {
            // Open diamond keeps the target itself visible through the marker.
            line(g, x, y - 6, x + 6, y, rgb); line(g, x + 6, y, x, y + 6, rgb);
            line(g, x, y + 6, x - 6, y, rgb); line(g, x - 6, y, x, y - 6, rgb);
        }
        var bearing = ShipBeaconBearing.from(mc.player.position(), target, 0);
        var text = Component.translatable(key, Math.round(bearing.distance()),
                (bearing.height() >= 0 ? "+" : "") + Math.round(bearing.height()));
        if (p.behind()) text.append(" · ").append(Component.translatable("hud.starboundmc.eva.behind"));
        float scale = Math.min(.68f, (g.guiWidth() - 20f) / Math.max(1, mc.font.width(text)));
        float half = mc.font.width(text) * scale / 2;
        float labelX = Math.clamp(x, half + 8, g.guiWidth() - half - 8);
        float labelY = y + 10;
        if (previous != null && Math.abs(previous.x() - x) < 140 && Math.abs(previous.y() - y) < 26) labelY += 14;
        labelY = Math.min(labelY, g.guiHeight() - 14);
        g.pose().pushPose(); g.pose().translate(labelX, labelY, 0); g.pose().scale(scale, scale, 1);
        g.drawCenteredString(mc.font, text, 0, 0, 0xD0000000 | rgb); g.pose().popPose();
        return p;
    }
    private static void line(GuiGraphics g, float x, float y, float tx, float ty, int rgb) {
        float dx = tx - x, dy = ty - y;
        float length = (float)Math.sqrt(dx * dx + dy * dy);
        if (length < .001) return;
        float nx = -dy / length * .55f, ny = dx / length * .55f;
        var b = g.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui());
        var pose = g.pose().last().pose(); int color = 0xDD000000 | rgb;
        b.addVertex(pose, x + nx, y + ny, 0).setColor(color);
        b.addVertex(pose, tx + nx, ty + ny, 0).setColor(color);
        b.addVertex(pose, tx - nx, ty - ny, 0).setColor(color);
        b.addVertex(pose, x - nx, y - ny, 0).setColor(color);
    }
}
