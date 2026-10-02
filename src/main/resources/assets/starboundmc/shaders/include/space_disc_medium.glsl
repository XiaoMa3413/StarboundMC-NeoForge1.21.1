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
// Per normalized optical column: emissivity j = kappa * source.
// Nonlinear density/thermal responses happen before any spatial averaging.
vec4 volumeCoefficient(float r,float polar,float height,float generation) {
    vec3 m=volumeMaterial(r,height,volumeField(r,polar,height,0.0,generation,
            vec3(0),vec3(0),vec3(0),vec3(0)));
    float kappa=m.x*m.z;
    return vec4(discSpectrum(m.y)*3.25*kappa,kappa);
}
