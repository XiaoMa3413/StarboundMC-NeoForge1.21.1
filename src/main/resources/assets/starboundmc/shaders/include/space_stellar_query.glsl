// Keep this cube convention, grid and texture packing in sync with SkyCube / StellarBackground.
uniform sampler2D SkyCells;
uniform sampler2D SkyIndices;
uniform sampler2D SkySources;
uniform sampler2D SkyFlux;
const int SKY_GRID = 32;
const int SKY_FLUX_SIZE = 128;
const float SKY_SUPPORT2 = 20.25;

vec3 skyCubeCoordinate(vec3 d) {
    vec3 a=abs(d);
    if(a.x>=a.y && a.x>=a.z) return d.x>0.0 ? vec3(0.0,-d.z/a.x,d.y/a.x) : vec3(1.0,d.z/a.x,d.y/a.x);
    if(a.y>=a.z) return d.y>0.0 ? vec3(2.0,d.x/a.y,-d.z/a.y) : vec3(3.0,d.x/a.y,d.z/a.y);
    return d.z>0.0 ? vec3(4.0,d.x/a.z,d.y/a.z) : vec3(5.0,-d.x/a.z,d.y/a.z);
}
vec3 skyCubeDirection(int face,vec2 uv) {
    if(face==0)return normalize(vec3(1.0,uv.y,-uv.x));
    if(face==1)return normalize(vec3(-1.0,uv.y,uv.x));
    if(face==2)return normalize(vec3(uv.x,1.0,-uv.y));
    if(face==3)return normalize(vec3(uv.x,-1.0,uv.y));
    if(face==4)return normalize(vec3(uv.x,uv.y,1.0));
    return normalize(vec3(-uv.x,uv.y,-1.0));
}
ivec2 skyDataPixel(int index) { return ivec2(index%1024,index/1024); }
vec4 skyFluxTexel(int level,int face,ivec2 pixel) {
    int n=SKY_FLUX_SIZE>>level;
    int row=6*(2*SKY_FLUX_SIZE-2*n);
    if(any(lessThan(pixel,ivec2(0))) || any(greaterThanEqual(pixel,ivec2(n)))) {
        vec3 mapped=skyCubeCoordinate(skyCubeDirection(face,2.0*(vec2(pixel)+0.5)/float(n)-1.0));
        face=int(mapped.x);pixel=ivec2(clamp(floor((mapped.yz+1.0)*0.5*float(n)),vec2(0),vec2(n-1)));
    }
    return texelFetch(SkyFlux,ivec2(pixel.x,row+face*n+pixel.y),0);
}
vec4 skyFluxLevel(vec3 cube,int level) {
    int n=SKY_FLUX_SIZE>>level;vec2 p=(cube.yz+1.0)*0.5*float(n)-0.5;
    ivec2 cell=ivec2(floor(p));vec2 t=fract(p);int face=int(cube.x);
    return mix(mix(skyFluxTexel(level,face,cell),skyFluxTexel(level,face,cell+ivec2(1,0)),t.x),
               mix(skyFluxTexel(level,face,cell+ivec2(0,1)),skyFluxTexel(level,face,cell+ivec2(1,1)),t.x),t.y);
}
vec3 skyWideStars(vec3 direction,float sigma,vec3 transmission) {
    float lod=clamp(log2(max(sigma*float(SKY_FLUX_SIZE),1.0)),0.0,7.0);
    int level=int(floor(lod));vec3 cube=skyCubeCoordinate(direction);
    vec4 flux=mix(skyFluxLevel(cube,level),skyFluxLevel(cube,min(level+1,7)),fract(lod));
    return flux.rgb*mix(vec3(1),transmission,flux.a);
}

// Curved-ray derivatives form an elliptical beam in the sky tangent plane. Unresolved stars
// have an IMAGE-plane PSF, not a fixed angular disc that turns into arcs under magnification.
// The angular reference kernel is retained only for independent query validation.
vec3 skyIndexedStars(vec3 direction,vec3 dx,vec3 dy,vec3 transmission,bool imagePsf) {
    vec3 axis=abs(direction.y)<0.9 ? vec3(0,1,0) : vec3(1,0,0);
    vec3 t=normalize(cross(axis,direction)),b=cross(direction,t);
    vec2 ex=vec2(dot(dx,t),dot(dx,b)),ey=vec2(dot(dy,t),dot(dy,b));
    float a=(ex.x*ex.x+ey.x*ey.x)/12.0,c=(ex.y*ex.y+ey.y*ey.y)/12.0;
    float off=(ex.x*ex.y+ey.x*ey.y)/12.0;
    float major=sqrt(max(0.5*(a+c+sqrt((a-c)*(a-c)+4.0*off*off)),0.0));
    if(imagePsf)major*=sqrt(1.0+12.0*0.48*0.48);
    vec3 broad=skyWideStars(direction,major,transmission);
    if(major>=0.008)return broad;
    vec3 cube=skyCubeCoordinate(direction);
    ivec2 cell=ivec2(clamp(floor((cube.yz+1.0)*0.5*float(SKY_GRID)),vec2(0),vec2(SKY_GRID-1)));
    vec2 range=texelFetch(SkyCells,ivec2(cell.x,int(cube.x)*SKY_GRID+cell.y),0).rg;
    vec3 sum=vec3(0);int first=int(range.x),count=int(range.y);
    // No fixed candidate limit: the index never silently drops a source in dense star clouds.
    for(int i=0;i<count;i++) {
        int id=int(texelFetch(SkyIndices,skyDataPixel(first+i),0).r);
        vec4 source=texelFetch(SkySources,skyDataPixel(id*2),0);
        vec4 light=texelFetch(SkySources,skyDataPixel(id*2+1),0);
        vec3 delta=source.xyz-direction;
        // light.a is the catalogue's PSF at the 720p / 70-degree calibration focal length.
        float sigmaPixels=light.a*514.13328243;
        float factor=imagePsf ? 1.0+12.0*sigmaPixels*sigmaPixels : 1.0;
        float s2=imagePsf ? 1e-16 : light.a*light.a;
        float aa=a*factor+s2,cc=c*factor+s2,xy=off*factor,det=max(aa*cc-xy*xy,1e-24);
        if(dot(delta,delta)>SKY_SUPPORT2*(major*major+s2))continue;
        vec2 offset=vec2(dot(delta,t),dot(delta,b));
        float exponent=(cc*offset.x*offset.x-2.0*xy*offset.x*offset.y+aa*offset.y*offset.y)/det;
        if(exponent>SKY_SUPPORT2)continue;
        float kernel=exp(-0.5*exponent)/(6.28318530718*sqrt(det));
        sum+=light.rgb*kernel*mix(vec3(1),transmission,source.a);
    }
    return mix(sum,broad,smoothstep(0.005,0.008,major));
}
