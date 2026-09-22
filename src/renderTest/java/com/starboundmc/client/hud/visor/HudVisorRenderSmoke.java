// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.visor;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.mojang.blaze3d.systems.RenderSystem;
import com.starboundmc.StarboundMC;
import com.starboundmc.client.hud.animation.HudComponentPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.nio.file.Files;

/** Opt-in GPU smoke test: no world is opened; isolated client settings and captures stay in its run directory. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class HudVisorRenderSmoke {
    private static final HudVisorProjection COMPASS = new HudVisorProjection(272, 48, HudVisorGeometry.Profile.COMPASS);
    private static final HudVisorProjection SURVIVAL = new HudVisorProjection(128, 48, HudVisorGeometry.Profile.SURVIVAL);
    private static final HudVisorProjection CONTROLS = new HudVisorProjection(360, 48, HudVisorGeometry.Profile.EVA_CONTROLS);
    private static final HudVisorProjection FLAT = new HudVisorProjection(200, 48);
    private static int scenario;
    private static final HudComponentPresentation NAVIGATION =
            new HudComponentPresentation(HudComponentPresentation.Kind.NAVIGATION);
    private static int frames;
    private static boolean finished;

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Pre event) throws IOException {
        var mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("starboundmc.debug.hudVisorSmoke") || finished
                || mc.level != null || mc.getOverlay() != null || LDLibShaders.getGuiTexture() == null) return;
        event.setCanceled(true);
        int scale = 2 + scenario / 2;
        if (mc.options.guiScale().get() != scale) {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
            return;
        }
        var g = event.getGuiGraphics();
        int width = g.guiWidth(), height = g.guiHeight();
        boolean bright = scenario % 2 == 1;
        g.fill(0, 0, width, height, bright ? 0xFFCBD6D2 : 0xFF122532);
        g.flush();
        // Ignore earlier unrelated GL errors; any new error belongs to this rendering exercise.
        while (GL11.glGetError() != GL11.GL_NO_ERROR) { }
        int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        var previousShader = RenderSystem.getShader();
        float lowerY = HudVisorGeometry.survivalAnchor(width, height, 3, 2, SURVIVAL.profile()).y();
        HudVisorCalibrationGrid.renderGuides(g, 88, lowerY, COMPASS.profile(), SURVIVAL.profile());
        FLAT.draw(g, width - 205, 8, 200, 48, canvas -> {
            canvas.drawString(mc.font, "N.O.V.A. LINK  ONLINE", 8, 4, 0xECD8EDF0, true);
            canvas.drawString(mc.font, "系统连接完成", 8, 20, 0xEC95E8E2, true);
        });
        float navFit = Math.min(1, (width - 16F) / 272);
        NAVIGATION.update(1D / 60, true);
        COMPASS.draw(g, width / 2F - 136 * navFit, 64 + NAVIGATION.offsetY(), 272 * navFit, 48 * navFit,
                1, NAVIGATION.glow(), canvas -> VisorCompassRenderer.draw(canvas, NAVIGATION.progress()));
        float hintFit = Math.min(1, (width - 16F) / 360);
        CONTROLS.draw(g, width / 2F - 180 * hintFit, 113, 360 * hintFit, 48 * hintFit, canvas ->
                canvas.drawCenteredString(mc.font, "WASD 移动  SPACE 上升  SHIFT 下降", 180, 12, 0xEC95E8E2));
        for (int row = 0; row < 3; row++) {
            var anchor = HudVisorGeometry.survivalAnchor(width, height, 3, row, SURVIVAL.profile());
            int index = row;
            SURVIVAL.draw(g, anchor.x(), anchor.y(), 128, 48, canvas -> {
                int rgb = index == 0 ? 0xFF9477 : index == 1 ? 0xA7DFFF : 0xFFD17C;
                canvas.drawCenteredString(mc.font, index == 0 ? "O₂  12%" : index == 1 ? "寒冷暴露 18%" : "热暴露 18%",
                        64, 9, 0xEC000000 | rgb);
                canvas.fill(6, 24, 122, 25, 0x40000000 | rgb);
                canvas.fill(6, 24, index == 0 ? 20 : 70, 25, 0xBA000000 | rgb);
                canvas.drawCenteredString(mc.font, index == 0 ? "氧气不足" : index == 1 ? "低温环境" : "高温环境",
                        64, 33, 0xB8000000 | rgb);
            });
        }
        // A fading projection catches alpha discard and state leakage into subsequent vanilla text.
        FLAT.draw(g, 10, height - 48, 150, 36, .15F, canvas ->
                canvas.drawString(mc.font, "FADE 15%", 5, 5, 0xFF95E8E2, true));
        if (GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) != drawFbo
                || RenderSystem.getShader() != previousShader || GL11.glGetError() != GL11.GL_NO_ERROR)
            throw new IllegalStateException("HUD compositor leaked render state or produced an OpenGL error");
        g.drawString(mc.font, "HUD GPU SMOKE / GUI " + scale + (bright ? " / BRIGHT" : " / DARK"),
                8, height - 10, 0xFFFFFFFF, true);
        g.flush();
        ++frames;
        if (frames != 4 && frames != 12 && frames != 42) return;
        var folder = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(folder);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(folder.resolve("hud-visor-" + mc.getWindow().getWidth() + "x"
                    + mc.getWindow().getHeight() + "-" + scale
                    + (frames < 42 ? "-entry-" + frames : "") + (bright ? "-bright.png" : "-dark.png")));
        }
        if (frames < 42) return;
        frames = 0;
        NAVIGATION.update(0, false);
        // Exercise both viewport changes with existing meshes and explicit release/recreation.
        if (scenario == 2 || scenario == 5) {
            COMPASS.close(); SURVIVAL.close(); CONTROLS.close(); FLAT.close();
        }
        if (++scenario == 6) {
            finished = true;
            Files.writeString(folder.resolve("hud-visor-smoke-passed.txt"),
                    "PASS: GUI scales 2/3/4, dark/bright, Compass establishment/steady, GL state, buffer release/recreation.\n");
            mc.stop();
        }
    }
}
