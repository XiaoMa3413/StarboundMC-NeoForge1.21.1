package com.starboundmc.client.ui.components;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * One catalogue entry: the item's icon with its name, and how much one craft yields beneath.
 *
 * <p>This is a browser row, not a button. It has no border and no panel of its own — the icon is the
 * anchor, the name is the label, and the yield is secondary. Selection reads as a subtle background
 * highlight plus the row's own accent rail, so a chosen row stays legible when the pointer moves
 * away without the row lighting up like a card.
 */
public final class RecipeListRow extends Button {
    private static final int H = 22;
    private static final int ICON = 16;
    private static final int ICON_LEFT = 4;
    private static final int TEXT_LEFT = 24;
    private static final int NAME_H = 11;
    private static final int YIELD_H = 9;

    private final UIElement rail = new UIElement().addClass("sb-recipe-rail");

    public RecipeListRow(ItemStack result, int outputCount, Runnable onSelect) {
        noText();
        addClass("sb-recipe-row");
        setOverflowVisible(false);
        layout(layout -> layout.widthPercent(100).height(H));

        rail.setAllowHitTest(false);
        rail.layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left(0).top(0).width(2).height(H));

        var icon = new UIElement().addClass("sb-recipe-icon");
        icon.setAllowHitTest(false);
        icon.layout(layout -> layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left(ICON_LEFT).top((H - ICON) / 2).width(ICON).height(ICON));
        icon.style(style -> style.backgroundTexture(new ItemStackTexture(result)));

        var text = new UIElement().addClass("sb-recipe-text");
        text.setAllowHitTest(false);
        text.setOverflowVisible(false);
        text.layout(layout -> layout
                .positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left(TEXT_LEFT).top(1).widthPercent(100)
                .heightPercent(100).paddingRight(4)
                .flexDirection(FlexDirection.COLUMN));

        var name = label(result.getHoverName(), "sb-recipe-name", NAME_H);
        var yield = label(Component.translatable(
                "gui.starboundmc.voxel_printing.recipe_row.yield", outputCount), "sb-recipe-amount",
                YIELD_H);
        text.addChildren(name, yield);

        addChildren(rail, icon, text);
        style(style -> style.tooltips(result.getHoverName()));
        addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isActive()) {
                onSelect.run();
                event.stopPropagation();
            }
        });
    }

    public RecipeListRow setSelectedState(boolean selected) {
        removeClass("sb-selected");
        if (selected) addClass("sb-selected");
        return this;
    }

    public RecipeListRow setCraftable(boolean craftable) {
        removeClasses("sb-craftable", "sb-unavailable");
        addClass(craftable ? "sb-craftable" : "sb-unavailable");
        return this;
    }

    private static Label label(Component text, String styleClass, int height) {
        Label label = new Label();
        label.setText(text);
        label.addClass(styleClass);
        label.setAllowHitTest(false);
        label.setOverflowVisible(false);
        label.layout(layout -> layout.widthPercent(100).height(height));
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        return label;
    }
}
