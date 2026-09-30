#version 150
#moj_import <starboundmc:space_common.glsl>
#moj_import <starboundmc:space_background_field.glsl>
uniform float LinearColor;
uniform mat4 InverseViewProjection;
uniform vec3 TintColor;
uniform float TintAmount;
uniform float FieldDetail;
in vec2 screenPosition;
out vec4 fragColor;
void main() {
    vec4 ray = InverseViewProjection * vec4(screenPosition, 1.0, 1.0);
    vec3 color = spaceBackgroundRadiance(normalize(ray.xyz), TintColor, TintAmount, FieldDetail);
    if (LinearColor < .5) color = spaceToDisplay(color);
    fragColor = vec4(color, 1.0);
}
