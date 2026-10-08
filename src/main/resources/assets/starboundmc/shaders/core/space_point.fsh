#version 150
#moj_import <starboundmc:space_common.glsl>
uniform float DistanceScale;
uniform float LinearColor;
uniform vec4 ColorModulator;
in vec4 vertexColor;
in vec3 viewPosition;
out vec4 fragColor;
void main() {
    fragColor = vertexColor * ColorModulator;
    if (LinearColor > .5) fragColor.rgb = spaceToLinear(fragColor.rgb);
    gl_FragDepth = celestialDepth(length(viewPosition) * DistanceScale);
}
