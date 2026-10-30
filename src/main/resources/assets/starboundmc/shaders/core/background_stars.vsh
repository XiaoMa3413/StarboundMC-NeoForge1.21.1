#version 150
// Derived from StellarView HDR2, Copyright (c) 2024 Povstalec, MIT.
// See third_party/stellarview/LICENSE.txt. Circular profile is tangent-rotation invariant.
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ObserverOffset;
uniform vec3 ConvergenceAxis;
uniform float Convergence;
uniform float StarAlpha;
uniform vec2 ViewportSize;
out vec2 externalUv;
out vec4 vertexColor;
void main() {
    int corner = gl_VertexID % 4;
    externalUv = corner == 0 ? vec2(-1,-1) : corner == 1 ? vec2(-1,1)
            : corner == 2 ? vec2(1,1) : vec2(1,-1);
    vec3 xyz = Position - ObserverOffset;
    float distance = length(xyz);
    if (distance <= 0.0 || distance > UV0.y) {
        gl_Position = vec4(0,0,2,1);
        vertexColor = vec4(0);
        return;
    }
    float starSize = max(UV0.x*(1.0-distance/1000000.0),.01);
    float alpha = Color.a;
    vec4 clip = ProjMat * ModelViewMat * vec4(normalize(xyz)*100.0,1.0);
    float pixels = starSize*abs(ProjMat[1][1])*ViewportSize.y / max(2.0*abs(clip.w),.001);
    float radius = max(pixels,.7);
    alpha *= min(1.0,pixels*pixels/(radius*radius));
    starSize *= radius/max(pixels,.0001);
    xyz /= distance;
    if (Convergence > 0.0) {
        vec3 axis = normalize(ConvergenceAxis);
        float axial = dot(xyz,axis);
        float squeeze = 1.0-.82*clamp(Convergence,0.0,1.0)*smoothstep(-.05,.9,axial);
        xyz = normalize(axis*axial+(xyz-axis*axial)*squeeze);
    }
    float theta = dot(xyz.xz,xyz.xz) > 1e-12 ? atan(xyz.x,xyz.z) : 0.0;
    float sinTheta = sin(theta), cosTheta = cos(theta);
    float phi = atan(length(xyz.xz),xyz.y);
    float height = externalUv.x*starSize, width = externalUv.y*starSize;
    float heightY = height*sin(phi), heightXZ = -height*cos(phi);
    vec3 tangent = vec3(heightXZ*sinTheta-width*cosTheta,heightY,width*sinTheta+heightXZ*cosTheta);
    gl_Position = ProjMat * ModelViewMat * vec4(xyz*100.0+tangent,1.0);
    gl_Position.z = gl_Position.w;
    vertexColor = vec4(Color.rgb,alpha*StarAlpha);
}
