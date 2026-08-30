package com.starboundmc.client.ui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;

/**
 * Shared LDLib2 lock surface used while the ship core is offline or rebooting.
 * The overlay is deliberately the last child in a screen's tree so no map or
 * machine control can receive pointer events through it.
 */
public final class ShipSystemLockOverlay extends UIElement
{
    private final Label title = new Label();

    public ShipSystemLockOverlay()
    {
        addClass("ship-system-lock-overlay");
        layout(layout -> layout.widthPercent(100).heightPercent(100)
                .positionType(TaffyPosition.ABSOLUTE)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER));
        style(style -> style.zIndex(100)
                .backgroundTexture(new ColorRectTexture(0xA805090F)));
        stopInteractionEventsPropagation();

        UIElement panel = new UIElement().addClass("ship-system-lock-panel");
        panel.layout(layout -> layout.width(158).height(54)
                .paddingAll(8)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER)
                .flexDirection(FlexDirection.COLUMN));
        panel.style(style -> style.backgroundTexture(GuiTextureGroup.of(
                new ColorRectTexture(0xEF05070C),
                new ColorBorderTexture(1, 0xFFD18A91))));
        panel.stopInteractionEventsPropagation();

        title.addClass("ship-system-lock-title");
        title.setText(Component.translatable("gui.starboundmc.system.fatal_error"));
        title.layout(layout -> layout.widthPercent(100).height(18));
        title.textStyle(text -> text.fontSize(11).textColor(0xFFFFD5D8)
                .textShadow(true)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        title.setAllowHitTest(false);

        panel.addChild(title);
        addChild(panel);
        setDisplay(false);
    }

    public void setLocked(boolean locked)
    {
        if (isDisplayed() != locked)
            setDisplay(locked);
    }
}
