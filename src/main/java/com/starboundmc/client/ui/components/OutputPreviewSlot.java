package com.starboundmc.client.ui.components;

import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The one place the machine's output is shown: what will be printed, how far along it is, and what
 * is ready to take. It sits on the vanilla output slot's own coordinate, so the completed item the
 * menu draws appears inside this frame instead of beside it — there is no second icon anywhere.
 *
 * <p>Three states, driven by {@link #setOutput}:
 * <ul>
 *   <li><b>Idle</b> — the target item, dimmed, reading as "this is what will be printed".</li>
 *   <li><b>Printing</b> — the dimmed item with the full-colour layer revealed from the top down, so
 *       the colour sweeping over it <em>is</em> the progress indicator. No separate progress bar.</li>
 *   <li><b>Finished</b> — nothing drawn: the real item is in the slot and shows through at full
 *       brightness, where the player clicks to take it.</li>
 * </ul>
 *
 * <p>The dimmed state is a low-alpha item under a dark neutral wash rather than a true
 * desaturation: LDLib2 2.2.36.a renders item sprites through the vanilla item renderer and exposes
 * no grayscale stage, so the ghost is faded and washed toward the frame tone instead. The result
 * reads as "not real yet" at a glance, which is the whole point — a preview that looks like a real
 * item invites the player to click a slot that has nothing to give.
 */
public final class OutputPreviewSlot extends UIElement {
    /** The socket surrounds the vanilla 16px item by one pixel on every side. */
    public static final int SIZE = 18;
    private static final int ITEM = 16;
    private static final int ITEM_INSET = 1;
    /** The preview item itself, kept faint: it must not read as something already in the slot. */
    private static final int PREVIEW_ALPHA = 0x59FFFFFF;
    /**
     * A dark neutral wash drawn over the preview. LDLib2 renders item sprites through the vanilla
     * item renderer and offers no grayscale stage, so the ghost is faded and washed toward the
     * frame's own dark tone instead — which is what pulls the colours out of it. The coloured
     * reveal is drawn after this, so the finished part stays at full saturation and reads as the
     * one thing on this slot that is actually real.
     */
    private static final int PREVIEW_WASH = 0x52060E12;

    private final ItemStackTexture base = new ItemStackTexture().setColor(PREVIEW_ALPHA);
    private final ItemStackTexture reveal = new ItemStackTexture();

    private int revealPercent;

    public OutputPreviewSlot() {
        addClasses("machine-slot-socket", "voxel-printing-output-socket", "sb-output-slot");
        setAllowHitTest(false);
        layout(layout -> layout.width(SIZE).height(SIZE));
    }

    /**
     * Point the slot at the selected item and say how far the machine is.
     *
     * @param target           the item this recipe produces; empty hides the preview entirely
     * @param progressPercent  0..100 of the craft currently on the bed
     * @param printing         whether a craft is running right now
     * @param occupied         whether the real output slot already holds an item; when it does, the
     *                         preview steps aside so only the real item is visible
     * @param tooltip          what to explain on hover
     */
    public OutputPreviewSlot setOutput(ItemStack target, int progressPercent, boolean printing,
                                       boolean occupied, Component tooltip) {
        ItemStack item = target == null ? ItemStack.EMPTY : target;
        boolean show = !occupied && !item.isEmpty();
        ItemStack icon = show ? item.copyWithCount(1) : ItemStack.EMPTY;
        base.setItems(icon);
        reveal.setItems(icon);
        revealPercent = show && printing ? Math.max(0, Math.min(100, progressPercent)) : 0;
        setDisplay(true);
        style(style -> style.tooltips(TooltipLines.split(tooltip)));
        return this;
    }

    /** Nothing selected: take the whole slot down, frame included. */
    public OutputPreviewSlot clear() {
        base.setItems(ItemStack.EMPTY);
        reveal.setItems(ItemStack.EMPTY);
        revealPercent = 0;
        setDisplay(false);
        return this;
    }

    /**
     * Draw the preview layers, both at the vanilla item's own 16px size and one pixel in, so they
     * line up exactly with the real item the menu draws in this same frame.
     *
     * <p>The reveal is clipped by hand here rather than handed to a child element, because the clip
     * has to wrap a single draw call — a child's background is painted by the framework, outside any
     * scissor this element could open around it. {@code enableScissor} takes (x, y, width, height).
     */
    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        if (base.items.length == 0 || base.items[0].isEmpty()) {
            return;
        }
        float x = getPositionX() + ITEM_INSET;
        float y = getPositionY() + ITEM_INSET;
        base.draw(context, x, y, ITEM, ITEM);
        // Wash the whole preview, then let the reveal paint over it: the not-yet-printed part stays
        // grey and faint, the printed part comes back at full colour.
        int left = Math.round(x);
        int top = Math.round(y);
        context.graphics.fill(left, top, left + ITEM, top + ITEM, PREVIEW_WASH);
        if (revealPercent <= 0) {
            return;
        }
        // Colour sweeps down from the top, so the clip grows downward as the craft progresses.
        int revealed = Math.max(1, Math.round(ITEM * (revealPercent / 100.0F)));
        context.enableScissor(x, y, ITEM, revealed);
        try {
            reveal.draw(context, x, y, ITEM, ITEM);
        } finally {
            context.disableScissor();
        }
    }
}
