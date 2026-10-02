#version 150
uniform sampler2D Sampler0,LensAzimuth;
uniform float FieldDetail,LensShadow,LensLogRange,BakeLevel,BakeGeneration;
uniform vec3 BakeSize,PreviousSize;
uniform vec2 ScratchSize;
#moj_import <starboundmc:space_common.glsl>
#moj_import <starboundmc:space_background_field.glsl>
#moj_import <starboundmc:space_schwarzschild_disc.glsl>
#moj_import <starboundmc:space_disc_medium.glsl>
out vec4 fragColor;
void main() {
    ivec2 pixel=ivec2(gl_FragCoord.xy);int tile=int(BakeSize.x)+2,rows=int(BakeSize.y);
    float slice=float(pixel.x/tile);
    float i=float(clamp(pixel.x%tile-1,0,int(BakeSize.x)-1));
    float j=float((pixel.y-1+rows)%rows);
    if(BakeLevel<.5) {
        // Eight fixed stratified points per base voxel. Average j and kappa,
        // not noise, temperature, source colour, or path-dependent opacity.
        vec4 sum=vec4(0);
        for(int z=0;z<2;z++)for(int y=0;y<2;y++)for(int x=0;x<2;x++) {
            vec3 f=(vec3(i,j,slice)+(vec3(x,y,z)+.5)*.5)/BakeSize;
            sum+=volumeCoefficient(mix(4.63,9.35,f.x),f.y*2.0*DISC_PI,mix(-3.0,3.0,f.z),BakeGeneration);
        }
        fragColor=sum*.125;return;
    }
    // Each hardware bilinear sample averages four parent xy coefficients;
    // two height charts complete the eight-voxel box reduction. Mip charts
    // get their own clamped radial and periodic azimuth guard texels.
    float px=PreviousSize.x>1.0?2.0*i+2.0:1.5;
    float py=PreviousSize.y>1.0?2.0*j+2.0:1.5;
    float z0=min(slice*2.0,PreviousSize.z-1.0),z1=min(z0+1.0,PreviousSize.z-1.0);
    vec4 a=textureLod(Sampler0,vec2(z0*(PreviousSize.x+2.0)+px,py)/ScratchSize,0.0);
    vec4 b=textureLod(Sampler0,vec2(z1*(PreviousSize.x+2.0)+px,py)/ScratchSize,0.0);
    fragColor=(a+b)*.5;
}
