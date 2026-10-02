uniform sampler2D DiscCoefficients;
uniform float DiscCacheEnabled;
uniform vec3 DiscCacheSize;
uniform vec4 DiscCacheAtlas,DiscCacheKeys0,DiscCacheKeys1,DiscCacheGains0,DiscCacheGains1;

vec3 coefficientSize(float level) {return max(vec3(1),floor(DiscCacheSize*exp2(-level)));}
float coefficientOffset(float level) {
    // All retained azimuth levels are powers of two >= 2. The exact geometric
    // sum avoids walking preceding charts at every volume material sample.
    return 2.0*DiscCacheSize.y*(1.0-exp2(-level))+2.0*level;
}
vec4 coefficientChart(vec3 coordinate,float level,float bank) {
    vec3 size=coefficientSize(level);
    float height=clamp(coordinate.z*size.z-.5,0.0,size.z-1.0),z=floor(height);
    float radius=clamp(coordinate.x*size.x-.5,0.0,size.x-1.0);
    float phase=coordinate.y*size.y-.5;
    int y=int(floor(phase))+1+int(bank*DiscCacheAtlas.z+coefficientOffset(level));
    int xa=int(z*(size.x+2.0))+int(floor(radius))+1;
    int xb=int(min(z+1.0,size.z-1.0)*(size.x+2.0))+int(floor(radius))+1;
    // Subtexel weights of hardware filtering can step during advection.
    // Integer fetches and float32 interpolation also avoid normalized-coordinate
    // rounding at a row centre, retaining continuous radial/height footprints.
    vec4 a=mix(mix(texelFetch(DiscCoefficients,ivec2(xa,y),0),texelFetch(DiscCoefficients,ivec2(xa+1,y),0),fract(radius)),
            mix(texelFetch(DiscCoefficients,ivec2(xa,y+1),0),texelFetch(DiscCoefficients,ivec2(xa+1,y+1),0),fract(radius)),fract(phase));
    if(size.z<=1.0)return a;
    vec4 b=mix(mix(texelFetch(DiscCoefficients,ivec2(xb,y),0),texelFetch(DiscCoefficients,ivec2(xb+1,y),0),fract(radius)),
            mix(texelFetch(DiscCoefficients,ivec2(xb,y+1),0),texelFetch(DiscCoefficients,ivec2(xb+1,y+1),0),fract(radius)),fract(phase));
    return mix(a,b,fract(height));
}
vec4 coefficientGeneration(float r,float polar,float height,float age,float generation,
        vec3 radiusSlope,vec3 polarSlope,vec3 timeSlope,vec3 heightSlope) {
    float bank=mod(generation,8.0),key=bank<4.0?DiscCacheKeys0[int(bank)]:DiscCacheKeys1[int(bank)-4];
    float gain=bank<4.0?DiscCacheGains0[int(bank)]:DiscCacheGains1[int(bank)-4];
    float omega=inversesqrt(2.0*r*r*r),phase=polar-omega*age;
    vec4 fallback=vec4(0);
    if(key!=generation || gain<1.0) {
        // A generation still being built or a very long delayed ray retains
        // the same nonlinear coefficient closure, never an unrelated atlas bank.
        fallback=volumeCoefficient(r,phase,height,generation);
        if(key!=generation)return fallback;
    }
    vec3 phaseSlope=polarSlope-omega*timeSlope+1.5*omega*age/r*radiusSlope;
    vec3 scaledR=radiusSlope*(DiscCacheSize.x/(9.35-4.63));
    vec3 scaledP=phaseSlope*(DiscCacheSize.y/(2.0*DISC_PI));
    vec3 scaledH=heightSlope*(DiscCacheSize.z/6.0);
    float width=max(max(length(vec3(scaledR.x,scaledP.x,scaledH.x)),length(vec3(scaledR.y,scaledP.y,scaledH.y))),
            length(vec3(scaledR.z,scaledP.z,scaledH.z)));
    float lod=clamp(log2(max(width,1.0)),0.0,DiscCacheAtlas.w),level=floor(lod);
    vec3 coordinate=vec3((r-4.63)/(9.35-4.63),fract(phase/(2.0*DISC_PI)),(height+3.0)/6.0);
    vec4 value=coefficientChart(coordinate,level,bank);
    if(fract(lod)>0.0)value=mix(value,coefficientChart(coordinate,min(level+1.0,DiscCacheAtlas.w),bank),fract(lod));
    return mix(fallback,value,gain);
}
vec4 volumeCachedCoefficients(float r,float polar,float height,float time,
        vec3 radiusSlope,vec3 polarSlope,vec3 timeSlope,vec3 heightSlope) {
    float ageA=mod(time,70.0),ageB=mod(time+35.0,70.0),weight=pow(sin(DISC_PI*ageA/70.0),2.0);
    vec4 a=coefficientGeneration(r,polar,height,ageA,2.0*floor(time/70.0),radiusSlope,polarSlope,timeSlope,heightSlope);
    vec4 b=coefficientGeneration(r,polar,height,ageB,2.0*floor((time+35.0)/70.0)+1.0,radiusSlope,polarSlope,timeSlope,heightSlope);
    return mix(b,a,weight);
}
