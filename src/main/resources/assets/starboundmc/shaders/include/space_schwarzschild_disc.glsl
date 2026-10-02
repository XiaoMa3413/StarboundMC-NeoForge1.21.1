// Timed finite trajectories packed in the fifth sampler; no extra texture units.
uniform float DiscEnabled,DiscDiagnostic,DiscPhysical,DiscTime,DiscObserverRadius,DiscShadowLow,DiscBeamGrid;
uniform vec3 DiscNormal,DiscU,DiscV;
const float DISC_PI=3.14159265359;
const int DISC_STRIDE=131;

vec2 discAddress(float angle) {
    // Subtract the high part first, then the residual. Adding both shadow
    // parts before subtraction would round away the correction again.
    float distance=(angle-LensShadow)-DiscShadowLow;
    int branch=distance<0.0?0:(angle<=DISC_PI*0.5?1:2);
    float t=branch==0?log(max(-distance,0.000001)/0.000001)/log(LensShadow/0.000001)
            :branch==1?log(max(distance,0.000001)/0.000001)/LensLogRange
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
    return mix(texelFetch(LensAzimuth,ivec2(x,393),0).rgb,
            texelFetch(LensAzimuth,ivec2(min(x+1,255),393),0).rgb,fract(p));
}

float discFrequency(float r,float mu) {
    float speed=inversesqrt(2.0*(max(r,3.0)-1.0));
    return sqrt((1.0-1.0/r)/(1.0-1.0/DiscObserverRadius))*sqrt(1.0-speed*speed)/(1.0-speed*mu);
}

// Analytic gradient of the same trilinear value noise used by the sky. Explicit
// footprints make material evaluation safe inside per-pixel visibility branches.
vec4 discNoiseGradient(vec3 point) {
    vec3 cell=floor(point),p=fract(point),s=p*p*(3.0-2.0*p),ds=6.0*p*(1.0-p);
    float a=spaceBackgroundHash(cell),b=spaceBackgroundHash(cell+vec3(1,0,0));
    float c=spaceBackgroundHash(cell+vec3(0,1,0)),d=spaceBackgroundHash(cell+vec3(1,1,0));
    float e=spaceBackgroundHash(cell+vec3(0,0,1)),f=spaceBackgroundHash(cell+vec3(1,0,1));
    float g=spaceBackgroundHash(cell+vec3(0,1,1)),h=spaceBackgroundHash(cell+vec3(1,1,1));
    float x0=mix(a,b,s.x),x1=mix(c,d,s.x),x2=mix(e,f,s.x),x3=mix(g,h,s.x);
    float y0=mix(x0,x1,s.y),y1=mix(x2,x3,s.y);
    vec3 gradient=vec3(mix(mix(b-a,d-c,s.y),mix(f-e,h-g,s.y),s.z)*ds.x,
            mix(x1-x0,x3-x2,s.z)*ds.y,(y1-y0)*ds.z);
    return vec4(gradient,mix(y0,y1,s.z));
}
float discFilteredNoise(vec3 point,vec3 dx,vec3 dy,out vec2 slope) {
    vec4 noise=discNoiseGradient(point);
    float width=max(length(dx),length(dy)),fade=exp(-.6*width*width);
    // First-order footprint: its filtering width is locally constant. The
    // gradient feeds domain warps without evaluating derivatives in a branch.
    slope=vec2(dot(noise.xyz,dx),dot(noise.xyz,dy))*fade;
    return mix(.5,noise.w,fade);
}

