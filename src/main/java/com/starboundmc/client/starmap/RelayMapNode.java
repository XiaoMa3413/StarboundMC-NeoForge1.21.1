// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** One hit target using exactly the same interpolated position as the station symbol. */
final class RelayMapNode extends UIElement {
    static final IGuiTexture ICON = (graphics, mouseX, mouseY, x, y, width, height, partialTicks) ->
            drawIcon(graphics, x + width / 2, y + height / 2, Math.min(width, height), 0xFFF0BC68);
    private final StarmapTerminalRoot root;

    RelayMapNode(StarmapTerminalRoot root) {
        this.root = root;
        layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .widthPercent(100).heightPercent(100));
        style(s -> s.zIndex(15).tooltips(Component.translatable("gui.starboundmc.relay.name")));
        addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button == 0) {
                root.selectRelay();
                event.stopPropagation();
            }
        });
    }

    private StarmapTerminalRoot.SelectedVisual placement() {
        return root.relayVisual(Math.max(1, Math.round(root.getSizeWidth())),
                Math.max(1, Math.round(root.getSizeHeight())), root.renderOrbitClock());
    }

    @Override public boolean isIntersectWithPoint(double x, double y) {
        var p = placement();
        return p != null && StarmapHitGeometry.contains(x, y,
                root.getPositionX() + p.x(), root.getPositionY() + p.y(), Math.max(9, p.diameter() * .65f));
    }

    @Override public void drawBackgroundAdditional(GUIContext context) {
        var p = placement();
        if (p == null) return;
        drawIcon(context.graphics, root.getPositionX() + p.x(), root.getPositionY() + p.y(),
                p.diameter(), root.isRelaySelected() ? 0xFFFFE0A4 : 0xFFF0BC68);
    }

    private static void drawIcon(GuiGraphics graphics, float x, float y, float size, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(size / 20, size / 20, 1);
        graphics.fill(-8, -1, 8, 1, color);
        graphics.fill(-3, -4, 3, 5, color);
        graphics.fill(-2, -3, 2, 4, 0xFF25343B);
        for (int side : new int[]{-9, 5}) {
            graphics.fill(side, -5, side + 4, 5, color);
            graphics.fill(side + 1, -4, side + 3, -1, 0xFF25343B);
            graphics.fill(side + 1, 1, side + 3, 4, 0xFF25343B);
        }
        graphics.fill(-1, -7, 1, -3, color);
        graphics.fill(-4, -8, 4, -7, color);
        graphics.pose().popPose();
    }
}
