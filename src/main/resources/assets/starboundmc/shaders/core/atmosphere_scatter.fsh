#version 150
#moj_import <starboundmc:space_common.glsl>
#moj_import <starboundmc:space_atmosphere.glsl>
uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform vec2 ViewportSize;
uniform vec3 SunDirection;
uniform vec3 CameraPositionMesh;
uniform vec3 AtmosphereColor;
uniform float AtmosphereStrength;
uniform float GlobalAlpha;
uniform float InnerRadius;
uniform float OuterRadius;
uniform float DistanceScale;
uniform float MeshToUniverse;
uniform int ViewSamples;
uniform int LightSamples;
in vec3 meshPosition;
out vec4 fragColor;
const float PI = 3.14159265;
vec2 sphere(vec3 origin, vec3 ray, float radius) {
    float b = dot(origin, ray);
    float d = b*b - dot(origin,origin) + radius*radius;
    if (d < 0.0) return vec2(1e20,-1e20);
    return vec2(-b-sqrt(d), -b+sqrt(d));
}
// The parallel sun's ground shadow is a cylinder clipped to the night hemisphere.
// Integrate its overlap with each view segment instead of testing a single sample;
// binary sample visibility produces visible concentric bands at the terminator.
vec2 shadowInterval(vec3 origin, vec3 ray, vec3 sun, float begin, float end) {
    float rs = dot(ray,sun), os = dot(origin,sun);
    float a = max(0.0,1.0-rs*rs);
    float b = dot(origin,ray)-os*rs;
    float c = dot(origin,origin)-os*os-InnerRadius*InnerRadius;
    vec2 interval = vec2(begin,end);
    if (a > .000001) {
        float d = b*b-a*c;
        if (d <= 0.0) return vec2(1e20,-1e20);
        interval = vec2(max(begin,(-b-sqrt(d))/a), min(end,(-b+sqrt(d))/a));
    } else if (c > 0.0) return vec2(1e20,-1e20);
    if (rs > .000001) interval.y = min(interval.y,-os/rs);
    else if (rs < -.000001) interval.x = max(interval.x,-os/rs);
    else if (os >= 0.0) return vec2(1e20,-1e20);
    return interval;
}
// Piecewise constant extinction has an exact integral and cannot overshoot energy.
void integrateSegment(float a, float b, vec3 ray, vec3 sun, vec2 heights,
                      vec3 betaR, vec3 betaM, float phaseR, float phaseM,
                      inout vec3 viewTransmission, inout vec3 scattering) {
    if (b <= a) return;
    vec3 start = CameraPositionMesh+ray*a, finish = CameraPositionMesh+ray*b;
    vec3 p = (start+finish)*.5;
    vec2 rho = (spaceAtmosphereDensity(start,InnerRadius,heights)
              +4.0*spaceAtmosphereDensity(p,InnerRadius,heights)
              +spaceAtmosphereDensity(finish,InnerRadius,heights))/6.0;
    vec3 extinction = betaR*rho.x+betaM*rho.y;
    vec3 segmentTransmission = exp(-extinction*(b-a));
    float va = spaceSunVisibility(start,sun,InnerRadius);
    float vm = spaceSunVisibility(p,sun,InnerRadius);
    float vb = spaceSunVisibility(finish,sun,InnerRadius);
    float visibility = (va+4.0*vm+vb)/6.0;
    if (visibility > .000001) {
        if (vm < .000001) p = va > vb ? start : finish;
        vec3 sunlight = spaceSunTransmission(p,sun,InnerRadius,OuterRadius,betaR,betaM,LightSamples)
                      * (visibility/max(spaceSunVisibility(p,sun,InnerRadius),.000001));
        vec3 source = betaR*rho.x*phaseR+betaM*rho.y*.9*phaseM;
        scattering += viewTransmission*sunlight*source*(1.0-segmentTransmission)/max(extinction,vec3(.000001));
    }
    viewTransmission *= segmentTransmission;
}
void main() {
    vec2 uv = gl_FragCoord.xy / ViewportSize;
    vec3 background = texture(SceneColor,uv).rgb;
    vec3 ray = normalize(meshPosition - CameraPositionMesh);
    vec3 sun = normalize(SunDirection);
    vec2 interval = sphere(CameraPositionMesh,ray,OuterRadius);
    float begin = max(0.0,interval.x), end = interval.y;
    float depth = texture(SceneDepth,uv).r;
    if (depth < .999999) {
        float distance = exp2(depth*40.0)-1.0;
        end = min(end, distance / max(MeshToUniverse*DistanceScale, .000001));
    }
    vec2 ground = sphere(CameraPositionMesh,ray,InnerRadius);
    if (ground.x > 0.0 && ground.y > ground.x) end = min(end,ground.x);
    if (end <= begin) discard;
    vec2 heights = spaceAtmosphereHeights(InnerRadius,OuterRadius);
    vec3 betaR,betaM;
    spaceAtmosphereCoefficients(InnerRadius,OuterRadius,AtmosphereColor,AtmosphereStrength,betaR,betaM);
    float cosine = dot(ray,sun), g = .76;
    float phaseR = 3.0/(16.0*PI)*(1.0+cosine*cosine);
    float phaseM = (1.0-g*g)/(4.0*PI*pow(1.0+g*g-2.0*g*cosine,1.5));
    float closest = clamp(-dot(CameraPositionMesh,ray),begin,end);
    vec2 shadow = shadowInterval(CameraPositionMesh,ray,sun,begin,end);
    vec3 transmission = vec3(1), scattering = vec3(0);
    for (int i=0; i<28; i++) {
        if (i >= ViewSamples) break;
        float a = spaceAtmosphereSample(float(i)/float(ViewSamples),begin,end,closest);
        float b = spaceAtmosphereSample(float(i+1)/float(ViewSamples),begin,end,closest);
        // Partition at the ground-shadow boundaries so the terminator cannot jump
        // when a uniform quadrature sample changes from illuminated to occluded.
        float c = clamp(shadow.x,a,b), d = clamp(shadow.y,c,b);
        integrateSegment(a,c,ray,sun,heights,betaR,betaM,phaseR,phaseM,transmission,scattering);
        integrateSegment(c,d,ray,sun,heights,betaR,betaM,phaseR,phaseM,transmission,scattering);
        integrateSegment(d,b,ray,sun,heights,betaR,betaM,phaseR,phaseM,transmission,scattering);
    }
    vec3 color = background*transmission + scattering*SPACE_SOLAR_IRRADIANCE;
    fragColor = vec4(mix(background,color,clamp(GlobalAlpha,0.0,1.0)),1);
}
