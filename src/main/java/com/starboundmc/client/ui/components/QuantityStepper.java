package com.starboundmc.client.ui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.function.IntConsumer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * One semantic quantity control composed from ordinary LDLib2 buttons/label.
 * The page owns the business limit; this component owns input ergonomics.
 *
 * <p>{@link Mode#FULL} offers the ±10 jumps as well; {@link Mode#COMPACT} keeps the −1/+1/MAX
 * controls and drops the tens, because a pane narrow enough to need it cannot afford to give the
 * action button next to it nothing but a clipped sliver. Either way the control stays compound.
 */
public final class QuantityStepper extends UIElement {
    public enum Mode {
        FULL,
        COMPACT
    }

    private final Button minusTen = button("−10", 20);
    private final Button minus = button("−", 14);
    private final Label value = new Label();
    private final Button plus = button("+", 14);
    private final Button plusTen = button("+10", 20);
    private final Button maximum = button("MAX", 28);
    private IntConsumer onChanged = ignored -> {};
    private int current = 1;
    private int max = 1;
    private Mode mode = Mode.FULL;

    public QuantityStepper(int width) {
        addClass("sb-quantity-stepper");
        layout(layout -> layout.width(width).height(13)
                .flexDirection(FlexDirection.ROW).gapAll(2));

        value.addClass("sb-quantity-value");
        value.setAllowHitTest(false);
        value.setOverflowVisible(false);
        value.layout(layout -> layout.width(28).height(13));
        value.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        wire(minusTen, -10);
        wire(minus, -1);
        wire(plus, 1);
        wire(plusTen, 10);
        maximum.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && maximum.isActive()) {
                onChanged.accept(max);
                event.stopPropagation();
            }
        });

        addChildren(minusTen, minus, value, plus, plusTen, maximum);
        setValueAndMaximum(1, 1);
    }

    public QuantityStepper onChanged(IntConsumer consumer) {
        onChanged = consumer == null ? ignored -> {} : consumer;
        return this;
    }

    /**
     * Pick the control density. The component sets its own width, since only it knows how many
     * buttons are on show — the page then gives the action button beside it the remaining room.
     */
    public QuantityStepper setMode(Mode value) {
        mode = value == null ? Mode.FULL : value;
        boolean full = mode == Mode.FULL;
        minusTen.setDisplay(full);
        plusTen.setDisplay(full);
        // 20+2+14+2+28+2+14+2+20+2+28 pressed together, minus the two 22px tens cells when compact.
        int width = full ? 134 : 90;
        layout(layout -> layout.width(width).height(13)
                .flexDirection(FlexDirection.ROW).gapAll(2));
        return this;
    }

    public QuantityStepper setValueAndMaximum(int current, int maximumValue) {
        max = Math.max(1, maximumValue);
        this.current = Math.max(1, Math.min(current, max));
        value.setText(Component.literal("×" + this.current));
        boolean canDecrease = this.current > 1;
        boolean canIncrease = this.current < max;
        minusTen.setActive(canDecrease);
        minus.setActive(canDecrease);
        plus.setActive(canIncrease);
        plusTen.setActive(canIncrease);
        maximum.setActive(canIncrease);
        applyTooltips(canDecrease, canIncrease);
        return this;
    }

    /**
     * The buttons carry their own explanation: a bare "−10"/"MAX" is not self-explanatory, and
     * at the ceiling the increase buttons must say which limit stopped them rather than going
     * silently inert.
     */
    private void applyTooltips(boolean canDecrease, boolean canIncrease) {
        Component decrease = Component.translatable("gui.starboundmc.voxel_printing.quantity.decrease");
        Component decreaseTen =
                Component.translatable("gui.starboundmc.voxel_printing.quantity.decrease_ten");
        Component increase = Component.translatable("gui.starboundmc.voxel_printing.quantity.increase");
        Component increaseTen =
                Component.translatable("gui.starboundmc.voxel_printing.quantity.increase_ten");
        Component limit = Component.translatable(
                "gui.starboundmc.voxel_printing.quantity.limit", max);
        minus.style(style -> style.tooltips(canDecrease ? decrease : limit));
        minusTen.style(style -> style.tooltips(canDecrease ? decreaseTen : limit));
        plus.style(style -> style.tooltips(canIncrease ? increase : limit));
        plusTen.style(style -> style.tooltips(canIncrease ? increaseTen : limit));
        maximum.style(style -> style.tooltips(Component.translatable(
                "gui.starboundmc.voxel_printing.quantity.maximum", max)));
    }

    private void wire(Button button, int delta) {
        button.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && button.isActive()) {
                int target = Math.max(1, Math.min(max, current + delta));
                onChanged.accept(target);
                event.stopPropagation();
            }
        });
    }

    private static Button button(String text, int width) {
        var button = new Button().setText(Component.literal(text));
        button.addClass("sb-stepper-button");
        button.text.setAllowHitTest(false);
        button.text.setOverflowVisible(false);
        button.text.layout(layout -> layout.widthPercent(100).heightPercent(100).marginHorizontal(0));
        button.layout(layout -> layout.width(width).height(13).paddingAll(1));
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        return button;
    }
}
