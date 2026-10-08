#version 150
#moj_import <starboundmc:space_common.glsl>
uniform sampler2D Sampler0;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    fragColor = vec4(spaceToLinear(texture(Sampler0, texCoord).rgb), 0.0);
}
