// Timed finite trajectories packed in the fifth sampler; no extra texture units.
uniform float DiscEnabled,DiscDiagnostic,DiscPhysical,DiscTime;
uniform vec3 DiscNormal,DiscU,DiscV;
const float DISC_PI=3.14159265359;
const int DISC_STRIDE=130;

vec2 discAddress(float angle) {
    int branch=angle<LensShadow?0:(angle<=DISC_PI*0.5?1:2);
    float t=branch==0?log(max(LensShadow-angle,0.000001)/0.000001)/log(LensShadow/0.000001)
            :branch==1?log(max(angle-LensShadow,0.000001)/0.000001)/LensLogRange
            :(angle-DISC_PI*0.5)/(DISC_PI*0.5);
    return vec2(clamp(t,0.0,1.0)*1023.0,float(branch));
}
vec4 discHeader(float angle) {
    vec2 p=discAddress(angle);int x=int(floor(p.x)),row=int(p.y)*DISC_STRIDE;
    return mix(texelFetch(LensAzimuth,ivec2(x,row),0),texelFetch(LensAzimuth,ivec2(min(x+1,1023),row),0),fract(p.x));
}
float discHermite(float a,float b,float da,float db,float t) {
    return (2.0*t*t*t-3.0*t*t+1.0)*a+(-2.0*t*t*t+3.0*t*t)*b
            +(t*t*t-2.0*t*t+t)*da+(t*t*t-t*t)*db;
}
float discTimeRate(float impact,float u) { return 1.0/(impact*u*u*(1.0-u)); }
vec4 discColumn(int x,int branch,float phi) {
    vec4 header=texelFetch(LensAzimuth,ivec2(x,branch*DISC_STRIDE),0);
    float p=clamp(phi/header.x,0.0,1.0)*128.0;
    int y=min(int(floor(p)),127);float t=p-float(y),h=header.x/128.0;
    vec3 a=texelFetch(LensAzimuth,ivec2(x,branch*DISC_STRIDE+1+y),0).xyz;
    vec3 b=texelFetch(LensAzimuth,ivec2(x,branch*DISC_STRIDE+2+y),0).xyz;
    float u=discHermite(a.x,b.x,h*a.y,h*b.y,t);
    float v=((6.0*t*t-6.0*t)*a.x+(-6.0*t*t+6.0*t)*b.x)/h
            +(3.0*t*t-4.0*t+1.0)*a.y+(3.0*t*t-2.0*t)*b.y;
    float delay=discHermite(a.z,b.z,h*discTimeRate(header.z,a.x),h*discTimeRate(header.z,b.x),t);
    return vec4(u,v,delay,phi<=header.x?1.0:0.0);
}
vec4 discTrajectory(float angle,float phi) {
    vec2 p=discAddress(angle);int x=int(floor(p.x)),branch=int(p.y);
    vec4 a=discColumn(x,branch,phi),b=discColumn(min(x+1,1023),branch,phi);
    vec4 result=mix(a,b,fract(p.x));result.w=min(a.w,b.w);return result;
}
vec3 discSpectrum(float temperature) {
    float p=clamp((temperature-1500.0)/10500.0,0.0,1.0)*255.0;
    int x=int(floor(p));
    return mix(texelFetch(LensAzimuth,ivec2(x,390),0).rgb,
            texelFetch(LensAzimuth,ivec2(min(x+1,255),390),0).rgb,fract(p));
}

float discFrequency(float r,float mu) {
    float speed=inversesqrt(2.0*(max(r,3.0)-1.0));
    return sqrt((1.0-1.0/r)/0.95)*sqrt(1.0-speed*speed)/(1.0-speed*mu);
}

float discFilteredNoise(vec3 point) {
    float width=max(length(dFdx(point)),length(dFdy(point)));
    return mix(0.5,spaceBackgroundNoise(point),exp(-0.6*width*width));
}

