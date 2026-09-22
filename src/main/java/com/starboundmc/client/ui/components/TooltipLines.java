// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.ui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Convert embedded line breaks into tooltip entries without losing styled text spans. */
public final class TooltipLines {
    private TooltipLines() {}

    public static Component[] split(Component text) {
        if (text == null) return new Component[0];
        if (!text.getString().contains("\n")) return new Component[]{text};

        List<MutableComponent> lines = new ArrayList<>();
        lines.add(Component.empty());
        text.visit((style, content) -> {
            String[] parts = content.split("\\r?\\n", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) lines.add(Component.empty());
                if (!parts[i].isEmpty()) {
                    lines.getLast().append(Component.literal(parts[i]).setStyle(style));
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        return lines.toArray(Component[]::new);
    }
}
