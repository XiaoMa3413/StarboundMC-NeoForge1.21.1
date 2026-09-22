package com.starboundmc.client.ui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.function.IntConsumer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * One semantic quantity control: the −10/−1/+1/+10/MAX buttons with an editable count between them.
 * The page owns the business limit; this component owns input ergonomics.
 *
 * <p>The count is a real {@link TextField}, so a player who knows they want 32 can say so instead of
 * clicking up twenty-nine times. Typing is validated three ways: only digits reach the field, the
 * value must stay inside the machine's hard cap, and whatever is committed is clamped to the
 * business limit the page supplies — which is why a typed 9 with only 4 affordable settles on 4
 * rather than being refused.
 *
 * <p>{@link Mode#FULL} offers the ±10 jumps as well; {@link Mode#COMPACT} drops the tens, because a
 * pane narrow enough to need it cannot afford to give the action button nothing but a clipped
 * sliver. Either way the control stays compound.
 */
public final class QuantityStepper extends UIElement {
    public enum Mode {
        FULL,
        COMPACT
    }

    /** The machine's own ceiling (64 outstanding crafts); typing can never exceed it. */
    private static final int HARD_CAP = 64;
    private static final int FIELD_H = 13;
    private static final int VALUE_W = 28;
    private static final int GAP = 2;

    private final Button minusTen = button("−10", 20);
    private final Button minus = button("−", 14);
    private final TextField value = new TextField();
    private final Button plus = button("+", 14);
    private final Button plusTen = button("+10", 20);
    private final Button maximum = button("MAX", 28);
    private IntConsumer onChanged = ignored -> {};
    private int current = 1;
    private int max = 1;
    private Mode mode = Mode.FULL;
    /**
     * True while this component is writing the field itself. {@code TextField.setValue} notifies its
     * value listeners, so a programmatic write would re-enter the responder below, which would call
     * back into the page, which refreshes this control — a loop. Every self-write goes through
     * {@link #writeField} so the re-entry is refused.
     */
    private boolean writingSelf;

    public QuantityStepper(int width) {
        addClass("sb-quantity-stepper");
        layout(layout -> layout.width(width).height(FIELD_H)
                .flexDirection(FlexDirection.ROW).gapAll(GAP));

        value.addClass("sb-quantity-value");
        // Digits only, and inside the machine's hard cap. The business limit is applied on commit
        // instead, because rejecting a keystroke would fight a player mid-number.
        value.setNumbersOnlyInt(1, HARD_CAP);
        value.layout(layout -> layout.width(VALUE_W).height(FIELD_H));
        value.textFieldStyle(style -> style
                .fontSize(6)
                .textShadow(false)
                .placeholder(Component.literal("×1")));
        // Every edit reports upward so the materials list and the action button stay in step with
        // what has been typed; the field is left alone while focused (see setValueAndMaximum).
        value.setTextResponder(text -> {
            if (writingSelf) {
                return;
            }
            Integer typed = parse(text);
            if (typed != null) {
                current = Math.max(1, Math.min(typed, max));
                onChanged.accept(current);
            }
        });
        // Committing — Enter or leaving the field — snaps the text to the value actually allowed.
        value.addEventListener(UIEvents.KEY_DOWN, event -> {
            if (event.keyCode == GLFW.GLFW_KEY_ENTER || event.keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                normalizeField();
                event.stopPropagation();
            }
        });
        value.addEventListener(UIEvents.BLUR, event -> normalizeField());

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
        int width = full ? 134 : 90;
        layout(layout -> layout.width(width).height(FIELD_H)
                .flexDirection(FlexDirection.ROW).gapAll(GAP));
        return this;
    }

    public QuantityStepper setValueAndMaximum(int current, int maximumValue) {
        max = Math.max(1, maximumValue);
        this.current = Math.max(1, Math.min(current, max));
        // Never rewrite the field while the player is typing in it: the page refreshes this control
        // every tick, and a tick landing mid-number would replace what they were entering.
        if (!value.isFocused()) {
            writeField(this.current);
        }
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

    /** Replace the field text with the value that is actually allowed, once editing has stopped. */
    private void normalizeField() {
        if (!value.isFocused()) {
            writeField(current);
        }
    }

    /** Write the field without letting the write look like typing. */
    private void writeField(int amount) {
        writingSelf = true;
        try {
            value.setValue(Integer.toString(amount), false);
        } finally {
            writingSelf = false;
        }
    }

    private static Integer parse(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    /**
     * The buttons and the field carry their own explanation: a bare "−10"/"MAX" is not
     * self-explanatory, and at the ceiling the controls must say which limit stopped them rather
     * than going silently inert.
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
        // The count is typeable, which a field of digits does not advertise on its own.
        value.style(style -> style.tooltips(Component.translatable(
                "gui.starboundmc.voxel_printing.quantity.type", max)));
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
        button.layout(layout -> layout.width(width).height(FIELD_H).paddingAll(1));
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        return button;
    }
}