// Radiance and remaining line-of-sight transmission, accumulated camera to source.
vec4 discRadiance(vec3 radial,vec3 tangent,float angle) {
    float A=dot(radial,DiscNormal),B=dot(tangent,DiscNormal);
    float planeWeight=step(0.0000001,length(vec2(A,B)));
    float first=mod(atan(-A,B+(1.0-planeWeight)),DISC_PI);if(first<0.0000001)first+=DISC_PI;
    float impact=20.0*sin(angle)/sqrt(0.95);
    vec3 color=vec3(0);float transmission=1.0;
    // EDGE clamp has fewer than six crossings; deeper critical trajectories remain research-only.
    // Fixed loop, no early exit: derivatives are evaluated in uniform control flow.
    for(int order=0;order<6;order++) {
        float phi=first+float(order)*DISC_PI;
        vec4 path=discTrajectory(angle,phi);
        float u=clamp(path.x,0.015625,0.952381),r=1.0/u;
        vec3 position=radial*cos(phi)+tangent*sin(phi);
        vec3 angular=-radial*sin(phi)+tangent*cos(phi);
        float polar=atan(dot(position,DiscV),dot(position,DiscU));
        float omega=inversesqrt(2.0*r*r*r);
        float phase=polar-omega*(DiscTime-path.z);
        vec2 phaseUnit=vec2(cos(phase),sin(phase));
        float phaseWidth=max(length(dFdx(phaseUnit)),length(dFdy(phaseUnit)));
        float width=max(0.015,min(fwidth(r),0.8));
        float coverage=smoothstep(4.63-width,4.63+width,r)*(1.0-smoothstep(9.35-width,9.35+width,r));
        // Periodic coordinates avoid a polar wrap seam. Radial elongation and
        // orbital shear break the regular bands into material arcs and filaments.
        float broad=discFilteredNoise(vec3(r*2.1,phaseUnit*3.2)+vec3(17.8,41.2,6.3));
        float turbulence=discFilteredNoise(vec3(r*12.0+(broad-0.5)*3.0,phaseUnit*5.7)+vec3(7.2,31.1,13.6));
        float filaments=discFilteredNoise(vec3(r*31.0+(turbulence-0.5)*2.0,phaseUnit*8.1)+vec3(23.6,5.3,47.1));
        float envelope=1.0-smoothstep(8.45+0.65*turbulence,9.35,r);
        float finePhase=r*83.0+sin(phase*5.0)*1.1+sin(r*11.0)*0.7;
        float fineWidth=length(vec2(dFdx(finePhase),dFdy(finePhase)));
        float fine=cos(finePhase)*exp(-0.125*fineWidth*fineWidth);
        float medium=cos(r*21.0+sin(phase*3.0)*2.0)*exp(-0.125*pow(fwidth(r)*21.0,2.0));
        float patches=cos(phase*9.0+log(r)*4.0)*exp(-0.125*phaseWidth*phaseWidth*81.0);
        float density=clamp(0.10+0.85*turbulence*turbulence+0.60*filaments*filaments
                +0.10*fine+0.07*medium+0.07*patches,0.025,1.5);
        float incidence=abs(impact*u*sqrt(1.0-u)*dot(angular,DiscNormal));
        float alpha=(1.0-exp(-0.30*density*coverage*envelope/max(incidence,0.025)))*path.w*planeWeight;
        float mu=clamp(-impact*u*sqrt(1.0-u)*dot(cross(radial,tangent),DiscNormal),-1.0,1.0);
        // Emission is supported only outside ISCO; keep unused off-disc samples finite too.
        float frequency=discFrequency(r,mu);
        vec3 emission=discSpectrum(4500.0*(DiscPhysical>0.5?frequency:1.0))*3.2*density;
        if(DiscDiagnostic>0.5) {
            float sector=floor(mod(polar+DISC_PI,2.0*DISC_PI)/(2.0*DISC_PI)*12.0);
            float band=floor((r-4.63)/(9.35-4.63)*6.0);
            emission=mix(vec3(0.06,0.7,1.0),vec3(1.0,0.3,0.04),mod(sector+band,2.0))*0.7;
            alpha=coverage*path.w*planeWeight;
        }
        color+=transmission*alpha*emission;
        transmission*=1.0-alpha;
    }
    return vec4(color,transmission);
}
