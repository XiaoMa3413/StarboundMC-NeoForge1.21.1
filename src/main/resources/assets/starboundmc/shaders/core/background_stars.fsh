#version 150
#moj_import <starboundmc:space_common.glsl>
#moj_import <starboundmc:space_star_optics.glsl>
uniform float LinearColor;
uniform float OpticalZoom;
in vec2 pixelOffset;
flat in vec3 starRadiance;
flat in vec2 starOptics;
flat in float starVisibility;
out vec4 fragColor;
void main() {
    vec3 color=starRadiance*spaceStarPixelFlux(pixelOffset,starOptics.x,starOptics.y,OpticalZoom);
    if (LinearColor < .5) color=spaceToDisplay(color);
    fragColor=vec4(color,starVisibility);
}
