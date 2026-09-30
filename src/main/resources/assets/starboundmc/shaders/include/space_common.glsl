// Universe distance is radial and logarithmic. 1.0 is reserved for empty space.
float celestialDepth(float distance) {
    return min(log2(1.0 + max(distance, 0.0)) / 40.0, 0.999999);
}

vec3 spaceToLinear(vec3 color) {
    return mix(color / 12.92, pow((color + 0.055) / 1.055, vec3(2.4)),
            step(vec3(0.04045), color));
}

vec3 spaceToDisplay(vec3 color) {
    color = max(color, vec3(0.0));
    return mix(color * 12.92, 1.055 * pow(color, vec3(1.0 / 2.4)) - 0.055,
            step(vec3(0.0031308), color));
}
