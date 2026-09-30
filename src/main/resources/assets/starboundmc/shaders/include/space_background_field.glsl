// Pure linear radiance by universe direction, reusable by future curved-ray samplers.
float spaceBackgroundHash(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}
float spaceBackgroundNoise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(spaceBackgroundHash(i), spaceBackgroundHash(i + vec3(1,0,0)), f.x),
                   mix(spaceBackgroundHash(i + vec3(0,1,0)), spaceBackgroundHash(i + vec3(1,1,0)), f.x), f.y),
               mix(mix(spaceBackgroundHash(i + vec3(0,0,1)), spaceBackgroundHash(i + vec3(1,0,1)), f.x),
                   mix(spaceBackgroundHash(i + vec3(0,1,1)), spaceBackgroundHash(i + vec3(1,1,1)), f.x), f.y), f.z);
}
float spaceBackgroundFractal(vec3 p) {
    return spaceBackgroundNoise(p) * .55 + spaceBackgroundNoise(p * 2.07 + 13.7) * .28
         + spaceBackgroundNoise(p * 4.31 + 31.1) * .12 + spaceBackgroundNoise(p * 9.13 + 7.3) * .05;
}
vec3 spaceBackgroundRadiance(vec3 direction, vec3 tintColor, float tintAmount, float detailLevel) {
    vec3 axis = normalize(vec3(0.24, 0.87, 0.43));
    float latitude = dot(direction, axis);
    float broad = spaceBackgroundNoise(direction * 3.1 + vec3(17.2, 4.1, 9.7));
    float structure = broad;
    if (detailLevel > .5)
        structure = spaceBackgroundFractal(direction * 36.0 + vec3(broad*3.0,19.1,7.5));
    float warpedLatitude = latitude + (broad-.5)*.025;
    float band = exp(-warpedLatitude*warpedLatitude*72.0);
    float thin = exp(-warpedLatitude*warpedLatitude*420.0);
    float coreDirection = pow(max(dot(direction,normalize(vec3(.87,-.24,0))),0.0),6.0);
    // Fractal opacity makes branching dust silhouettes rather than additive gray fog.
    float lane = exp(-pow(warpedLatitude-(structure-.5)*.025,2.0)*3200.0);
    float dust = smoothstep(.25,.68,structure)*lane;
    float transmission = exp(-dust*3.8);
    float knots = pow(max(structure-.38,0.0)*2.5,2.0);
    vec3 black = vec3(.00025,.00032,.00042);
    vec3 light = band*(.30+broad)*vec3(.010,.0105,.012);
    light += thin*(.25+knots)*mix(vec3(.029,.029,.032),vec3(.085,.070,.049),coreDirection);
    float grain = detailLevel > .5 ? spaceBackgroundFractal(direction*110.0+vec3(3.7,19.0,4.1)) : structure;
    light *= transmission * (.65 + grain*.75);
    // Unresolved stellar populations carry muted warm/cool colour, not luminous nebula paint.
    light += band*knots*mix(vec3(.010,.008,.007),vec3(.006,.008,.012),broad)*transmission;
    vec3 color = black + light;
    return mix(color, color * (0.75 + tintColor * 0.25), clamp(tintAmount, 0.0, 1.0));
}
