package com.starboundmc.client;

import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable content consumed by both the wide detail terminal and compact drawer. */
public record StarmapDetailContent(Component title, Component subtitle,
                                   Component description,
                                   List<StarmapDetailSection> sections)
{
    public StarmapDetailContent
    {
        title = Objects.requireNonNull(title, "title");
        subtitle = Objects.requireNonNull(subtitle, "subtitle");
        description = Objects.requireNonNull(description, "description");
        sections = List.copyOf(Objects.requireNonNull(sections, "sections"));
        Set<String> ids = new HashSet<>();
        for (StarmapDetailSection section : sections)
        {
            if (!ids.add(section.id()))
                throw new IllegalArgumentException("detail section ids must be unique");
        }
    }

}
