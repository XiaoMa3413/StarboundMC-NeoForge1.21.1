#version 150
#moj_import <starboundmc:space_common.glsl>
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
vec2 density(vec3 p, float thickness) {
    float altitude = max(0.0, length(p) - InnerRadius) / thickness;
    return exp(-altitude * vec2(5.5,16.0));
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
    // Analytic ground termination keeps the shell independent of sphere tessellation.
    vec2 ground = sphere(CameraPositionMesh,ray,InnerRadius);
    if (ground.x > 0.0 && ground.y > ground.x) end = min(end,ground.x);
    if (end <= begin) discard;
    float thickness = max(OuterRadius-InnerRadius, .0001);
    // Profile chromaticity controls artistic species; optical coefficients are linear.
    vec3 tint = max(spaceToLinear(AtmosphereColor), vec3(.015));
    tint /= max(tint.r,max(tint.g,tint.b));
    vec3 betaR = mix(vec3(.19,.44,1.0), tint, .65) * (1.8*AtmosphereStrength/thickness);
    vec3 betaM = vec3(.16*AtmosphereStrength/thickness);
    float cosine = dot(ray,sun), g = .76;
    float phaseR = 3.0/(16.0*PI)*(1.0+cosine*cosine);
    float phaseM = (1.0-g*g)/(4.0*PI*pow(1.0+g*g-2.0*g*cosine,1.5));
    float stepSize = (end-begin)/float(ViewSamples);
    vec2 shadow = shadowInterval(CameraPositionMesh,ray,sun,begin,end);
    vec2 viewDepth = vec2(0);
    vec3 scattering = vec3(0);
    for (int i=0; i<28; i++) {
        if (i >= ViewSamples) break;
        float segmentBegin = begin+float(i)*stepSize;
        float midpoint = segmentBegin+.5*stepSize;
        vec3 p = CameraPositionMesh + ray*midpoint;
        vec2 rho = density(p,thickness), segment = rho*stepSize;
        float shadowBegin = max(segmentBegin,shadow.x), shadowEnd = min(segmentBegin+stepSize,shadow.y);
        float shadowLength = max(0.0,shadowEnd-shadowBegin);
        float litLength = max(0.0,stepSize-shadowLength);
        if (litLength > stepSize*.0001) {
            // Midpoint of the illuminated part, including a segment straddling the shadow edge.
            float litMidpoint = (midpoint*stepSize-(shadowBegin+shadowEnd)*.5*shadowLength)/litLength;
            p = CameraPositionMesh+ray*clamp(litMidpoint,segmentBegin,segmentBegin+stepSize);
            vec2 litDensity = density(p,thickness);
            float lightLength = max(0.0,sphere(p,sun,OuterRadius).y);
            float lightStep = lightLength/float(LightSamples);
            vec2 lightDepth = vec2(0);
            for (int j=0; j<8; j++) {
                if (j >= LightSamples) break;
                lightDepth += density(p+sun*((float(j)+.5)*lightStep),thickness)*lightStep;
            }
            vec2 optical = viewDepth + rho*(litMidpoint-segmentBegin) + lightDepth;
            vec3 transmission = exp(-(betaR*optical.x+betaM*optical.y));
            scattering += transmission*(betaR*litDensity.x*phaseR+betaM*litDensity.y*phaseM)*litLength;
        }
        viewDepth += segment;
    }
    vec3 transmission = exp(-(betaR*viewDepth.x+betaM*viewDepth.y));
    vec3 color = background*transmission + scattering*18.0;
    fragColor = vec4(mix(background,color,clamp(GlobalAlpha,0.0,1.0)),1);
}
