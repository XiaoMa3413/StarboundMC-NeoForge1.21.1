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
 * The catalogue of things the machine can make, as a scrollable list of {@link RecipeListRow}s.
 *
 * <p>What the browser does not own is which entries are relevant: the page decides that (a category
 * tag, a name search, a tier) and calls {@link #setVisible} with the answer, because only the page
 * knows the rules. The browser's job is to apply that answer without the list ever ending up scrolled
 * past its own content.
 *
 * <p>The rows themselves are a flat catalogue: no bordered cards, no per-row panels — selection is
 * a subtle highlight plus the row's own accent rail.
 */
public final class RecipeBrowser extends UIElement {
    /** One catalogue entry: which stack it shows, how many it yields, and what selecting it does. */
    public record Entry(ItemStack result, int outputCount, Runnable onSelect) {
    }

    private final ScrollerView list = new ScrollerView();
    private final List<RecipeListRow> rows = new ArrayList<>();
    private final Label empty;

    public RecipeBrowser(int width, int height) {
        addClass("sb-recipe-browser");
        setOverflowVisible(false);
        layout(layout -> layout.width(width).height(height));

        list.addClass("voxel-recipe-list");
        list.layout(layout -> layout.widthPercent(100).heightPercent(100));
        list.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .minScrollPixel(8)
                .maxScrollPixel(20));
        list.viewPort(view -> view
                .layout(layout -> layout.paddingAll(0))
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY)));
        list.viewContainer(view -> view.layout(layout -> layout
                .widthPercent(100)
                .gapAll(0)
                .flexDirection(FlexDirection.COLUMN)));

        empty = new Label();
        empty.addClass("voxel-recipe-empty");
        empty.setAllowHitTest(false);
        empty.layout(layout -> layout.widthPercent(100).height(28));
        empty.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.WRAP));
        list.addScrollViewChild(empty);

        addChild(list);
    }

    /** Replace the catalogue with these entries, in order, and drop the previous rows. */
    public RecipeBrowser setEntries(List<Entry> entries) {
        List<Entry> catalogue = entries == null ? List.of() : entries;
        for (RecipeListRow row : rows) {
            list.removeScrollViewChild(row);
        }
        rows.clear();
        for (Entry entry : catalogue) {
            var row = new RecipeListRow(entry.result(), entry.outputCount(), entry.onSelect());
            rows.add(row);
            list.addScrollViewChild(row);
        }
        empty.setDisplay(catalogue.isEmpty());
        return this;
    }

    /** The row for entry {@code index}, for the page to drive its selection and availability. */
    public RecipeListRow row(int index) {
        return index >= 0 && index < rows.size() ? rows.get(index) : null;
    }

    public int size() {
        return rows.size();
    }

    /**
     * Show only the entries the given predicate accepts. The page holds the filtering rules; this
     * just applies the answer and scrolls back to the top, so a re-filter never leaves the list
     * scrolled past its own content.
     */
    public int setVisible(java.util.function.IntPredicate visible) {
        int first = -1;
        for (int index = 0; index < rows.size(); index++) {
            boolean shown = visible.test(index);
            rows.get(index).setDisplay(shown);
            if (shown && first < 0) {
                first = index;
            }
        }
        empty.setDisplay(first < 0);
        list.verticalScroller.setNormalizedValue(0);
        return first;
    }

    /** Point the browser's own empty-state text at a hint. */
    public RecipeBrowser setEmptyHint(Component text) {
        empty.setText(text);
        return this;
    }

    /** The scroll view, for a page that needs to drive the scroll position itself. */
    public ScrollerView scroller() {
        return list;
    }
}
