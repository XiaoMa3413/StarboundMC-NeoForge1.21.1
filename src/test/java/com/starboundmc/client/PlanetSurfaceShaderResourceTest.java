package com.starboundmc.client;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the planet surface shader's resource layout. The shader is loaded by
 * id at runtime, so a renamed file or a missing uniform declaration would only
 * surface as an unlit planet in the client; these checks keep the resource
 * files, the JSON declarations and the GLSL symbols in agreement.
 */
final class PlanetSurfaceShaderResourceTest {
    private static final Path SHADER_DIR =
            Path.of("src/main/resources/assets/starboundmc/shaders/core");

    private static final String[] CUSTOM_UNIFORMS = {
            "SunDirection", "NightFloor", "TerminatorWidth", "GlobalAlpha" };

    @Test
    void shaderAssetsExistWithTheExpectedLayout() {
        assertTrue(Files.exists(SHADER_DIR.resolve("planet_surface.json")),
                "planet_surface.json is missing from shaders/core");
        assertTrue(Files.exists(SHADER_DIR.resolve("planet_surface.vsh")),
                "planet_surface.vsh is missing from shaders/core");
        assertTrue(Files.exists(SHADER_DIR.resolve("planet_surface.fsh")),
                "planet_surface.fsh is missing from shaders/core");
    }

    @Test
    void shaderJsonDeclaresTheUniformsAndSamplerTheRendererUses() throws Exception {
        String json = Files.readString(SHADER_DIR.resolve("planet_surface.json"));
        // Program references without a namespace resolve to minecraft, so the
        // mod's own programs must carry the mod namespace.
        assertTrue(json.contains("\"vertex\": \"starboundmc:planet_surface\""), "vertex program reference");
        assertTrue(json.contains("\"fragment\": \"starboundmc:planet_surface\""), "fragment program reference");
        assertTrue(json.contains("\"Sampler0\""), "albedo sampler");
        for (String uniform : new String[] { "ModelViewMat", "ProjMat" })
            assertTrue(json.contains("\"" + uniform + "\""), uniform + " must be declared for automatic binding");
        for (String uniform : CUSTOM_UNIFORMS)
            assertTrue(json.contains("\"" + uniform + "\""), uniform + " must be declared in the JSON");
    }

    @Test
    void glslSourcesDeclareTheMatchingSymbols() throws Exception {
        String vertex = Files.readString(SHADER_DIR.resolve("planet_surface.vsh"));
        String fragment = Files.readString(SHADER_DIR.resolve("planet_surface.fsh"));
        assertTrue(vertex.contains("#version 150"), "vertex shader version");
        assertTrue(fragment.contains("#version 150"), "fragment shader version");
        assertTrue(vertex.contains("in vec3 Position;"), "position attribute");
        assertTrue(vertex.contains("in vec2 UV0;"), "uv attribute");
        assertTrue(vertex.contains("uniform mat4 ModelViewMat;"), "model-view matrix");
        assertTrue(vertex.contains("uniform mat4 ProjMat;"), "projection matrix");
        assertTrue(fragment.contains("uniform sampler2D Sampler0;"), "albedo sampler");
        assertTrue(fragment.contains("uniform vec3 SunDirection;"), "sun direction");
        for (String uniform : new String[] { "NightFloor", "TerminatorWidth", "GlobalAlpha" })
            assertTrue(fragment.contains("uniform float " + uniform + ";"), uniform + " declaration");
    }
}
