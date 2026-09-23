package com.starboundmc.client;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the atmosphere shader's resource layout, the same way
 * {@link PlanetSurfaceShaderResourceTest} guards the planet surface shader: the
 * files load by id at runtime, so a rename or a missing uniform would only
 * surface as a flat glow shell in the client.
 */
final class AtmosphereShaderResourceTest {
    private static final Path SHADER_DIR =
            Path.of("src/main/resources/assets/starboundmc/shaders/core");

    @Test
    void shaderAssetsExistWithTheExpectedLayout() {
        assertTrue(Files.exists(SHADER_DIR.resolve("atmosphere.json")),
                "atmosphere.json is missing from shaders/core");
        assertTrue(Files.exists(SHADER_DIR.resolve("atmosphere.vsh")),
                "atmosphere.vsh is missing from shaders/core");
        assertTrue(Files.exists(SHADER_DIR.resolve("atmosphere.fsh")),
                "atmosphere.fsh is missing from shaders/core");
    }

    @Test
    void shaderJsonDeclaresTheUniformsAndProgramReferences() throws Exception {
        String json = Files.readString(SHADER_DIR.resolve("atmosphere.json"));
        // Program references without a namespace resolve to minecraft.
        assertTrue(json.contains("\"vertex\": \"starboundmc:atmosphere\""), "vertex program reference");
        assertTrue(json.contains("\"fragment\": \"starboundmc:atmosphere\""), "fragment program reference");
        for (String uniform : new String[] {
                "ModelViewMat", "ProjMat", "AtmosphereCenter", "PlanetRadius",
                "ShellRadius", "SunDirection", "AtmosphereColor", "Density" })
            assertTrue(json.contains("\"" + uniform + "\""), uniform + " must be declared in the JSON");
    }

    @Test
    void glslSourcesDeclareTheMatchingSymbols() throws Exception {
        String vertex = Files.readString(SHADER_DIR.resolve("atmosphere.vsh"));
        String fragment = Files.readString(SHADER_DIR.resolve("atmosphere.fsh"));
        assertTrue(vertex.contains("#version 150"), "vertex shader version");
        assertTrue(fragment.contains("#version 150"), "fragment shader version");
        assertTrue(vertex.contains("in vec3 Position;"), "position attribute");
        assertTrue(vertex.contains("out vec3 shellDirection;"), "shell direction output");
        assertTrue(vertex.contains("out vec3 sunDirectionView;"), "view-space sun output");
        assertTrue(fragment.contains("in vec3 shellDirection;"), "shell direction input");
        assertTrue(fragment.contains("in vec3 sunDirectionView;"), "view-space sun input");
        assertTrue(fragment.contains("uniform float Density;"), "density declaration");
        // The night side keeps a faint contour rather than going fully dark,
        // and the terminator gets a small lift; both are pinned here.
        assertTrue(fragment.contains("0.12 + 0.88 * smoothstep(-0.35, 0.25, sunDot)"),
                "the night-side contour floor must stay");
        assertTrue(fragment.contains("exp(-abs(sunDot) * 5.0) * 0.22"),
                "the terminator lift must stay");
    }
}
