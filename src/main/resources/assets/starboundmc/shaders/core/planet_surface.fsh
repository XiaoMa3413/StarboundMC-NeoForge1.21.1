#version 150

uniform sampler2D Sampler0;
// Emissive mask (lava veins and similar); multiplied by EmissiveStrength,
// which is zero for bodies that do not author a mask.
uniform sampler2D Sampler1;

uniform float NightFloor;
uniform float TerminatorWidth;
uniform float GlobalAlpha;
uniform float Roughness;
uniform float SpecularStrength;
uniform float FresnelStrength;
uniform float OceanRoughness;
uniform float OceanSpecular;
uniform float EmissiveStrength;
uniform vec3 EmissiveColor;

// Cloud layer: when CloudMode is 1 the albedo is procedural (no texture), the
// alpha comes from animated noise, and the colour is the day/night light. One
// shader shades both spheres so the terminator maths exists exactly once.
uniform float CloudMode;
uniform float CloudCoverage;
uniform float CloudTime;

in vec2 texCoord0;
in vec3 viewNormal;
in vec3 viewPosition;
in vec3 sunDirectionView;
in vec3 spherePosition;

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i + vec3(0.0, 0.0, 0.0)), hash(i + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(hash(i + vec3(0.0, 1.0, 0.0)), hash(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(hash(i + vec3(0.0, 0.0, 1.0)), hash(i + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(hash(i + vec3(0.0, 1.0, 1.0)), hash(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float total = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        total += noise(p) * amp;
        p *= 2.03;
        amp *= 0.5;
    }
    return total;
}

void main() {
    vec3 normal = normalize(viewNormal);
    vec3 sun = normalize(sunDirectionView);
    vec3 toCamera = normalize(-viewPosition);
    float sunDot = dot(normal, sun);

    // Terminator curve: 1 on the fully lit side, 0 on the dark side. Same
    // smoothstep the CPU bake used, evaluated per fragment so the boundary
    // stays smooth across the sphere. Both the curve and the warm lift below
    // divide by the width, so it is floored here once: a body that authors
    // zero would otherwise make the division produce NaN across the disc.
    // Old datapacks keep their authored value; the floor only touches zero.
    float terminatorWidth = max(TerminatorWidth, 0.0001);
    float litAmount = smoothstep(0.0, 1.0, (sunDot + terminatorWidth) / (terminatorWidth * 2.0));
    float shade = (1.0 - litAmount) * (1.0 - NightFloor);
    vec3 light = vec3(
        mix(1.0, 0.06, shade),
        mix(1.0, 0.08, shade),
        mix(1.0, 0.20, shade));

    // Warm lift right at the terminator, scaled down away from it.
    float terminator = max(0.0, 1.0 - abs(sunDot) / terminatorWidth);
    light.r += (0.90 - light.r) * terminator * 0.35;
    light.g += (0.55 - light.g) * terminator * 0.25;
    light.b += (0.25 - light.b) * terminator * 0.18;

    if (CloudMode > 0.5) {
        // Cloud density on the sphere's own frame, so the pattern rotates with
        // the mesh and stays seamless at the poles. The field scrolls over
        // time, so the weather evolves instead of merely revolving. A light
        // domain warp keeps the masses filamentary rather than blobby.
        vec3 p = normalize(spherePosition) * 6.5;
        vec3 warp = vec3(noise(p * 1.7 + 11.3), noise(p * 1.7 + 27.1), noise(p * 1.7 + 41.7));
        float density = fbm(p + warp * 0.55 + vec3(0.0, CloudTime * 0.012, CloudTime * 0.005));
        float alpha = smoothstep(CloudCoverage, CloudCoverage + 0.16, density);
        if (alpha < 0.02) {
            discard;
        }
        fragColor = vec4(light, alpha * GlobalAlpha);
        return;
    }

    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.1) {
        discard;
    }

    // Water is read from the albedo itself: blue-dominant pixels are water,
    // everything else is land. Deriving the mask in the shader keeps it
    // aligned with the texture at any resolution and needs no hand-drawn
    // mask that could drift. A body with no ocean highlight keeps one
    // uniform roughness across its whole surface.
    float water = smoothstep(0.02, 0.15, texel.b - texel.g) * step(0.001, OceanSpecular);
    float roughness = mix(Roughness, OceanRoughness, water);
    float specularStrength = mix(SpecularStrength, OceanSpecular, water);

    // Broad Blinn-Phong highlight: rough surfaces get a wide, dim one, glossy
    // ones a tighter, brighter one. Highlights belong to the lit hemisphere
    // only; the night side keeps its dark face instead of picking up glints.
    float specularPower = mix(48.0, 4.0, clamp(roughness, 0.0, 1.0));
    vec3 halfVector = normalize(sun + toCamera);
    float specular = pow(max(dot(normal, halfVector), 0.0), specularPower)
            * specularStrength * litAmount;

    // A broad, view-independent sheen on the water. The tight mirror glint
    // above only lands on the visible disc when the camera, sun and planet
    // line up, which at planet scale happens from few vantage points; this
    // keeps the ocean reading as reflective from any angle that sees the day
    // side, brightest around the sub-solar point. The sheen is an ocean
    // effect only: it is gated on the water mask and the ocean highlight, so
    // land never picks it up even on a body that authors a land specular.
    float waterSheen = pow(max(sunDot, 0.0), 3.0) * OceanSpecular * water * 0.22;

    // A gentle deep-water darkening: the ocean reads as deep azure rather
    // than a bright sheet, while land keeps its authored albedo untouched.
    vec3 albedo = texel.rgb * mix(1.0, 0.88, water);

    // Limb sheen, strongest at grazing view angles on the lit limb. The
    // exponent is deliberately shallow: a PBR-style pow(_, 5) falloff is
    // sub-pixel at planet scale, so the rim would never be visible.
    float fresnel = pow(1.0 - max(dot(normal, toCamera), 0.0), 0.5)
            * FresnelStrength * litAmount;

    // Emission rides on top of the day/night lighting, so masked lava keeps
    // glowing after the terminator has passed.
    vec3 emissive = texture(Sampler1, texCoord0).rgb * EmissiveColor * EmissiveStrength;

    vec3 color = albedo * light + vec3(specular + waterSheen) + light * fresnel + emissive;
    // GlobalAlpha rides the alpha channel alone, matching the cloud branch:
    // scaling RGB by it too would attenuate the colour twice under standard
    // blending, so a planet fading in reads as roughly alpha-squared dark.
    fragColor = vec4(color, texel.a * GlobalAlpha);
}
