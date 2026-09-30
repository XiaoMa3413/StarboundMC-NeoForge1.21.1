#version 150
#moj_import <starboundmc:space_common.glsl>
uniform sampler2D Sampler0;
uniform float DistanceScale;
uniform float LinearColor;
uniform vec4 ColorModulator;
in vec2 texCoord;
in vec4 vertexColor;
in vec3 viewPosition;
out vec4 fragColor;
void main() {
    vec4 color = texture(Sampler0, texCoord) * vertexColor * ColorModulator;
    if (color.a < .01) discard;
    if (LinearColor > .5) color.rgb = spaceToLinear(color.rgb);
    fragColor = color;
    gl_FragDepth = celestialDepth(length(viewPosition) * DistanceScale);
}
