// Shared solar attenuation for ground, cloud tops and the scattering volume.
// Lengths use the caller's sphere frame; optical depths remain dimensionless.
const float SPACE_SOLAR_IRRADIANCE = 6.9115038; // diffuse irradiance / pi = 2.2
const float SPACE_SUN_ANGLE = .00465;
uniform sampler2D SolarOpticalDepth;
vec2 spaceAtmosphereHeights(float ground, float top) {
    return max(vec2(.00001), min(vec2(ground*.0016,ground*.00032),vec2((top-ground)*.2)));
}
vec2 spaceAtmosphereDensity(vec3 p, float ground, vec2 heights) {
    return exp(-max(0.0,length(p)-ground)/heights);
}
void spaceAtmosphereCoefficients(float ground, float top, vec3 tint, float strength,
                                  out vec3 betaR, out vec3 betaM) {
    vec2 heights = spaceAtmosphereHeights(ground,top);
    vec3 species = max(spaceToLinear(tint),vec3(.015));
    species /= max(species.r,max(species.g,species.b));
    float opticalScale = max(strength,0.0)/.2;
    betaR = mix(vec3(.175,.41,1.0),species,.35)*(.18*opticalScale/heights.x);
    betaM = vec3(.015*opticalScale/heights.y);
}
float spaceSunVisibility(vec3 p, vec3 sun, float ground) {
    float radius = max(length(p),ground);
    float sineHorizon = clamp(ground/radius,0.0,1.0);
    float horizon = -sqrt(max(0.0,1.0-sineHorizon*sineHorizon));
    float width = max(.00001,sineHorizon*SPACE_SUN_ANGLE);
    return smoothstep(-width,width,dot(p,sun)/radius-horizon);
}
// Quadratic spacing concentrates optical-depth samples around closest approach.
float spaceAtmosphereSample(float u, float begin, float end, float closest) {
    if (closest <= begin) return mix(begin,end,u*u);
    if (closest >= end) return mix(end,begin,(1.0-u)*(1.0-u));
    return u < .5 ? mix(closest,begin,(1.0-2.0*u)*(1.0-2.0*u))
                   : mix(closest,end,(2.0*u-1.0)*(2.0*u-1.0));
}
vec3 spaceSunTransmission(vec3 p, vec3 sun, float ground, float top,
                           vec3 betaR, vec3 betaM, int samples) {
    float visibility = spaceSunVisibility(p,sun,ground);
    if (visibility <= 0.0) return vec3(0);
    // A partially visible solar disk uses the tangent path, never a path through ground.
    float radius = max(length(p),ground);
    float mu = max(dot(p,sun)/radius,-sqrt(max(0.0,1.0-ground*ground/(radius*radius))));
    // Normalized scale heights share one table; exceptionally thin authored shells
    // retain numerical integration, because their density profile is compressed.
    if (top-ground >= ground*.008 && radius < ground*1.08) {
        float horizon = -sqrt(max(0.0,1.0-ground*ground/(radius*radius)));
        vec2 coordinate = vec2(sqrt(max(0.0,(mu-horizon)/(1.0-horizon))),
                sqrt(clamp((radius/ground-1.0)/.08,0.0,1.0)));
        vec2 optical = textureLod(SolarOpticalDepth,(coordinate*vec2(255,127)+.5)/vec2(256,128),0.0).rg*ground;
        return visibility*exp(-(betaR*optical.x+betaM*optical.y));
    }
    float b = radius*mu;
    float end = max(0.0,-b+sqrt(max(0.0,b*b+top*top-radius*radius)));
    float closest = clamp(-b,0.0,end);
    vec2 heights = spaceAtmosphereHeights(ground,top), optical = vec2(0);
    for (int j=0;j<8;j++) {
        if (j >= samples) break;
        float a = spaceAtmosphereSample(float(j)/float(samples),0.0,end,closest);
        float c = spaceAtmosphereSample(float(j+1)/float(samples),0.0,end,closest);
        float t = (a+c)*.5;
        float altitude = max(0.0,sqrt(max(ground*ground,radius*radius+2.0*b*t+t*t))-ground);
        optical += exp(-altitude/heights)*(c-a);
    }
    return visibility*exp(-(betaR*optical.x+betaM*optical.y));
}
vec3 spaceSurfaceSun(vec3 point, vec3 sun, float ground, float top, vec3 tint, float strength) {
    if (strength <= 0.0 || top <= ground) return vec3(1);
    vec3 betaR,betaM;
    spaceAtmosphereCoefficients(ground,top,tint,strength,betaR,betaM);
    return spaceSunTransmission(point,sun,ground,top,betaR,betaM,8);
}
