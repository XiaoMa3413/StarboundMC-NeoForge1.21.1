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
#moj_import <starboundmc:space_schwarzschild_disc.glsl>
#moj_import <starboundmc:space_schwarzschild_volume.glsl>
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
    if(TestMode>19.5) {
        vec3 radial=vec3(0,0,1);
        DiscBeamRay center=discBeamRay(radial,vec3(sin(TestAngle),0,-cos(TestAngle)));
        DiscBeamRay neighbour=discBeamRay(radial,vec3(sin(TestAngle+TestAnisotropy),0,-cos(TestAngle+TestAnisotropy)));
        center.angle=TestAngle;neighbour.angle=TestAngle+TestAnisotropy;
        VolumeLookup lookup=volumeLookup(center.angle),other=volumeLookup(neighbour.angle);
        float q;vec4 path=volumeTrajectory(lookup,TestSigma,q);
        bool allowed=volumeDifferentialAllowed(center,neighbour,neighbour);
        float impact=mix(lookup.a.z,lookup.b.z,lookup.blend);
        vec4 fast=volumeFootPoint(radial,neighbour,other,TestSigma+TestRotation,path,center.angle,TestSigma,q,impact,allowed);
        vec4 full=volumePoint(radial,neighbour,other,TestSigma+TestRotation);
        bool used=allowed && abs(TestRotation)<=.01 && abs(path.y*TestRotation)+abs(q*TestAnisotropy)<=.005*path.x;
        fragColor=vec4(fast.x-full.x,fast.z-full.z,fast.w-full.w,used?1.0:0.0);return;
    }
    if(TestMode>18.5) {
        VolumeLookup lookup=volumeLookup(TestAngle);float q;
        vec4 path=volumeTrajectory(lookup,TestSigma,q);float u=path.x,b=mix(lookup.a.z,lookup.b.z,lookup.blend);
        fragColor=vec4(q,b*path.y*q/((1.0-u)*u*u),0,path.w);return;
    }
    if(TestMode>17.5) {fragColor=volumeNoiseGradient(TestDirection);return;}
    if(TestMode>16.5) {
        VolumeLookup lookup=volumeLookup(TestAngle);
        fragColor=vec4(lookup.a.z,DiscObserverRadius,TestAngle,lookup.radial?1.0:0.0);return;
    }
    if(TestMode>15.5) {fragColor=volumeTrajectory(volumeLookup(TestAngle),TestSigma);return;}
    if(TestMode>14.5) {
        vec3 direction=vec3(sin(TestAngle),0,-cos(TestAngle));
        fragColor=discVolumeBeamMode(vec3(0,0,1),direction,vec3(cos(TestAngle),0,sin(TestAngle))*TestSigma,
                vec3(0,TestSigma*TestAnisotropy,0),true);return;
    }
    if(TestMode>13.5) {
        fragColor=volumeFlow(TestSigma,TestAngle,TestAnisotropy,DiscTime,1.0,1.0,
                vec3(0),vec3(0),vec3(0),vec3(0));return;
    }
    if(TestMode>12.5) {
        vec3 radial=vec3(0,0,1),direction=vec3(sin(TestAngle),0,-cos(TestAngle));
        DiscBeamRay beam=discBeamRay(radial,direction);
        // The diagnostic directly queries the supplied float angle, as in the
        // timed-ray check; projection inversion is validated by image fixtures.
        beam.angle=TestAngle;beam.impact=DiscObserverRadius*sin(TestAngle)/sqrt(1.0-1.0/DiscObserverRadius);
        float tau;vec4 transfer=discVolumeSample(radial,beam,beam,beam,true,tau);
        fragColor=vec4(tau,transfer.a,transfer.r,1);return;
    }
    if(TestMode>11.5) { fragColor=discShell(TestAngle);return; }
    if(TestMode>10.5) {
        vec3 direction=vec3(sin(TestAngle),0,-cos(TestAngle));
        fragColor=discFiniteBeam(vec3(0,0,1),direction,vec3(cos(TestAngle),0,sin(TestAngle))*TestSigma,
                vec3(0,TestSigma*TestAnisotropy,0));return;
    }
    if(TestMode>9.5) { fragColor=vec4(spaceBackgroundNoise(TestDirection),0,0,1);return; }
    if(TestMode>8.5) { fragColor=discNoiseGradient(TestDirection);return; }
    if(TestMode>7.5) { fragColor=discFlowRadiance(TestSigma,TestAngle,DiscTime,1.0,1.0,vec2(0),vec2(0),vec2(0));return; }
    if(TestMode>6.5) { fragColor=discRadiance(vec3(0,0,1),normalize(TestDirection),TestAngle);return; }
    if(TestMode>5.5) { fragColor=discHeader(TestAngle);return; }
    if(TestMode>4.5) { fragColor=vec4(discFrequency(TestSigma,TestAngle),0,0,1);return; }
    if(TestMode>3.5) { fragColor=vec4(discSpectrum(TestSigma),1);return; }
    if(TestMode>2.5) { fragColor=discTrajectory(TestAngle,TestSigma);return; }
    if(TestMode>1.5) { fragColor=vec4(lensAzimuth(TestAngle),0,0,1);return; }
    vec4 ray=InverseViewProjection*vec4(screenPosition,1,1);
    vec3 direction=normalize(ray.xyz);float escapeCoverage=1.0;vec4 disc=vec4(0,0,0,1);
    vec3 imageDirection=direction,imageDx=dFdx(direction),imageDy=dFdy(direction);
    if(LensEnabled>0.5) {
        vec3 radial=-normalize(LensDirection);
        float cosine=clamp(-dot(direction,radial),-1.0,1.0);
        float angle=acos(cosine),width=max(fwidth(angle),0.000001);
        float criticalDistance=(angle-LensShadow)-(DiscEnabled>0.5?DiscShadowLow:0.0);
        escapeCoverage=clamp(criticalDistance/width+0.5,0.0,1.0);
        vec3 tangent=direction+radial*cosine;
        tangent=dot(tangent,tangent)>1e-12 ? normalize(tangent) : normalize(cross(radial,vec3(0,1,0)));
        if(DiscEnabled>0.5) {
            // Half an fwidth spans the angular extent of the pixel. Keep a
            // one-pixel guard beyond that support rather than a broad annulus.
            float beamWeight=1.0-smoothstep(width*.5,width*1.5,abs(criticalDistance));
            if(DiscVolumeEnabled>.5) {
                disc=discVolumeImage(radial,imageDirection,imageDx,imageDy);
                if(DiscBeamGrid>1.5 && beamWeight>0.0)
                    disc=mix(disc,discVolumeBeam(radial,imageDirection,imageDx,imageDy),beamWeight);
            } else {
                disc=discRadiance(radial,tangent,angle);
                if(DiscBeamGrid>1.5 && beamWeight>0.0)
                    disc=mix(disc,discFiniteBeam(radial,imageDirection,imageDx,imageDy),beamWeight);
            }
        }
        float azimuth=DiscEnabled>0.5?discHeader(max(angle,LensShadow+0.000001)).y
                :lensAzimuth(max(angle,LensShadow+0.000001));
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
    color=disc.rgb+color*disc.a;
    if(LinearColor<0.5)color=spaceToDisplay(color);
    fragColor=vec4(color,1);
}
