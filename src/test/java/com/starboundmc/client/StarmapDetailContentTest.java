package com.starboundmc.client;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StarmapDetailContentTest
{
    @Test
    void contentAndSectionsDefensivelyCopyTheirLists()
    {
        List<StarmapDetailLine> lines = new ArrayList<>();
        lines.add(StarmapDetailLine.of(Component.literal("line"),
                StarmapDetailLine.Tone.BODY));
        StarmapDetailSection section = new StarmapDetailSection(
                "overview", Optional.empty(), lines);
        List<StarmapDetailSection> sections = new ArrayList<>();
        sections.add(section);
        StarmapDetailContent content = new StarmapDetailContent(
                Component.literal("title"), Component.literal("subtitle"),
                Component.literal("description"), sections);

        lines.clear();
        sections.clear();

        assertEquals(1, section.lines().size());
        assertEquals(1, content.sections().size());
        assertThrows(UnsupportedOperationException.class,
                () -> section.lines().add(StarmapDetailLine.of(
                        Component.literal("extra"), StarmapDetailLine.Tone.BODY)));
        assertThrows(UnsupportedOperationException.class,
                () -> content.sections().clear());
    }

    @Test
    void emptyOrUnstableSectionsAreRejected()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new StarmapDetailSection("empty", Optional.empty(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> StarmapDetailSection.unlabeled("Not Stable",
                        StarmapDetailLine.of(Component.literal("line"),
                                StarmapDetailLine.Tone.BODY)));
        assertThrows(IllegalArgumentException.class,
                () -> StarmapDetailLine.withIcon(Component.literal("line"),
                        StarmapDetailLine.Tone.BODY, "Bad Icon"));
    }

    @Test
    void duplicateCategoryIdsAreRejected()
    {
        StarmapDetailSection first = StarmapDetailSection.labeled("scan",
                Component.literal("A"), StarmapDetailLine.of(
                        Component.literal("one"), StarmapDetailLine.Tone.BODY));
        StarmapDetailSection duplicate = StarmapDetailSection.labeled("scan",
                Component.literal("B"), StarmapDetailLine.of(
                        Component.literal("two"), StarmapDetailLine.Tone.BODY));

        assertThrows(IllegalArgumentException.class, () -> new StarmapDetailContent(
                Component.literal("title"), Component.empty(), Component.empty(),
                List.of(first, duplicate)));
    }
}
