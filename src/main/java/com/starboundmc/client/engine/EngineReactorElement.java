package com.starboundmc.client.engine;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.math.Axis;
import com.starboundmc.menu.ShipEngineMenu;
import com.starboundmc.story.ShipStoryService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Small, bounded vector scene: counter-rotating coils, inbound energy and a server-driven ignition ring. */
final class EngineReactorElement extends UIElement {
    private final ShipEngineMenu menu;
    EngineReactorElement(ShipEngineMenu menu) {
        this.menu = menu;
        setAllowHitTest(false);
    }

    @Override public void drawBackgroundAdditional(GUIContext context) {
        GuiGraphics g = context.graphics;
        var level = Minecraft.getInstance().level;
        float time = (level == null ? 0 : level.getGameTime() % 72000) + context.partialTick;
        boolean igniting = menu.status() == ShipEngineMenu.Status.IGNITING;
        boolean online = menu.status() == ShipEngineMenu.Status.ONLINE;
        float phase = time * (igniting ? 4 : online ? 1.2F : 0.25F);
        int color = online ? 0xff63e9b6 : igniting ? 0xff7eeaff : 0xffb28a51;
        g.pose().pushPose();
        g.pose().translate(getPositionX() + 46, getPositionY() + 46, 0);
        // Fixed graduations make the rotating coil readable without moving the item socket.
        for (int i = 0; i < 48; i++) spoke(g, i * 7.5F, 40, i % 4 == 0 ? 44 : 42, 0xff314c57);
        for (int i = 0; i < 72; i++) {
            if (i % 24 < 17) spoke(g, i * 5 + phase, 33, 35, color);
            if (i % 18 < 11) spoke(g, i * 5 - phase * 0.65F, 25, 26, 0xff416d7b);
        }
        if (igniting || online) {
            float progress = online ? 1 : Math.clamp(1F - menu.remainingTicks()
                    / (float) ShipStoryService.SUBLIGHT_IGNITION_TICKS, 0, 1);
            for (int i = 0; i < (int)(64 * progress); i++) spoke(g, i * 5.625F - 90, 37, 39, color);
            for (int arm = 0; arm < 4; arm++) {
                float radius = 30 - (time * 0.4F + arm * 4) % 15;
                spoke(g, arm * 90, radius, radius + 3, color);
            }
        }
        g.pose().popPose();
    }

    private static void spoke(GuiGraphics g, float degrees, float start, float end, int color) {
        g.pose().pushPose();
        g.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
        g.fill(Math.round(start), -1, Math.round(end), 1, color);
        g.pose().popPose();
    }
}
