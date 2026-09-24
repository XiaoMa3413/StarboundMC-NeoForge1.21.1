#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    vec4 emission = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    // Preserve a dense hot core rather than fading the whole jet like smoke.
    emission.a = sqrt(max(emission.a, 0.0)) * vertexColor.a;
    fragColor = emission;
}