// Finite-lived advected structures avoid winding one initial noise field into
// increasingly fine complete rings. Two overlapping generations have zero
// weight and zero time derivative when their seed changes. This is an artistic
// stochastic flow closure, not a fluid simulation.
vec3 discFlowLayer(float r,float polar,float age,float generation,vec2 radiusSlope,vec2 polarSlope,vec2 timeSlope) {
    float omega=inversesqrt(2.0*r*r*r);
    float phase=polar-omega*age;
    vec2 p=vec2(cos(phase),sin(phase));
    vec2 phaseSlope=polarSlope-omega*timeSlope+1.5*omega*age/r*radiusSlope;
    vec2 px=vec2(-p.y,p.x)*phaseSlope.x,py=vec2(-p.y,p.x)*phaseSlope.y;
    vec3 seed=vec3(17.8,41.2,6.3)+vec3(5.7,13.1,3.9)*generation;
    vec2 broadSlope,ribbonSlope;
    float broad=discFilteredNoise(vec3(r*1.6,p*4.3)+seed,
            vec3(radiusSlope.x*1.6,px*4.3),vec3(radiusSlope.y*1.6,py*4.3),broadSlope);
    vec2 warpedRadius=radiusSlope*10.0+broadSlope*5.0;
    float ribbon=discFilteredNoise(vec3(r*10.0+(broad-.5)*5.0,p*11.3)+seed+37.1,
            vec3(warpedRadius.x,px*11.3),vec3(warpedRadius.y,py*11.3),ribbonSlope);
    float filaments=.5;
    if(FieldDetail>.5) {
        vec2 warpSlope,filamentSlope;
        float warp=discFilteredNoise(vec3(r*3.7,p*7.1)+seed+19.7,
                vec3(radiusSlope.x*3.7,px*7.1),vec3(radiusSlope.y*3.7,py*7.1),warpSlope);
        vec2 fineRadius=radiusSlope*31.0+warpSlope*8.0;
        filaments=discFilteredNoise(vec3(r*31.0+(warp-.5)*8.0,p*19.7)+seed+71.3,
                vec3(fineRadius.x,px*19.7),vec3(fineRadius.y,py*19.7),filamentSlope);
    }
    return vec3(broad,ribbon,filaments);
}
vec3 discMaterial(float r,vec3 flow) {
    // Broad dark gaps survive between branching bright ribbons; neither a
    // global sine band nor a uniform colour supplies the final structure.
    // A ridge transform would put its maximum at the filtered noise mean .5,
    // turning unresolved images into excessively hot, dense complete rings.
    float density=clamp(.035+2.4*pow(flow.y,3.0)*pow(flow.x,1.4)
            +pow(flow.z,4.0)*flow.x,.015,1.8);
    float temperature=clamp(4700.0*pow(4.63/r,.35)+(density-.45)*2400.0
            +(flow.x-.5)*1200.0,2400.0,5900.0);
    float envelope=1.0-smoothstep(8.45+.65*flow.x,9.35,r);
    return vec3(density,temperature,envelope);
}
vec4 discGenerationRadiance(vec3 material,float frequency,float opticalPath) {
    float alpha=1.0-exp(-.30*material.x*material.z*opticalPath);
    // Density controls optical depth. An opaque thermal layer approaches its
    // source spectrum instead of multiplying that spectrum by density again.
    vec3 source=discSpectrum(material.y*frequency)*5.0;
    return vec4(source*alpha,alpha);
}
vec4 discFlowRadiance(float r,float polar,float emissionTime,float frequency,float opticalPath,
        vec2 radiusSlope,vec2 polarSlope,vec2 timeSlope) {
    const float lifetime=70.0;
    float ageA=mod(emissionTime,lifetime),ageB=mod(emissionTime+lifetime*.5,lifetime);
    float weightA=pow(sin(DISC_PI*ageA/lifetime),2.0);
    vec3 a=discMaterial(r,discFlowLayer(r,polar,ageA,floor(emissionTime/lifetime)*2.0,radiusSlope,polarSlope,timeSlope));
    vec3 b=discMaterial(r,discFlowLayer(r,polar,ageB,floor((emissionTime+lifetime*.5)/lifetime)*2.0+1.0,radiusSlope,polarSlope,timeSlope));
    // Mix emitted radiance and coverage after the nonlinear thermal/opacity
    // responses. Mixing noise or temperature first causes periodic dimming.
    return mix(discGenerationRadiance(b,frequency,opticalPath),
            discGenerationRadiance(a,frequency,opticalPath),weightA);
}

