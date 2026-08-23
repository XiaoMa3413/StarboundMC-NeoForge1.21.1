package com.starboundmc.client;

import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.Optional;

/** One semantic line in a star-map detail section. */
public record StarmapDetailLine(Component text, Tone tone, Optional<String> iconId)
{
    public StarmapDetailLine
    {
        text = Objects.requireNonNull(text, "text");
        tone = Objects.requireNonNull(tone, "tone");
        iconId = Objects.requireNonNull(iconId, "iconId");
        iconId.ifPresent(StarmapDetailLine::validateStableId);
    }

    public static StarmapDetailLine of(Component text, Tone tone)
    {
        return new StarmapDetailLine(text, tone, Optional.empty());
    }

    public static StarmapDetailLine withIcon(Component text, Tone tone, String iconId)
    {
        return new StarmapDetailLine(text, tone, Optional.of(iconId));
    }

    private static void validateStableId(String id)
    {
        if (id.isEmpty() || !id.matches("[a-z0-9_.-]+"))
            throw new IllegalArgumentException("iconId must be a stable lowercase id");
    }

    public enum Tone
    {
        PRIMARY,
        SECONDARY,
        BODY,
        CURRENT,
        VISITED,
        ATTENTION,
        DANGER,
        FUEL,
        DISABLED
    }
}
