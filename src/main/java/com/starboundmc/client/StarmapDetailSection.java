package com.starboundmc.client;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A real, non-empty category of detail information with a stable extension id. */
public record StarmapDetailSection(String id, Optional<Component> label,
                                   List<StarmapDetailLine> lines)
{
    public StarmapDetailSection
    {
        validateStableId(id);
        label = Objects.requireNonNull(label, "label");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.isEmpty())
            throw new IllegalArgumentException("detail sections must contain real information");
    }

    public static StarmapDetailSection labeled(String id, Component label,
                                               StarmapDetailLine... lines)
    {
        return new StarmapDetailSection(id, Optional.of(Objects.requireNonNull(label, "label")),
                List.of(lines));
    }

    public static StarmapDetailSection unlabeled(String id, StarmapDetailLine... lines)
    {
        return new StarmapDetailSection(id, Optional.empty(), List.of(lines));
    }

    private static void validateStableId(String id)
    {
        Objects.requireNonNull(id, "id");
        if (id.isEmpty() || !id.matches("[a-z0-9_.-]+"))
            throw new IllegalArgumentException("section id must be a stable lowercase id");
    }
}
