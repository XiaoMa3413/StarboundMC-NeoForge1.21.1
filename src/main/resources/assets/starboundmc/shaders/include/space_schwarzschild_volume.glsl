// Finite-height, grey research medium. Geometry uses the existing timed null
// trajectories; the compact vertical profile and stochastic flow are closures.
uniform float DiscVolumeEnabled,DiscThickness,DiscVolumeSteps,DiscPiHigh,DiscPiLow,DiscDifferentials;
float discHeightProfile(float height);
vec4 volumeNoisePlane(ivec3 cell) {
    cell=cell&ivec3(63);
    int pixel=cell.x+64*cell.y+4096*cell.z;
    return texelFetch(LensAzimuth,ivec2(pixel&1023,394+(pixel>>10)),0);
}
vec4 volumeNoiseGradient(vec3 point) {
    ivec3 cell=ivec3(floor(point));vec3 p=fract(point),s=p*p*(3.0-2.0*p),ds=6.0*p*(1.0-p);
    // Two RGBA fetches supply all eight corners, without dynamic component
    // indexing or corner-dependent branches in the inner volume loop.
    vec4 low=volumeNoisePlane(cell),high=volumeNoisePlane(cell+ivec3(0,0,1));
    vec2 ab=low.xy,cd=low.zw,ef=high.xy,gh=high.zw;
    vec4 x=mix(vec4(ab.x,cd.x,ef.x,gh.x),vec4(ab.y,cd.y,ef.y,gh.y),s.x);
    vec2 y=mix(x.xz,x.yw,s.y);
    vec3 gradient=vec3(mix(mix(ab.y-ab.x,cd.y-cd.x,s.y),mix(ef.y-ef.x,gh.y-gh.x,s.y),s.z)*ds.x,
            mix(x.y-x.x,x.w-x.z,s.z)*ds.y,(y.y-y.x)*ds.z);
    return vec4(gradient,mix(y.x,y.y,s.z));
}
float volumeFilteredNoise(vec3 point,vec3 dx,vec3 dy,vec3 dz,out vec3 slope) {
    float widthSquared=max(max(dot(dx,dx),dot(dy,dy)),dot(dz,dz));
    // Beyond this width the value rounds to .5; the remaining projected
    // gradient is below 1e-8, which is omitted as a bounded filtering remainder.
    if(widthSquared>36.0) {slope=vec3(0);return .5;}
    vec4 noise=volumeNoiseGradient(point);float fade=exp(-.6*widthSquared);
    slope=vec3(dot(noise.xyz,dx),dot(noise.xyz,dy),dot(noise.xyz,dz))*fade;
    return mix(.5,noise.w,fade);
}
struct VolumeLookup {int x;int branch;float blend;vec4 a;vec4 b;bool radial;};
VolumeLookup volumeLookup(float angle) {
    // The native sine approximation loses relative accuracy close to zero
    // (measured .75% at 1e-5 on the validation GPU). Reduce the small endpoint
    // angle explicitly, retaining pi's float residual at the outward pole.
    // Separate uniforms prevent compile-time folding of pi + its residual
    // back to a single float before subtracting the near-pi angle.
    float small=angle<DISC_PI*.5?angle:(DiscPiHigh-angle)+DiscPiLow;
    small=max(small,0.0);
    float sine=small<.02?small*(1.0-small*small/6.0):abs(sin(angle));
    float impact=DiscObserverRadius*sine/sqrt(1.0-1.0/DiscObserverRadius);
    if(impact<.12) {
        // Near-radial interpolation mixes columns whose phi scales differ by
        // thousands. Use the bounded radial asymptote in this small-impact
        // domain; its error is checked against the independent nonradial ODE.
        impact=max(impact,DiscObserverRadius*.000001/sqrt(1.0-1.0/DiscObserverRadius));
        float sign=angle<DISC_PI*.5?1.0:-1.0;
        float end=impact*(sign>0.0?1.0/1.05-1.0/DiscObserverRadius:1.0/DiscObserverRadius-1.0/64.0);
        vec4 header=vec4(end,sign,impact,0);
        return VolumeLookup(0,0,0.0,header,header,true);
    }
    vec2 p=discAddress(angle);int x=int(floor(p.x)),branch=int(p.y),row=branch*DISC_STRIDE;
    return VolumeLookup(x,branch,fract(p.x),texelFetch(LensAzimuth,ivec2(x,row),0),
            texelFetch(LensAzimuth,ivec2(min(x+1,1023),row),0),false);
}
vec4 volumeColumn(int x,int branch,vec4 header,float phi,out float angularDerivative) {
    float p=clamp(phi/header.x,0.0,1.0)*128.0;
    int y=min(int(floor(p)),127);float t=p-float(y),h=header.x/128.0;
    vec4 a=texelFetch(LensAzimuth,ivec2(x,branch*DISC_STRIDE+1+y),0);
    vec4 b=texelFetch(LensAzimuth,ivec2(x,branch*DISC_STRIDE+2+y),0);
    float u=discHermite(a.x,b.x,h*a.y,h*b.y,t);
    float v=((6.0*t*t-6.0*t)*a.x+(-6.0*t*t+6.0*t)*b.x)/h
            +(3.0*t*t-4.0*t+1.0)*a.y+(3.0*t*t-2.0*t)*b.y;
    float delay=discHermite(a.z,b.z,h*discTimeRate(header.z,a.x),h*discTimeRate(header.z,b.x),t);
    angularDerivative=mix(a.w,b.w,t);
    return vec4(u,v,delay,phi<=header.x?1.0:0.0);
}
vec4 volumeTrajectory(VolumeLookup lookup,float phi,out float angularDerivative) {
    if(lookup.radial) {
        float u=1.0/DiscObserverRadius+lookup.a.y*phi/lookup.a.z,r=1.0/u;
        float delay=lookup.a.y*(DiscObserverRadius-r+log((DiscObserverRadius-1.0)/(r-1.0)));
        angularDerivative=0.0;
        return vec4(u,lookup.a.y/lookup.a.z,delay,phi<=lookup.a.x?1.0:0.0);
    }
    float qa,qb;
    vec4 a=volumeColumn(lookup.x,lookup.branch,lookup.a,phi,qa);
    vec4 b=volumeColumn(min(lookup.x+1,1023),lookup.branch,lookup.b,phi,qb);
    angularDerivative=mix(qa,qb,lookup.blend);
    vec4 result=mix(a,b,lookup.blend);result.w=min(a.w,b.w);return result;
}
vec4 volumeTrajectory(VolumeLookup lookup,float phi) {
    float q;return volumeTrajectory(lookup,phi,q);
}
float volumeLocalU(VolumeLookup lookup,vec4 path,float offset) {
    if(lookup.radial)return path.x+path.y*offset;
    // The null ODE gives all local derivatives. Fourth order supplies the two
    // nearby Gauss radii without eight extra texture fetches per flow cell.
    float u=path.x,v=path.y,a=-u+1.5*u*u,j=(-1.0+3.0*u)*v,s=3.0*v*v+(-1.0+3.0*u)*a;
    return u+offset*(v+offset*(a*.5+offset*(j/6.0+offset*s/24.0)));
}
float volumeExtinctionRate(float u,float impact,float orbitNormal,float m) {
    u=clamp(u,.015625,.952381);
    float profile=discHeightProfile(m/DiscThickness),comoving=1.0;
    if(DiscPhysical>.5) {
        float speed=inversesqrt(2.0*(1.0/u-1.0));
        float mu=clamp(-impact*u*sqrt(1.0-u)*orbitNormal/sqrt(max(1.0-m*m,1e-6)),-1.0,1.0);
        comoving=(1.0-speed*mu)*inversesqrt(1.0-speed*speed);
    }
    return .4*profile*comoving/(DiscThickness*impact*u*sqrt(1.0-u));
}
vec4 volumeShell(VolumeLookup lookup) {
    if(lookup.radial) {
        float u=1.0/DiscObserverRadius,b=lookup.a.z;
        if(lookup.a.y>0.0)return vec4(b*max(1.0/9.35-u,0.0),b*max(1.0/4.63-u,0.0),lookup.a.x,lookup.a.x);
        return vec4(0,0,b*max(u-1.0/4.63,0.0),b*max(u-1.0/9.35,0.0));
    }
    int x=lookup.x,row=lookup.branch*DISC_STRIDE+DISC_STRIDE-1;
    return mix(texelFetch(LensAzimuth,ivec2(x,row),0),texelFetch(LensAzimuth,ivec2(min(x+1,1023),row),0),lookup.blend);
}
vec4 discShell(float angle) {return volumeShell(volumeLookup(angle));}
float discHeightProfile(float height) {
    float p=max(exp(-.5*height*height)-.01110899654,0.0);return p*p/1.71761326341;
}
float volumePlaneAmplitude(vec3 radial,DiscBeamRay ray) {
    return length(vec2(dot(radial,DiscNormal),dot(ray.tangent,DiscNormal)));
}
vec2 volumeSpan(vec2 shell,float amplitude,float center) {
    if(amplitude<=DiscThickness*3.0)return shell;
    float width=asin(clamp(DiscThickness*3.0/amplitude,0.0,1.0));
    float lo=max(shell.x,center-width),hi=min(shell.y,center+width);
    return hi>=lo?vec2(lo,hi):vec2(clamp(center,shell.x,shell.y));
}
float volumeAlignedCenter(float center,float first,float otherFirst) {
    float difference=otherFirst-first;return center+.5*atan(sin(2.0*difference),cos(2.0*difference));
}
vec4 volumePoint(vec3 radial,DiscBeamRay ray,VolumeLookup lookup,float phi) {
    vec4 path=volumeTrajectory(lookup,phi);
    vec3 position=radial*cos(phi)+ray.tangent*sin(phi);
    return vec4(1.0/clamp(path.x,.015625,.952381),atan(dot(position,DiscV),dot(position,DiscU)),path.z,
            dot(position,DiscNormal)/DiscThickness);
}
bool volumeDifferentialAllowed(DiscBeamRay ray,DiscBeamRay rx,DiscBeamRay ry) {
    float width=max(abs(rx.angle-ray.angle),abs(ry.angle-ray.angle));
    float critical=abs((ray.angle-LensShadow)-DiscShadowLow);
    float distance=min(critical,min(ray.angle,DISC_PI-ray.angle));
    return DiscDifferentials>.5 && ray.angle>.02 && ray.angle<DISC_PI-.02 && width<.01*distance;
}
vec4 volumeFootPoint(vec3 radial,DiscBeamRay ray,VolumeLookup lookup,float phi,
        vec4 centerPath,float centerAngle,float centerPhi,float q,float impact,bool differential) {
    float dphi=phi-centerPhi,dtheta=ray.angle-centerAngle;
    float du=centerPath.y*dphi+q*dtheta;
    // A bounded first-order footprint is inexpensive away from the critical
    // curve. Full neighbouring trajectories remain the fallback across the
    // curve, poles, large phase shifts or strong radius variations.
    if(!differential || abs(dphi)>.01 || abs(centerPath.y*dphi)+abs(q*dtheta)>.005*centerPath.x)
        return volumePoint(radial,ray,lookup,phi);
    float u=centerPath.x,f=1.0-u;
    float delay=centerPath.z+discTimeRate(impact,u)*dphi+impact*centerPath.y*q/(f*u*u)*dtheta;
    vec3 position=radial*cos(phi)+ray.tangent*sin(phi);
    return vec4(1.0/clamp(u+du,.015625,.952381),atan(dot(position,DiscV),dot(position,DiscU)),delay,
            dot(position,DiscNormal)/DiscThickness);
}
// Three correlated scales in a periodic, vertically varying flow. The volume
// does not need the thin surface's fourth noise layer at every integration cell.
vec3 volumeField(float r,float polar,float height,float age,float generation,
        vec3 radiusSlope,vec3 polarSlope,vec3 timeSlope,vec3 heightSlope) {
    float omega=inversesqrt(2.0*r*r*r),phase=polar-omega*age;
    vec2 p=vec2(cos(phase),sin(phase));
    vec3 phaseSlope=polarSlope-omega*timeSlope+1.5*omega*age/r*radiusSlope;
    vec2 px=vec2(-p.y,p.x)*phaseSlope.x,py=vec2(-p.y,p.x)*phaseSlope.y,pz=vec2(-p.y,p.x)*phaseSlope.z;
    vec3 heightWarp=vec3(.35,.90,.55);
    vec3 seed=vec3(17.8,41.2,6.3)+vec3(5.7,13.1,3.9)*generation+heightWarp*height;
    vec3 sx=heightWarp*heightSlope.x,sy=heightWarp*heightSlope.y,sz=heightWarp*heightSlope.z;
    vec3 broadSlope,ribbonSlope;
    float broad=volumeFilteredNoise(vec3(r*.65,p*3.3)+seed,
            vec3(radiusSlope.x*.65,px*3.3)+sx,vec3(radiusSlope.y*.65,py*3.3)+sy,
            vec3(radiusSlope.z*.65,pz*3.3)+sz,broadSlope);
    vec3 warped=radiusSlope*2.7+broadSlope*3.3;
    float ribbon=volumeFilteredNoise(vec3(r*2.7+(broad-.5)*3.3,p*7.3)+seed+37.1,
            vec3(warped.x,px*7.3)+sx,vec3(warped.y,py*7.3)+sy,vec3(warped.z,pz*7.3)+sz,ribbonSlope);
    float fine=.5;
    if(FieldDetail>.5) {
        vec3 fineRadius=radiusSlope*9.7+ribbonSlope*5.0;
        vec3 dx=vec3(fineRadius.x,px*17.3)+sx,dy=vec3(fineRadius.y,py*17.3)+sy,dz=vec3(fineRadius.z,pz*17.3)+sz;
        float widthSquared=max(max(dot(dx,dx),dot(dy,dy)),dot(dz,dz));
        // Beyond this footprint, the residual is smaller than a float32 ulp
        // around .5. Its gradient is unused, so omit the detail lookup exactly
        // where the filtered value rounds to the mean anyway.
        if(widthSquared<=36.0)fine=mix(.5,volumeNoiseGradient(vec3(r*9.7+(ribbon-.5)*5.0,p*17.3)+seed+71.3).w,exp(-.6*widthSquared));
    }
    return vec3(broad,ribbon,fine);
}
vec3 volumeMaterial(float r,float height,vec3 flow) {
    float ribbon=max((flow.y-.16)/.84,0.0);
    float density=clamp(.006+6.0*pow(ribbon,3.0)*pow(flow.x,1.3)+2.0*pow(flow.z,5.0)*flow.x,.006,2.0);
    // Cool interstitial gas can absorb the far layer without becoming an
    // equally bright thermal sheet. Dense filaments supply the warm-white
    // highlights; keep density (extinction) separate from source temperature.
    float temperature=clamp(3350.0*pow(4.63/r,.25)+2600.0*smoothstep(.15,.95,density)
            +(flow.x-.5)*1700.0,2100.0,6900.0);
    temperature*=1.0-.08*min(height*height/9.0,1.0);
    float envelope=smoothstep(4.63,4.71,r)*(1.0-smoothstep(9.2,9.35,r));
    return vec3(density,temperature,envelope);
}
vec4 volumeFlow(float r,float polar,float height,float time,float frequency,float opticalPath,
        vec3 radiusSlope,vec3 polarSlope,vec3 timeSlope,vec3 heightSlope) {
    const float lifetime=70.0;
    float ageA=mod(time,lifetime),ageB=mod(time+lifetime*.5,lifetime),weight=pow(sin(DISC_PI*ageA/lifetime),2.0);
    vec3 a=volumeMaterial(r,height,volumeField(r,polar,height,ageA,floor(time/lifetime)*2.0,
            radiusSlope,polarSlope,timeSlope,heightSlope));
    vec3 b=volumeMaterial(r,height,volumeField(r,polar,height,ageB,floor((time+lifetime*.5)/lifetime)*2.0+1.0,
            radiusSlope,polarSlope,timeSlope,heightSlope));
    vec4 transfer=mix(discGenerationRadiance(b,frequency,opticalPath),discGenerationRadiance(a,frequency,opticalPath),weight);
    // The column normalization differs from the historical thin layer. Keep
    // this exposure calibration in radiance, leaving extinction untouched.
    transfer.rgb*=.65;return transfer;
}
// Homogeneous mode omits the stochastic material and returns an independently
// testable optical depth alongside constant-source transfer. Production uses
// explicit x/y footprints plus a third footprint along the integration cell.
vec4 discVolumeSample(vec3 radial,DiscBeamRay ray,DiscBeamRay rx,DiscBeamRay ry,bool homogeneous,out float opticalDepth) {
    VolumeLookup lookup=volumeLookup(ray.angle),lookupX=volumeLookup(rx.angle),lookupY=volumeLookup(ry.angle);
    vec4 ranges=volumeShell(lookup),rangesX=volumeShell(lookupX),rangesY=volumeShell(lookupY);
    float A=dot(radial,DiscNormal),B=dot(ray.tangent,DiscNormal),amplitude=length(vec2(A,B));
    // The table's bounded radial limit remains finite when projection rounds
    // acos to exactly 0/pi. Its stored impact belongs to the queried trajectory.
    float impact=max(mix(lookup.a.z,lookup.b.z,lookup.blend),1e-12);
    float orbitNormal=dot(cross(radial,ray.tangent),DiscNormal);
    float ampX=volumePlaneAmplitude(radial,rx),ampY=volumePlaneAmplitude(radial,ry),support=DiscThickness*3.0;
    bool differential=volumeDifferentialAllowed(ray,rx,ry);
    bool full=amplitude<=support;float halfWidth=asin(clamp(support/max(amplitude,1e-9),0.0,1.0));
    vec3 color=vec3(0);float transmission=1.0;opticalDepth=0.0;
    int steps=int(clamp(DiscVolumeSteps,2.0,32.0));
    for(int portion=0;portion<2;portion++) {
        vec2 shell=portion==0?ranges.xy:ranges.zw,shellX=portion==0?rangesX.xy:rangesX.zw,shellY=portion==0?rangesY.xy:rangesY.zw;
        if(shell.y-shell.x<1e-8)continue;
        float firstOrder=ceil((shell.x-halfWidth-ray.first)/DISC_PI);
        for(int window=0;window<3;window++) {
            if(full && window>0)break;
            float center=ray.first+(firstOrder+float(window))*DISC_PI;
            vec2 span=volumeSpan(shell,amplitude,center);if(span.y-span.x<1e-8)continue;
            vec2 spanX=volumeSpan(shellX,ampX,volumeAlignedCenter(center,ray.first,rx.first));
            vec2 spanY=volumeSpan(shellY,ampY,volumeAlignedCenter(center,ray.first,ry.first));
            float delta=(span.y-span.x)/float(steps);
            float offset=delta*.288675134595,cosOffset=cos(offset),sinOffset=sin(offset);
            for(int i=0;i<32;i++) {
                if(i>=steps)break;
                float fraction=(float(i)+.5)/float(steps),phi=mix(span.x,span.y,fraction);
                float q;vec4 path=volumeTrajectory(lookup,phi,q);float u=clamp(path.x,.015625,.952381),r=1.0/u;
                if(path.w<.5 || r<4.63 || r>9.35)continue;
                vec3 position=radial*cos(phi)+ray.tangent*sin(phi),angular=-radial*sin(phi)+ray.tangent*cos(phi);
                float m=dot(position,DiscNormal),height=m/DiscThickness,profile=discHeightProfile(height);
                if(profile<=0.0)continue;
                float frequency=1.0;
                if(DiscPhysical>.5) {
                    float mu=clamp(-impact*u*sqrt(1.0-u)*orbitNormal/sqrt(max(1.0-m*m,1e-6)),-1.0,1.0);
                    frequency=discFrequency(r,mu);
                }
                // Two-point Gauss integrates the smooth height/metric column
                // inside each cell. Expensive flow/source evaluation remains
                // at the cell centre with an explicit along-cell footprint.
                float mShift=dot(angular,DiscNormal)*sinOffset;
                float opticalCell=delta*.5*(volumeExtinctionRate(volumeLocalU(lookup,path,-offset),impact,orbitNormal,m*cosOffset-mShift)
                        +volumeExtinctionRate(volumeLocalU(lookup,path,offset),impact,orbitNormal,m*cosOffset+mShift));
                float tau=.30*opticalCell;opticalDepth+=tau;
                float polar=atan(dot(position,DiscV),dot(position,DiscU));
                vec4 emitted;
                if(homogeneous || DiscDiagnostic>.5) {
                    float alpha=1.0-exp(-tau);
                    vec3 source=vec3(1);
                    if(!homogeneous) {
                        float sector=floor(mod(polar+DISC_PI,2.0*DISC_PI)/(2.0*DISC_PI)*12.0);
                        float band=floor((r-4.63)/(9.35-4.63)*6.0);
                        source=mix(vec3(.042,.49,.7),vec3(.7,.21,.028),mod(sector+band,2.0));
                    }
                    emitted=vec4(source*alpha,alpha);
                } else {
                    vec4 hx=volumeFootPoint(radial,rx,lookupX,mix(spanX.x,spanX.y,fraction),path,ray.angle,phi,q,impact,differential);
                    vec4 hy=volumeFootPoint(radial,ry,lookupY,mix(spanY.x,spanY.y,fraction),path,ray.angle,phi,q,impact,differential);
                    vec2 polarDifference=vec2(hx.y-polar,hy.y-polar);
                    vec2 polarFoot=atan(sin(polarDifference),cos(polarDifference));
                    vec2 planar=vec2(dot(position,DiscU),dot(position,DiscV)),planarRate=vec2(dot(angular,DiscU),dot(angular,DiscV));
                    float polarRate=(planar.x*planarRate.y-planar.y*planarRate.x)/max(dot(planar,planar),1e-6);
                    vec3 radiusSlope=vec3(hx.x-r,hy.x-r,-path.y/(u*u)*delta);
                    vec3 polarSlope=vec3(polarFoot,polarRate*delta);
                    vec3 timeSlope=-vec3(hx.z-path.z,hy.z-path.z,discTimeRate(impact,u)*delta);
                    vec3 heightSlope=vec3(hx.w-height,hy.w-height,dot(angular,DiscNormal)/DiscThickness*delta);
                    emitted=volumeFlow(r,polar,height,DiscTime-path.z,frequency,opticalCell,
                            radiusSlope,polarSlope,timeSlope,heightSlope);
                }
                color+=transmission*emitted.rgb;transmission*=1.0-emitted.a;
            }
        }
    }
    return vec4(color,transmission);
}
vec4 discVolumeImageMode(vec3 radial,vec3 direction,vec3 dx,vec3 dy,bool homogeneous) {
    float tau;
    return discVolumeSample(radial,discBeamRay(radial,direction),discBeamRay(radial,normalize(direction+dx)),
            discBeamRay(radial,normalize(direction+dy)),homogeneous,tau);
}
vec4 discVolumeImage(vec3 radial,vec3 direction,vec3 dx,vec3 dy) {
    return discVolumeImageMode(radial,direction,dx,dy,false);
}
vec4 discVolumeBeamMode(vec3 radial,vec3 direction,vec3 dx,vec3 dy,bool homogeneous) {
    int grid=int(clamp(DiscBeamGrid,1.0,8.0));float cell=1.0/float(grid);vec4 result=vec4(0);
    for(int y=0;y<8;y++)for(int x=0;x<8;x++) {
        if(x>=grid || y>=grid)continue;
        vec2 offset=(vec2(x,y)+.5)*cell-.5;
        result+=discVolumeImageMode(radial,normalize(direction+dx*offset.x+dy*offset.y),dx*cell,dy*cell,homogeneous)*cell*cell;
    }
    return result;
}
vec4 discVolumeBeam(vec3 radial,vec3 direction,vec3 dx,vec3 dy) {
    return discVolumeBeamMode(radial,direction,dx,dy,false);
}
