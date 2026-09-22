package com.starboundmc.client.ui.components;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * One material requirement expressed as a compact industrial data row instead of a card:
 * {@code [icon] Name                     current / required}. Hierarchy comes from a single
 * separator rule, never from a per-row panel background.
 */
public final class RequirementRow extends UIElement {
    /**
     * Row density. {@link #COMPACT} is the standard crafting row — a whole five-material recipe
     * fits a short list without scrolling. {@link #NORMAL} trades density for breathing room on
     * pages that are not height-critical. Both keep one column structure: icon, name, then the
     * {@code current / required} counter flush right.
     */
    public enum Mode {
        COMPACT(14),
        NORMAL(18);

        private final int height;

        Mode(int height) {
            this.height = height;
        }

        int height() {
            return height;
        }
    }

    private static final int ICON_SIZE = 12;
    private static final int NAME_LEFT = 16;

    private final int width;
    private final UIElement icon = new UIElement().addClass("sb-requirement-icon");
    private final Label name = new Label();
    private final Label amount = new Label();
    private final ItemStackTexture iconTexture = new ItemStackTexture();
    private Mode mode = Mode.COMPACT;

    public RequirementRow(int width) {
        this.width = width;
        addClass("sb-requirement-row");
        setOverflowVisible(false);

        icon.setAllowHitTest(false);
        icon.style(style -> style.backgroundTexture(iconTexture));

        name.addClass("sb-requirement-name");
        amount.addClass("sb-requirement-amount");
        configureLabel(name);
        configureLabel(amount);
        amount.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        addChildren(icon, name, amount);
        applyMode();
    }

    public RequirementRow setMode(Mode value) {
        this.mode = value == null ? Mode.COMPACT : value;
        return applyMode();
    }

    public RequirementRow setRequirement(ItemStack stack, Component displayName,
                                         long current, long required, Component tooltip) {
        iconTexture.setItems(stack == null ? ItemStack.EMPTY : stack.copyWithCount(1));
        name.setText(displayName);
        amount.setText(Component.literal(current + " / " + required));
        style(style -> style.tooltips(TooltipLines.split(tooltip)));
        removeClasses("sb-requirement-ok", "sb-requirement-missing");
        addClass(current >= required ? "sb-requirement-ok" : "sb-requirement-missing");
        return this;
    }

    public RequirementRow clearRequirement() {
        iconTexture.setItems(ItemStack.EMPTY);
        name.setText(Component.empty());
        amount.setText(Component.empty());
        removeClasses("sb-requirement-ok", "sb-requirement-missing");
        setDisplay(false);
        return this;
    }

    private RequirementRow applyMode() {
        int rowHeight = mode.height();
        int textHeight = rowHeight - 1;
        layout(layout -> layout.width(width).height(rowHeight));
        icon.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(1).top((rowHeight - ICON_SIZE) / 2).width(ICON_SIZE).height(ICON_SIZE));
        name.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(NAME_LEFT).top(0).width(Math.max(18, width - 61)).height(textHeight));
        amount.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(Math.max(36, width - 43)).top(0).width(42).height(textHeight));
        return this;
    }

    private static void configureLabel(Label label) {
        label.setAllowHitTest(false);
        label.setOverflowVisible(false);
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        int x = Math.round(getPositionX());
        int y = Math.round(getPositionY() + mode.height() - 1);
        context.graphics.fill(x + NAME_LEFT, y, x + width, y + 1, 0x292F4C55);
    }
}
