#version 150
#moj_import <starboundmc:space_common.glsl>
#moj_import <starboundmc:space_atmosphere.glsl>
#moj_import <starboundmc:space_ring_optics.glsl>
#moj_import <starboundmc:space_texture.glsl>
uniform sampler2D Sampler0;
uniform float DistanceScale;
uniform float LinearColor;
uniform vec4 ColorModulator;
uniform vec3 SunDirection;
uniform vec3 CameraPositionMesh;
uniform float GroundRadius;
uniform float VolumeRadius;
uniform int RingLayer;
in vec2 texCoord;
in vec4 vertexColor;
in vec3 viewPosition;
in vec3 meshPosition;
out vec4 fragColor;
vec4 ringSample(float u) {
    float border=.5/float(textureSize(Sampler0,0).x);
    return spaceBilinear(Sampler0,vec2(clamp(u,border,1.0-border),texCoord.y),true);
}
void main() {
    vec3 view=normalize(CameraPositionMesh-meshPosition);
    if (RingLayer != 0) {
        // Rings lie outside the cloud/atmosphere bound. A ray can place a ring
        // either before the entire volume or after it, never inside that volume.
        vec3 ray=-view;
        float b=dot(CameraPositionMesh,ray);
        float d=b*b-dot(CameraPositionMesh,CameraPositionMesh)+VolumeRadius*VolumeRadius;
        bool front=true;
        if (d > 0.0) {
            float entry=max(0.0,-b-sqrt(d)), exit=-b+sqrt(d);
            if (exit > entry) front=length(meshPosition-CameraPositionMesh) <= entry;
        }
        if (front != (RingLayer > 0)) discard;
    }
    float footprint=max(abs(dFdx(texCoord.x)),abs(dFdy(texCoord.x)));
    vec4 texel=(ringSample(texCoord.x-.375*footprint)+ringSample(texCoord.x-.125*footprint)
              +ringSample(texCoord.x+.125*footprint)+ringSample(texCoord.x+.375*footprint))*.25;
    float tau=spaceRingOpticalDepth(texel.a);
    float opacity=1.0-exp(-tau/spaceRingCosine(view.y));
    if (opacity*ColorModulator.a < .001) discard;
    vec3 sun=normalize(SunDirection);
    float cosine=-dot(view,sun);
    float phase=.65*spaceRingPhase(cosine,.72)+.35*spaceRingPhase(cosine,-.3);
    bool reflection=view.y*sun.y > 0.0;
    float source=spaceRingSingleScatter(tau,abs(view.y),abs(sun.y),reflection)*phase;
    // A bounded diffuse closure keeps dense icy rings readable on the lit side.
    // It is an art approximation, not a solved multiple-scattering medium.
    float transport=tau*.5*(1.0/spaceRingCosine(view.y)+1.0/spaceRingCosine(sun.y));
    float diffuse=.8/3.14159265*abs(sun.y)*opacity*(1.0-exp(-tau))
            *(1.0-exp(-tau/spaceRingCosine(sun.y)));
    source += diffuse*(reflection ? transport : 1.0)/(1.0+transport);
    float visibility=spaceSunVisibility(meshPosition,sun,GroundRadius);
    // Existing ring art contains baked darkening; calibrate it as icy reflectance.
    vec3 albedo=min(vec3(1),texel.rgb*vertexColor.rgb*2.2);
    float luminance=dot(albedo,vec3(.2126,.7152,.0722));
    albedo=mix(luminance*vec3(1,.97,.92),albedo,.3);
    vec3 radiance=albedo*
            (.0015+SPACE_SOLAR_IRRADIANCE*.95*source*visibility/max(opacity,.0001));
    if (LinearColor < .5) radiance=spaceToDisplay(radiance*(1.0+radiance/16.0)/(1.0+radiance));
    fragColor=vec4(radiance,opacity*ColorModulator.a);
    gl_FragDepth = celestialDepth(length(viewPosition) * DistanceScale);
}
