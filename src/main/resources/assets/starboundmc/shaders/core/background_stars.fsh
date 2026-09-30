#version 150
#moj_import <starboundmc:space_common.glsl>
uniform float LinearColor;
in vec2 starUv;
in vec4 starColor;
out vec4 fragColor;
void main() {
    float radiusSquared = dot(starUv, starUv);
    float profile = 1.0 - smoothstep(0.0, 1.0, radiusSquared);
    fragColor = vec4(starColor.rgb, starColor.a * profile);
    if (LinearColor > .5) fragColor.rgb = spaceToLinear(starColor.rgb);
}
