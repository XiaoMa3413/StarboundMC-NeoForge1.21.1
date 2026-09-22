package com.starboundmc.client.ui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import net.minecraft.network.chat.Component;

/**
 * The print-count line, which is also the queue representation: there is no separate queue readout.
 *
 * <p>Before printing it is the quantity control — how many the player is about to ask for. While the
 * machine is working it becomes the count that matters then: how many are still to come. The two
 * never appear at once, because a spinner the player cannot use during a print is just noise.
 */
public final class PrintCountBar extends UIElement {
    private static final int H = 13;

    private final QuantityStepper stepper;
    private final Label remaining = new Label();

    public PrintCountBar(int width, QuantityStepper.Mode mode) {
        addClass("sb-print-count-bar");
        setOverflowVisible(false);
        layout(layout -> layout.width(width).height(H));

        stepper = new QuantityStepper(0).setMode(mode);

        remaining.addClass("sb-print-count-remaining");
        remaining.setAllowHitTest(false);
        remaining.setOverflowVisible(false);
        remaining.layout(layout -> layout.widthPercent(100).height(H));
        remaining.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        remaining.setDisplay(false);

        addChildren(stepper, remaining);
    }

    /**
     * Show either the quantity control or the remaining count.
     *
     * @param printing   true while a craft is on the bed, which is when the stepper steps aside
     * @param remaining_ how many crafts are still due, including the one being printed
     */
    public PrintCountBar setPrinting(boolean printing, int remaining_) {
        remaining.setDisplay(printing);
        stepper.setDisplay(!printing);
        remaining.setText(Component.translatable(
                "gui.starboundmc.voxel_printing.print_count.remaining", remaining_));
        // The action button is disabled mid-print, so the reason belongs on this line instead.
        remaining.style(style -> style.tooltips(Component.translatable(
                "gui.starboundmc.voxel_printing.print_count.remaining_hint")));
        return this;
    }

    /** The quantity control. The page owns its business ceiling; this bar owns when it is shown. */
    public QuantityStepper quantityStepper() {
        return stepper;
    }
}
