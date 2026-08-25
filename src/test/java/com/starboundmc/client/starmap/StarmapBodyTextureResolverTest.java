package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarSystems;
import com.starboundmc.world.starmap.StarmapBodyType;
import com.starboundmc.world.starmap.StarmapBodyVisual;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

final class StarmapBodyTextureResolverTest {
    private final StarmapBodyTextureResolver resolver = new StarmapBodyTextureResolver(
            StarmapBodyTextureResolverTest::resourceExists);

    @Test
    void resolvesFocusNormalAndFallbackPathsWithoutMissingTextures() {
        PlanetEntry barren = StarSystems.entryById("sys1:barren");
        PlanetEntry molten = StarSystems.entryById("sys1:molten");
        PlanetEntry gasGiant = StarSystems.entryById("sys1:gasgiant");

        assertEquals(ResourceLocation.parse(
                        "starboundmc:textures/gui/starmap/bodies/barren_focus.png"),
                resolver.resolve(barren.getVisual(), true));
        assertEquals(ResourceLocation.parse(
                        "starboundmc:textures/gui/starmap/bodies/molten.png"),
                resolver.resolve(molten.getVisual(), true));
        assertNull(resolver.resolve(gasGiant.getVisual(), true));
        GuiTextureGroup sprite = assertInstanceOf(GuiTextureGroup.class,
                resolver.texture(barren, 32.0F, true));
        GuiTextureGroup fallback = assertInstanceOf(GuiTextureGroup.class,
                resolver.texture(gasGiant, 16.0F, true));
        assertInstanceOf(SpriteTexture.class, sprite.getTextures()[0]);
        assertInstanceOf(SDFRectTexture.class, sprite.getTextures()[1]);
        assertInstanceOf(SDFRectTexture.class, fallback.getTextures()[0]);
        assertInstanceOf(SDFRectTexture.class, fallback.getTextures()[1]);
    }

    @Test
    void missingOrInvalidDeclaredResourcesResolveToFallback() {
        StarmapBodyVisual missing = StarmapBodyVisual.builder(
                        StarmapBodyType.ROCKY, 0xFF778899, 16, 1L)
                .texture("starboundmc:textures/gui/starmap/bodies/not_present.png")
                .focusTexture("not a resource id")
                .build();

        assertNull(resolver.resolve(missing, false));
        assertNull(resolver.resolve(missing, true));
    }

    @Test
    void allDeclaredSpritesAreSquareAndUseTheAuthoredSizes() throws Exception {
        for (var system : StarSystems.all()) {
            for (PlanetEntry entry : system.getEntries()) {
                assertDimensions(entry.getVisual().getTextureId(), 64);
                assertDimensions(entry.getVisual().getFocusTextureId(), 128);
            }
        }
    }

    private static void assertDimensions(String rawId, int expectedSize) throws Exception {
        if (rawId == null)
            return;
        ResourceLocation location = ResourceLocation.parse(rawId);
        var resource = StarmapBodyTextureResolverTest.class.getResource(resourcePath(location));
        assertNotNull(resource, "Missing declared body texture " + location);
        var image = ImageIO.read(resource);
        assertNotNull(image, "Unreadable body texture " + location);
        assertEquals(expectedSize, image.getWidth(), "Unexpected width for " + location);
        assertEquals(expectedSize, image.getHeight(), "Unexpected height for " + location);
    }

    private static boolean resourceExists(ResourceLocation location) {
        return StarmapBodyTextureResolverTest.class.getResource(resourcePath(location)) != null;
    }

    private static String resourcePath(ResourceLocation location) {
        return "/assets/" + location.getNamespace() + "/" + location.getPath();
    }
}
