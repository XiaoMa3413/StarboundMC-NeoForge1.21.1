#version 150
#moj_import <starboundmc:space_common.glsl>
#moj_import <starboundmc:space_background_field.glsl>
#moj_import <starboundmc:space_stellar_query.glsl>
uniform sampler2D LensAzimuth;
uniform mat4 InverseViewProjection;
uniform float LinearColor,FieldDetail,TintAmount,LensEnabled,LensShadow,LensLogRange;
uniform vec3 TintColor,LensDirection;
// Used only by the real GPU validation fixture; normal draws always set zero.
uniform float TestMode,TestSigma,TestAngle,TestAnisotropy,TestRotation,TestImagePsf;
uniform vec3 TestDirection;
in vec2 screenPosition;
out vec4 fragColor;

float lensAzimuth(float angle) {
    int row=angle>1.57079632679 ? 1 : 0;
    float t=row==1 ? (angle-1.57079632679)/1.57079632679
            : log(max(angle-LensShadow,0.000001)/0.000001)/LensLogRange;
    float p=clamp(t,0.0,1.0)*2047.0;int left=int(floor(p)),right=min(left+1,2047);
    return mix(texelFetch(LensAzimuth,ivec2(left,row),0).r,texelFetch(LensAzimuth,ivec2(right,row),0).r,fract(p));
}
void main() {
    if(TestMode>1.5) { fragColor=vec4(lensAzimuth(TestAngle),0,0,1);return; }
    vec4 ray=InverseViewProjection*vec4(screenPosition,1,1);
    vec3 direction=normalize(ray.xyz);float escapeCoverage=1.0;
    if(LensEnabled>0.5) {
        vec3 radial=-normalize(LensDirection);
        float cosine=clamp(-dot(direction,radial),-1.0,1.0);
        float angle=acos(cosine),width=max(fwidth(angle),0.000001);
        escapeCoverage=clamp((angle-LensShadow)/width+0.5,0.0,1.0);
        vec3 tangent=direction+radial*cosine;
        tangent=dot(tangent,tangent)>1e-12 ? normalize(tangent) : normalize(cross(radial,vec3(0,1,0)));
        float azimuth=lensAzimuth(max(angle,LensShadow+0.000001));
        direction=normalize(radial*cos(azimuth)+tangent*sin(azimuth));
    }
    vec3 dx=dFdx(direction),dy=dFdy(direction);
    if(TestMode>0.5) {
        direction=normalize(TestDirection);
        vec3 t=normalize(cross(abs(direction.y)<0.9?vec3(0,1,0):vec3(1,0,0),direction));
        vec3 b=cross(direction,t);float cs=cos(TestRotation),sn=sin(TestRotation);
        dx=(t*cs+b*sn)*(TestSigma*sqrt(12.0));
        dy=(-t*sn+b*cs)*(TestSigma*TestAnisotropy*sqrt(12.0));
    }
    float footprint=max(length(dx),length(dy));
    vec3 transmission=TestMode>0.5 ? vec3(1) : spaceBackgroundTransmission(direction,FieldDetail);
    vec3 stars=skyIndexedStars(direction,dx,dy,transmission,TestMode<0.5 || TestImagePsf>0.5);
    if(TestMode<0.5)stars=mix(stars,stars*(0.75+TintColor*0.25),clamp(TintAmount,0.0,1.0));
    vec3 color=TestMode>0.5 ? stars : stars+spaceBackgroundRadiance(direction,TintColor,TintAmount,FieldDetail,footprint);
    color*=escapeCoverage;
    if(LinearColor<0.5)color=spaceToDisplay(color);
    fragColor=vec4(color,1);
}
