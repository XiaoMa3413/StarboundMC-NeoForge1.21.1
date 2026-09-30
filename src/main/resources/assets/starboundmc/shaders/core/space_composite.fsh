#version 150
#moj_import <starboundmc:space_common.glsl>
uniform sampler2D Sampler0;
uniform float LinearColor;
uniform float Exposure;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec3 color = texture(Sampler0, texCoord).rgb;
    if (LinearColor > 0.5) {
        color *= Exposure;
        // Fixed white point: retain dark detail while rolling stellar energy into display range.
        color = color * (1.0 + color / 16.0) / (1.0 + color);
        color = spaceToDisplay(color);
    }
    fragColor = vec4(color, 1.0);
}
