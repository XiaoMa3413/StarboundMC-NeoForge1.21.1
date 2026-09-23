#version 150

uniform vec4 ColorModulator;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    vec2 p = texCoord0 * 2.0 - 1.0;
    float radiusSquared = dot(p, p);
    float glow = pow(max(1.0 - radiusSquared, 0.0), 3.0);
    fragColor = vec4(vertexColor.rgb, vertexColor.a * glow) * ColorModulator;
}