// Radiance and remaining line-of-sight transmission, accumulated camera to source.
vec4 discRadiance(vec3 radial,vec3 tangent,float angle) {
    float A=dot(radial,DiscNormal),B=dot(tangent,DiscNormal);
    float planeWeight=step(0.0000001,length(vec2(A,B)));
    float first=mod(atan(-A,B+(1.0-planeWeight)),DISC_PI);if(first<0.0000001)first+=DISC_PI;
    float impact=DiscObserverRadius*sin(angle)/sqrt(1.0-1.0/DiscObserverRadius);
    vec3 color=vec3(0);float transmission=1.0;
    // EDGE clamp has fewer than six crossings; deeper critical trajectories remain research-only.
    // Geometry derivatives stay in uniform control flow. Material uses explicit
    // gradients and can skip intersections which have no emitting support.
    for(int order=0;order<6;order++) {
        float phi=first+float(order)*DISC_PI;
        vec4 path=discTrajectory(angle,phi);
        float u=clamp(path.x,0.015625,0.952381),r=1.0/u;
        vec3 position=radial*cos(phi)+tangent*sin(phi);
        vec3 angular=-radial*sin(phi)+tangent*cos(phi);
        float polar=atan(dot(position,DiscV),dot(position,DiscU));
        vec2 polarUnit=vec2(cos(polar),sin(polar));
        vec2 radiusSlope=vec2(dFdx(r),dFdy(r));
        vec2 polarSlope=vec2(dot(vec2(-polarUnit.y,polarUnit.x),dFdx(polarUnit)),
                dot(vec2(-polarUnit.y,polarUnit.x),dFdy(polarUnit)));
        vec2 timeSlope=-vec2(dFdx(path.z),dFdy(path.z));
        float width=max(0.015,min(abs(radiusSlope.x)+abs(radiusSlope.y),0.8));
        float coverage=smoothstep(4.63-width,4.63+width,r)*(1.0-smoothstep(9.35-width,9.35+width,r));
        float incidence=abs(impact*u*sqrt(1.0-u)*dot(angular,DiscNormal));
        float mu=clamp(-impact*u*sqrt(1.0-u)*dot(cross(radial,tangent),DiscNormal),-1.0,1.0);
        // Emission is supported only outside ISCO; keep unused off-disc samples finite too.
        float frequency=discFrequency(r,mu);
        vec4 emitted=vec4(0);
        if(coverage*path.w*planeWeight>0.0)
            emitted=discFlowRadiance(r,polar,DiscTime-path.z,DiscPhysical>0.5?frequency:1.0,
                    coverage/max(incidence,.025),radiusSlope,polarSlope,timeSlope)*path.w*planeWeight;
        if(DiscDiagnostic>0.5) {
            float sector=floor(mod(polar+DISC_PI,2.0*DISC_PI)/(2.0*DISC_PI)*12.0);
            float band=floor((r-4.63)/(9.35-4.63)*6.0);
            float alpha=coverage*path.w*planeWeight;
            emitted=vec4(mix(vec3(.06,.7,1.0),vec3(1.0,.3,.04),mod(sector+band,2.0))*.7*alpha,alpha);
        }
        color+=transmission*emitted.rgb;
        transmission*=1.0-emitted.a;
    }
    return vec4(color,transmission);
}

