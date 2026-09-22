package com.starboundmc.client.ui.components;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

/**
 * The catalogue's search line: a plain terminal-style input rather than a boxed web search widget —
 * no icon, no frame, the placeholder does the labelling.
 *
 * <p>It owns the query text and reports every edit; what the query <em>means</em> belongs to the
 * page, which is the only place that knows what is being listed. The length is capped because an
 * unbounded query is not a filter.
 */
public final class RecipeSearchField extends TextField {
    private static final int H = 14;
    /** Long enough for any item name a player would type, short enough to stay a filter. */
    private static final int MAX_QUERY = 32;

    private Consumer<String> onChanged = ignored -> {};
    private String query = "";

    public RecipeSearchField(int width) {
        addClass("sb-recipe-search");
        setAnyString();
        // LDLib2 2.2.36.a has no max-length setter, so the cap is a text validator (the same way the
        // teleporter's name field bounds its input).
        setTextValidator(text -> text == null || text.length() <= MAX_QUERY);
        layout(layout -> layout.width(width).height(H));
        textFieldStyle(style -> style
                .fontSize(6)
                .textShadow(false)
                .placeholder(Component.translatable(
                        "gui.starboundmc.voxel_printing.search.placeholder")));
        setTextResponder(text -> {
            query = text == null ? "" : text.trim();
            onChanged.accept(query);
        });
    }

    public RecipeSearchField onChanged(Consumer<String> listener) {
        onChanged = listener == null ? ignored -> {} : listener;
        return this;
    }

    /** The trimmed query, empty when the field is clear. */
    public String query() {
        return query;
    }

    /** Total height the search line occupies, so a layout can reserve room for it. */
    public static int height() {
        return H;
    }
}
