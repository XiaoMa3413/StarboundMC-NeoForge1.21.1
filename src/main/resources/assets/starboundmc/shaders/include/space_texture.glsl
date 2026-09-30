// Celestial filtering is local to these shaders, preserving Minecraft texture/sampler state.
// Longitude wraps, latitude clamps; color is decoded BEFORE interpolation, masks remain linear.
vec4 spaceTexel(sampler2D image, ivec2 p, ivec2 size, bool colorData) {
    p = ivec2((p.x % size.x + size.x) % size.x, clamp(p.y,0,size.y-1));
    vec4 value = texelFetch(image,p,0);
    if (colorData) value.rgb = spaceToLinear(value.rgb);
    return value;
}
vec4 spaceBilinear(sampler2D image, vec2 uv, bool colorData) {
    ivec2 size = textureSize(image,0);
    vec2 pixel = vec2(fract(uv.x),clamp(uv.y,0.0,1.0))*vec2(size)-.5;
    ivec2 p = ivec2(floor(pixel));
    vec2 f = fract(pixel);
    return mix(mix(spaceTexel(image,p,size,colorData),spaceTexel(image,p+ivec2(1,0),size,colorData),f.x),
               mix(spaceTexel(image,p+ivec2(0,1),size,colorData),spaceTexel(image,p+ivec2(1,1),size,colorData),f.x),f.y);
}
