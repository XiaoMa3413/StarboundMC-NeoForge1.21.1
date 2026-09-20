package com.starboundmc.client.ui.components;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The materials a recipe needs, as a scrollable list of {@link RequirementRow}s. There are no
 * per-row cards: each row draws its own separator rule, so the list reads as one table.
 *
 * <p>The list owns the row pool and the scrolling; the page only supplies data. Feeding it more
 * lines than fit scrolls rather than overflowing, so a page never has to decide how many
 * materials are worth showing. A title is optional — a pane tight for height drops it, because a
 * list of icons and counts under an {@link ItemHeader} is already self-explanatory.
 */
public final class MaterialRequirementView extends UIElement {
    /** One material line: the icon to draw, what to call it, and the progress toward its need. */
    public record Line(ItemStack icon, Component name, long current, long required, Component tooltip) {
    }

    private static final int HEADER_H = 10;
    private static final int GAP = 2;
    // Rows are inset so the vertical scroller never overlaps them.
    private static final int SCROLLER_GUTTER = 8;
    private static final int ROW_TRIM = 6;

    private final int rowWidth;
    private final ScrollerView list = new ScrollerView();
    private final List<RequirementRow> pool = new ArrayList<>();
    private final Label emptyHint = new Label();

    public MaterialRequirementView(Component title, int width) {
        rowWidth = Math.max(24, width - SCROLLER_GUTTER);
        addClass("sb-material-requirements");
        setOverflowVisible(false);
        layout(layout -> layout.width(width).flexDirection(FlexDirection.COLUMN).gapAll(GAP));

        if (title != null) {
            var header = new SectionHeader(title, width);
            header.layout(layout -> layout.width(width).height(HEADER_H));
            addChild(header);
        }

        list.addClass("sb-material-scroll");
        list.setOverflowVisible(false);
        // The scroller takes whatever height the section is given, so the page decides how many
        // materials are visible simply by how much room it hands over.
        list.layout(layout -> layout.width(rowWidth).flexGrow(1));
        list.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .minScrollPixel(6)
                .maxScrollPixel(14));
        list.viewPort(view -> view
                .layout(layout -> layout.paddingAll(0))
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY)));
        list.viewContainer(view -> view.layout(layout -> layout
                .widthPercent(100)
                // No flex gap: each row draws its own bottom rule, so the rule is the divider.
                .gapAll(0)
                .flexDirection(FlexDirection.COLUMN)));

        emptyHint.addClass("sb-material-empty");
        emptyHint.setAllowHitTest(false);
        emptyHint.layout(layout -> layout.widthPercent(100).height(RequirementRow.Mode.COMPACT.height()));
        emptyHint.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        list.addScrollViewChild(emptyHint);

        addChild(list);
    }

    /** Show exactly these materials; extra pooled rows are hidden, missing ones are created. */
    public MaterialRequirementView setRequirements(List<Line> lines) {
        List<Line> materials = lines == null ? List.of() : lines;
        emptyHint.setDisplay(materials.isEmpty());
        for (int index = 0; index < materials.size(); index++) {
            RequirementRow row = row(index);
            Line line = materials.get(index);
            row.setDisplay(true);
            row.setRequirement(line.icon(), line.name(), line.current(), line.required(), line.tooltip());
        }
        for (int index = materials.size(); index < pool.size(); index++) {
            pool.get(index).clearRequirement();
        }
        return this;
    }

    public MaterialRequirementView clearRequirements() {
        return setRequirements(List.of());
    }

    /** Text for the degenerate "this recipe needs nothing" case; defaults to a plain hint. */
    public MaterialRequirementView setEmptyHint(Component text) {
        emptyHint.setText(text);
        return this;
    }

    private RequirementRow row(int index) {
        while (pool.size() <= index) {
            var created = new RequirementRow(rowWidth - ROW_TRIM)
                    .setMode(RequirementRow.Mode.COMPACT);
            created.setDisplay(false);
            pool.add(created);
            list.addScrollViewChild(created);
        }
        return pool.get(index);
    }
}