struct DiscBeamRay {vec3 tangent;float angle;float first;float impact;float planeWeight;};
DiscBeamRay discBeamRay(vec3 radial,vec3 direction) {
    float cosine=clamp(-dot(radial,direction),-1.0,1.0);
    vec3 tangent=direction+radial*cosine;
    tangent=dot(tangent,tangent)>1e-12?normalize(tangent):normalize(cross(radial,vec3(0,1,0)));
    float A=dot(radial,DiscNormal),B=dot(tangent,DiscNormal);
    float weight=step(.0000001,length(vec2(A,B)));
    float first=mod(atan(-A,B+(1.0-weight)),DISC_PI);if(first<.0000001)first+=DISC_PI;
    float angle=acos(cosine);
    return DiscBeamRay(tangent,angle,first,DiscObserverRadius*sin(angle)/sqrt(1.0-1.0/DiscObserverRadius),weight);
}
// Radius, periodic polar coordinate, look-back time and finite-path validity.
vec4 discBeamHit(vec3 radial,DiscBeamRay ray,int order) {
    float phi=ray.first+float(order)*DISC_PI;
    vec4 path=discTrajectory(ray.angle,phi);
    vec3 position=radial*cos(phi)+ray.tangent*sin(phi);
    return vec4(1.0/clamp(path.x,.015625,.952381),atan(dot(position,DiscV),dot(position,DiscU)),path.z,path.w*ray.planeWeight);
}
vec4 discBeamSample(vec3 radial,vec3 direction,vec3 dx,vec3 dy) {
    DiscBeamRay ray=discBeamRay(radial,direction),rx=discBeamRay(radial,normalize(direction+dx)),ry=discBeamRay(radial,normalize(direction+dy));
    vec3 color=vec3(0);float transmission=1.0;
    for(int order=0;order<6;order++) {
        vec4 hit=discBeamHit(radial,ray,order);
        // Phi increases monotonically: no later plane event can lie inside a
        // trajectory which already ended. Explicit gradients permit this exit.
        if(hit.w<.5)break;
        // Radial coverage has a bounded .8 rs support. These events cannot emit
        // even with the widest allowed footprint; skip both neighbour lookups.
        if(hit.x<3.83 || hit.x>10.15)continue;
        vec4 hx=discBeamHit(radial,rx,order),hy=discBeamHit(radial,ry,order);
        vec2 radiusSlope=vec2(hx.x-hit.x,hy.x-hit.x);
        vec2 polarDifference=vec2(hx.y-hit.y,hy.y-hit.y);
        vec2 polarSlope=atan(sin(polarDifference),cos(polarDifference));
        vec2 timeSlope=-vec2(hx.z-hit.z,hy.z-hit.z);
        float width=max(.015,min(abs(radiusSlope.x)+abs(radiusSlope.y),.8));
        float coverage=smoothstep(4.63-width,4.63+width,hit.x)*(1.0-smoothstep(9.35-width,9.35+width,hit.x));
        if(coverage*hit.w>0.0) {
            float phi=ray.first+float(order)*DISC_PI,u=1.0/hit.x;
            vec3 angular=-radial*sin(phi)+ray.tangent*cos(phi);
            float incidence=abs(ray.impact*u*sqrt(1.0-u)*dot(angular,DiscNormal));
            float mu=clamp(-ray.impact*u*sqrt(1.0-u)*dot(cross(radial,ray.tangent),DiscNormal),-1.0,1.0);
            vec4 emitted=discFlowRadiance(hit.x,hit.y,DiscTime-hit.z,DiscPhysical>.5?discFrequency(hit.x,mu):1.0,
                    coverage/max(incidence,.025),radiusSlope,polarSlope,timeSlope)*hit.w;
            if(DiscDiagnostic>.5) {
                float sector=floor(mod(hit.y+DISC_PI,2.0*DISC_PI)/(2.0*DISC_PI)*12.0);
                float band=floor((hit.x-4.63)/(9.35-4.63)*6.0),alpha=coverage*hit.w;
                emitted=vec4(mix(vec3(.06,.7,1.0),vec3(1.0,.3,.04),mod(sector+band,2.0))*.7*alpha,alpha);
            }
            color+=transmission*emitted.rgb;transmission*=1.0-emitted.a;
        }
    }
    return vec4(color,transmission);
}
vec4 discFiniteBeam(vec3 radial,vec3 direction,vec3 dx,vec3 dy) {
    int grid=int(clamp(DiscBeamGrid,1.0,8.0));float cell=1.0/float(grid);
    vec4 integral=vec4(0);
    for(int y=0;y<8;y++)for(int x=0;x<8;x++) {
        if(x>=grid || y>=grid)continue;
        vec2 offset=(vec2(x,y)+.5)*cell-.5;
        vec3 sampleDirection=normalize(direction+dx*offset.x+dy*offset.y);
        integral+=discBeamSample(radial,sampleDirection,dx*cell,dy*cell)*cell*cell;
    }
    return integral;
}
