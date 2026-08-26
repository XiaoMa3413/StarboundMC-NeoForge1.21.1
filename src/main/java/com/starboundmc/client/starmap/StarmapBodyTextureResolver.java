package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.mojang.logging.LogUtils;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarmapBodyVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Resolves optional body sprites once and guarantees an SDF fallback. */
final class StarmapBodyTextureResolver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_OUTLINE = 0xD06B94A4;

    private final Predicate<ResourceLocation> resourceExists;
    private final Map<String, Optional<ResourceLocation>> cache = new HashMap<>();
    private final boolean logFailures;

    static StarmapBodyTextureResolver clientResources() {
        return new StarmapBodyTextureResolver(location -> Minecraft.getInstance()
                .getResourceManager().getResource(location).isPresent(), true);
    }

    StarmapBodyTextureResolver(Predicate<ResourceLocation> resourceExists) {
        this(resourceExists, false);
    }

    private StarmapBodyTextureResolver(Predicate<ResourceLocation> resourceExists,
                                       boolean logFailures) {
        this.resourceExists = resourceExists;
        this.logFailures = logFailures;
    }

    ResourceLocation resolve(StarmapBodyVisual visual, boolean preferFocus) {
        if (visual == null)
            return null;
        if (preferFocus) {
            ResourceLocation focus = resolveId(visual.getFocusTextureId());
            if (focus != null)
                return focus;
        }
        return resolveId(visual.getTextureId());
    }

    IGuiTexture texture(PlanetEntry entry, float size, boolean preferFocus) {
        ResourceLocation resolved = entry == null ? null : resolve(entry.getVisual(), preferFocus);
        return texture(entry, size, resolved);
    }

    IGuiTexture texture(PlanetEntry entry, float size, ResourceLocation resolved) {
        if (entry == null)
            return IGuiTexture.EMPTY;
        StarmapBodyVisual visual = entry.getVisual();
        IGuiTexture body = resolved != null
                ? SpriteTexture.of(resolved)
                : SDFRectTexture.of(visual.getPrimaryColor())
                .setRadius(Math.max(2.0F, size * 0.5F))
                .setStroke(0.0F);
        int outlineColor = visual.hasAtmosphere()
                ? (0xD0000000 | (visual.getAtmosphereColor() & 0x00FFFFFF))
                : DEFAULT_OUTLINE;
        IGuiTexture outline = SDFRectTexture.of(0x00000000)
                .setRadius(Math.max(2.0F, size * 0.5F))
                .setBorderColor(outlineColor)
                .setStroke(Math.max(0.75F, Math.min(1.25F, size * 0.04F)));
        return GuiTextureGroup.of(body, outline);
    }

    private ResourceLocation resolveId(String rawId) {
        if (rawId == null)
            return null;
        return cache.computeIfAbsent(rawId, id -> {
            ResourceLocation location = ResourceLocation.tryParse(id);
            if (location == null) {
                if (logFailures)
                    LOGGER.warn("Ignoring invalid starmap body texture id: {}", id);
                return Optional.empty();
            }
            if (!resourceExists.test(location)) {
                if (logFailures)
                    LOGGER.warn("Starmap body texture {} is missing; using SDF fallback", location);
                return Optional.empty();
            }
            return Optional.of(location);
        }).orElse(null);
    }
}
