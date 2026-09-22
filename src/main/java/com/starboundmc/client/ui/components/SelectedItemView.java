package com.starboundmc.client.ui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The selected recipe as an item detail view, stacked in reading order: what the item is, what it
 * costs, how many, and the action that makes it.
 *
 * <pre>
 * SelectedItemView
 *   OutputPreviewSlot          the item, its progress and its pickup in one frame
 *   ItemHeader                 name and yield
 *   MaterialRequirementView    one compact row per material
 *   PrintCountBar + ActionButton   quantity (or remaining count) and the one action
 * </pre>
 *
 * <p>One flex column, in that order. The material view is the only flexible child, so the region's
 * leftover height is what decides how many material rows are visible rather than a height chosen
 * here.
 *
 * <p>The output slot is the region's one absolutely positioned element and also the one thing this
 * view draws no duplicate of: the vanilla slot it frames is where the finished item appears, so
 * putting a second icon anywhere else on this screen would show the same item twice.
 *
 * <p>The count line and the action share their row: the region is 174px wide and 120px tall, and a
 * row of their own would be taken out of the material list, which has to keep five rows visible.
 *
 * <p>Only two children are interactive and the page owns both: {@link #quantityStepper()} and
 * {@link #craftButton()}. Business limits, packets and state stay on the page.
 */
public final class SelectedItemView extends UIElement {
    private static final int PAD = 4;
    private static final int STACK_GAP = 2;
    private static final int ACTION_H = 13;
    private static final int ACTION_GAP = 4;
    // The vanilla output slot draws at the coordinate its menu slot dictates; the socket frames it
    // by one pixel. That is why the view itself carries no padding — the coordinate is measured
    // from the region's own edge, so the frame stays on the slot.
    private static final int SOCKET_LEFT = 4;
    private static final int SOCKET_TOP = 2;

    private final OutputPreviewSlot output;
    private final ItemHeader header;
    private final MaterialRequirementView materials;
    private final PrintCountBar countBar;
    private final Button craftButton = new Button();

    public SelectedItemView(int width, int height, QuantityStepper.Mode stepperMode) {
        int contentWidth = width - PAD * 2;
        addClass("sb-item-view");
        setOverflowVisible(false);
        layout(layout -> layout.width(width).height(height));

        output = new OutputPreviewSlot();
        output.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(SOCKET_LEFT).top(SOCKET_TOP));

        header = new ItemHeader(contentWidth, true);
        materials = new MaterialRequirementView(null, contentWidth);
        materials.setEmptyHint(
                Component.translatable("gui.starboundmc.voxel_printing.no_material"));
        countBar = new PrintCountBar(stepperWidth(stepperMode), stepperMode);
        configureCraftButton();

        // One row: the count line keeps the stepper's width and the button takes what is left, so
        // the button stays legible instead of being squeezed to a sliver.
        var actionRow = new UIElement().addClass("sb-craft-action-bar");
        actionRow.setOverflowVisible(false);
        actionRow.layout(layout -> layout.widthPercent(100).height(ACTION_H)
                .flexDirection(FlexDirection.ROW).gapAll(ACTION_GAP));
        actionRow.addChildren(countBar, craftButton);

        // Padding lives on this inner frame so the output frame above keeps its plain coordinate.
        // The column is identity, materials, count, action; the material view absorbs the slack.
        var frame = new UIElement().addClass("sb-item-view-content");
        frame.setOverflowVisible(false);
        frame.layout(layout -> layout.widthPercent(100).heightPercent(100)
                .paddingAll(PAD)
                .gapAll(STACK_GAP)
                .flexDirection(FlexDirection.COLUMN));
        frame.addChildren(header, materials, actionRow);

        addChildren(output, frame);
    }

    /** What is being made: its name, how many units one craft yields, and its description. */
    public SelectedItemView setItem(Component itemName, int outputCount, Component tooltip) {
        header.setItem(itemName, Component.translatable(
                "gui.starboundmc.voxel_printing.item_header.count", outputCount), tooltip);
        return this;
    }

    /**
     * Point the output frame at the selected item and tell it how far the machine is.
     *
     * @param target           the item this recipe produces
     * @param progressPercent  0..100 of the craft on the bed
     * @param printing         whether a craft is running
     * @param occupied         whether the real output slot already holds an item
     */
    public SelectedItemView setOutput(ItemStack target, int progressPercent, boolean printing,
                                     boolean occupied, Component tooltip) {
        output.setOutput(target, progressPercent, printing, occupied, tooltip);
        return this;
    }

    /** Nothing selected: the frame comes down rather than keeping a stale item on screen. */
    public SelectedItemView clearItem(Component placeholder) {
        output.clear();
        header.clear(placeholder);
        return this;
    }

    /** Show exactly these materials; the list scrolls if there are more than fit. */
    public SelectedItemView setRequirements(List<MaterialRequirementView.Line> lines) {
        materials.setRequirements(lines);
        return this;
    }

    public SelectedItemView clearRequirements() {
        materials.clearRequirements();
        return this;
    }

    /** Whether the count line shows the quantity control or the remaining count. */
    public SelectedItemView setPrinting(boolean printing, int remaining) {
        countBar.setPrinting(printing, remaining);
        return this;
    }

    /** The amount control. The page owns its business ceiling; this view owns its placement. */
    public QuantityStepper quantityStepper() {
        return countBar.quantityStepper();
    }

    /** The craft action. The page owns its state, text and tooltip. */
    public Button craftButton() {
        return craftButton;
    }

    private void configureCraftButton() {
        craftButton.addClass("sb-craft-button");
        craftButton.text.setAllowHitTest(false);
        craftButton.text.setOverflowVisible(false);
        craftButton.text.layout(layout -> layout
                .widthPercent(100).heightPercent(100).marginHorizontal(0));
        craftButton.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        craftButton.layout(layout -> layout.height(ACTION_H).flexGrow(1).paddingAll(1));
    }

    /** The width the count line holds: exactly what the stepper at this density needs. */
    private static int stepperWidth(QuantityStepper.Mode mode) {
        return mode == QuantityStepper.Mode.COMPACT ? 90 : 134;
    }
}
