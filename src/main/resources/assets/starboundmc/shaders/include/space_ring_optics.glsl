// Authored normal coverage becomes optical depth, not a view-independent alpha.
float spaceRingOpticalDepth(float coverage) {
    return -log(max(1.0-clamp(coverage,0.0,1.0)*.9,.01));
}
float spaceRingCosine(float mu) {
    // Finite thickness/particle size regularizes an exactly edge-on sheet.
    return sqrt(mu*mu+.0004);
}
float spaceRingPhase(float cosine, float g) {
    return (1.0-g*g)/(12.5663706*pow(max(.001,1.0+g*g-2.0*g*cosine),1.5));
}
float spaceRingSingleScatter(float tau, float mu, float mu0, bool reflection) {
    float a=1.0/spaceRingCosine(mu), b=1.0/spaceRingCosine(mu0);
    if (reflection) return a/(a+b)*(1.0-exp(-tau*(a+b)));
    float x=tau*abs(a-b);
    // Continuous equal-angle limit; avoids subtracting almost equal exponentials.
    float integral=x < .001 ? 1.0-x*.5+x*x/6.0 : (1.0-exp(-x))/x;
    return a*tau*exp(-tau*min(a,b))*integral;
}
float spaceRingSolarTransmission(sampler2D ringMap, vec3 point, vec3 sun,
                                 float inner, float outer) {
    if (abs(sun.y) < .00001) return 1.0;
    float t=-point.y/sun.y;
    if (t <= 0.0) return 1.0;
    float radius=length((point+sun*t).xz);
    if (radius <= inner || radius >= outer) return 1.0;
    ivec2 size=textureSize(ringMap,0);
    float pixel=(radius-inner)/(outer-inner)*float(size.x)-.5;
    int x=int(floor(pixel)), y=size.y/2;
    float coverage=mix(texelFetch(ringMap,ivec2(clamp(x,0,size.x-1),y),0).a,
                       texelFetch(ringMap,ivec2(clamp(x+1,0,size.x-1),y),0).a,fract(pixel));
    return exp(-spaceRingOpticalDepth(coverage)/spaceRingCosine(sun.y));
}
