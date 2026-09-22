// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client;

import com.starboundmc.client.ui.components.TooltipLines;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TooltipLinesTest {
    @Test
    void separatesLocalizedRequirementLinesBeforeRendering() {
        for (String pattern : new String[]{"%1$s\n需要：%2$s\n拥有：%3$s",
                "%1$s\nRequired: %2$s\nAvailable: %3$s"}) {
            Component text = Component.translatableWithFallback(
                    "test.printer.requirement", pattern, "Voxel", 75, 3);
            Component[] lines = TooltipLines.split(text);
            assertEquals(3, lines.length);
            assertEquals("Voxel", lines[0].getString());
            assertTrue(lines[1].getString().endsWith("75"));
            assertTrue(lines[2].getString().endsWith("3"));
            assertTrue(Arrays.stream(lines).noneMatch(line -> line.getString().contains("\n")));
        }
    }

    @Test
    void preservesInheritedAndSiblingStylesAcrossBreaks() {
        Component text = Component.literal("Part\n")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal("Missing\nStill missing").withStyle(ChatFormatting.GOLD));
        Component[] lines = TooltipLines.split(text);
        assertArrayEquals(new String[]{"Part", "Missing", "Still missing"},
                Arrays.stream(lines).map(Component::getString).toArray(String[]::new));
        assertEquals(ChatFormatting.AQUA.getColor(), colorOf(lines[0]));
        assertEquals(ChatFormatting.GOLD.getColor(), colorOf(lines[1]));
        assertEquals(ChatFormatting.GOLD.getColor(), colorOf(lines[2]));
    }

    @Test
    void keepsBlankLinesAndDoesNotCopySingleLineComponents() {
        Component single = Component.literal("one");
        assertSame(single, TooltipLines.split(single)[0]);
        assertArrayEquals(new String[]{"one", "", "three", ""},
                Arrays.stream(TooltipLines.split(Component.literal("one\r\n\nthree\n")))
                        .map(Component::getString).toArray(String[]::new));
    }

    private static Integer colorOf(Component line) {
        return line.visit((style, text) -> text.isEmpty() ? Optional.<Integer>empty()
                : Optional.of(style.getColor().getValue()), Style.EMPTY).orElseThrow();
    }
}
