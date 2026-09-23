package com.starboundmc.client;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the planet surface shader's resource layout. The shader is loaded by
 * id at runtime, so a renamed file or a missing uniform declaration would only
 * surface as an unlit or unmaterialed planet in the client; these checks keep
 * the resource files, the JSON declarations and the GLSL symbols in agreement.
 */
final class PlanetSurfaceShaderResourceTest {
    private static final Path SHADER_DIR =
            Path.of("src/main/resources/assets/starboundmc/shaders/core");

    private static final String[] SHADING_UNIFORMS = {
            "SunDirection", "NightFloor", "TerminatorWidth", "GlobalAlpha" };

    private static final String[] MATERIAL_UNIFORMS = {
            "Roughness", "SpecularStrength", "FresnelStrength",
            "OceanRoughness", "OceanSpecular", "EmissiveStrength" };

    private static final String[] CLOUD_UNIFORMS = {
            "CloudMode", "CloudCoverage", "CloudTime" };
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
    void shaderJsonDeclaresTheUniformsAndSamplersTheRendererUses() throws Exception {
        String json = Files.readString(SHADER_DIR.resolve("planet_surface.json"));
        // Program references without a namespace resolve to minecraft, so the
        // mod's own programs must carry the mod namespace.
        assertTrue(json.contains("\"vertex\": \"starboundmc:planet_surface\""), "vertex program reference");
        assertTrue(json.contains("\"fragment\": \"starboundmc:planet_surface\""), "fragment program reference");
        assertTrue(json.contains("\"Sampler0\""), "albedo sampler");
        assertTrue(json.contains("\"Sampler1\""), "emissive mask sampler");
        for (String uniform : new String[] { "ModelViewMat", "ProjMat" })
            assertTrue(json.contains("\"" + uniform + "\""), uniform + " must be declared for automatic binding");
        for (String uniform : SHADING_UNIFORMS)
            assertTrue(json.contains("\"" + uniform + "\""), uniform + " must be declared in the JSON");
        for (String uniform : MATERIAL_UNIFORMS)
            assertTrue(json.contains("\"" + uniform + "\""), uniform + " must be declared in the JSON");
        for (String uniform : CLOUD_UNIFORMS)
            assertTrue(json.contains("\"" + uniform + "\""), uniform + " must be declared in the JSON");
        assertTrue(json.contains("\"EmissiveColor\""), "emissive colour must be declared in the JSON");
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
        // The sun is transformed into view space in the vertex shader, so the
        // fragment stage receives it as a varying rather than a uniform.
        assertTrue(vertex.contains("uniform vec3 SunDirection;"), "mesh-local sun direction");
        assertTrue(vertex.contains("out vec3 sunDirectionView;"), "view-space sun output");
        assertTrue(fragment.contains("in vec3 sunDirectionView;"), "view-space sun input");
        // The normal must be view space too: mixing a mesh-local normal with a
        // view-space sun makes the terminator swing with the camera.
        assertTrue(vertex.contains("out vec3 viewNormal;"), "view-space normal output");
        assertTrue(fragment.contains("in vec3 viewNormal;"), "view-space normal input");
        assertTrue(fragment.contains("uniform sampler2D Sampler0;"), "albedo sampler");
        assertTrue(fragment.contains("uniform sampler2D Sampler1;"), "emissive mask sampler");
        for (String uniform : new String[] { "NightFloor", "TerminatorWidth", "GlobalAlpha" })
            assertTrue(fragment.contains("uniform float " + uniform + ";"), uniform + " declaration");
        for (String uniform : MATERIAL_UNIFORMS)
            assertTrue(fragment.contains("uniform float " + uniform + ";"), uniform + " declaration");
        assertTrue(fragment.contains("uniform vec3 EmissiveColor;"), "emissive colour declaration");
        // The procedural cloud layer shares this shader through CloudMode, so
        // its noise, its sphere-frame input and its branch are all pinned.
        assertTrue(fragment.contains("uniform float CloudMode;"), "cloud mode declaration");
        assertTrue(fragment.contains("uniform float CloudCoverage;"), "cloud coverage declaration");
        assertTrue(fragment.contains("uniform float CloudTime;"), "cloud time declaration");
        assertTrue(vertex.contains("out vec3 spherePosition;"), "mesh-local position output");
        assertTrue(fragment.contains("in vec3 spherePosition;"), "mesh-local position input");
        assertTrue(fragment.contains("float hash(vec3 p)"), "cloud noise hash");
        assertTrue(fragment.contains("normalize(spherePosition) * 6.5"),
                "the cloud noise frequency is calibrated to the measured coverage");
        assertTrue(fragment.contains("fragColor = vec4(light, alpha * GlobalAlpha);"),
                "the cloud branch must shade with the shared day/night light");
        // Water is derived from the albedo's blue dominance, and only bodies
        // that author an ocean highlight take part; both are pinned so the
        // mask cannot silently drift or leak onto non-ocean worlds.
        assertTrue(fragment.contains("smoothstep(0.02, 0.15, texel.b - texel.g)"),
                "water mask must come from the albedo's blue dominance");
        assertTrue(fragment.contains("step(0.001, OceanSpecular)"),
                "the water mask must be gated on the ocean highlight");
        // The view-independent sheen is what keeps the ocean reflective from
        // vantage points where the mirror glint falls outside the disc.
        assertTrue(fragment.contains("pow(max(sunDot, 0.0), 3.0)"),
                "the water sheen must face the sun, not the camera");
        // The deep-water darkening must be masked to water only; applied to
        // the whole disc it would dim every planet.
        assertTrue(fragment.contains("mix(1.0, 0.88, water)"),
                "the deep-water darkening must be gated on the water mask");
        // The smoothstep curve is the lit amount: inverting it, or gating the
        // highlights with its complement, swaps the lit and dark hemispheres,
        // so both the curve and the shade expression are pinned.
        assertTrue(fragment.contains("float litAmount = smoothstep(0.0, 1.0, (sunDot + TerminatorWidth)"),
                "lit amount must come straight from the smoothstep curve");
        assertTrue(fragment.contains("float shade = (1.0 - litAmount) * (1.0 - NightFloor);"),
                "shade must be the darkness amount, matching the CPU bake");
    }

    @Test
    void theEmissiveMaskTextureTheCatalogReferencesExists() throws Exception {
        String universe = Files.readString(
                Path.of("src/main/java/com/starboundmc/world/universe/BuiltInUniverse.java"));
        assertTrue(universe.contains("molten_emissive.png"),
                "BuiltInUniverse must reference the molten emissive mask");
        assertTrue(Files.exists(Path.of(
                        "src/main/resources/assets/starboundmc/textures/planet/molten_emissive.png")),
                "the molten emissive mask texture is missing");
    }
}
