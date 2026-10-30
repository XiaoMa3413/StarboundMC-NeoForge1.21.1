#version 150
// Derived from StellarView HDR2, Copyright (c) 2024 Povstalec, MIT; see third_party/stellarview/LICENSE.txt.
#moj_import <starboundmc:space_common.glsl>
uniform float LinearColor;
in vec2 externalUv;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    vec3 radiance = spaceToLinear(vertexColor.rgb)*(.7*6.4);
    float profile = max(0.0,(exp(-5.0*dot(externalUv,externalUv))-exp(-5.0))/(1.0-exp(-5.0)));
    if (LinearColor < .5) radiance = spaceToDisplay(radiance);
    fragColor = vec4(radiance,vertexColor.a*profile);
}
