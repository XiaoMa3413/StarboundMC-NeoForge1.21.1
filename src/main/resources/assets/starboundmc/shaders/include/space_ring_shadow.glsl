#moj_import <starboundmc:space_ring_optics.glsl>
uniform sampler2D RingOpticalDepth;
uniform float RingShadowEnabled;
uniform vec2 RingRadii;
uniform vec3 RingFrameX;
uniform vec3 RingFrameY;
uniform vec3 RingFrameZ;
vec3 spaceToRingFrame(vec3 p) { return RingFrameX*p.x+RingFrameY*p.y+RingFrameZ*p.z; }
float spaceRingShadow(vec3 point, vec3 sun) {
    if (RingShadowEnabled <= 0.0) return 1.0;
    vec3 p=spaceToRingFrame(point), s=spaceToRingFrame(sun);
    vec3 tangent=normalize(cross(s,abs(s.y) < .95 ? vec3(0,1,0) : vec3(1,0,0)));
    vec3 bitangent=cross(s,tangent);
    float transmission=spaceRingSolarTransmission(RingOpticalDepth,p,s,RingRadii.x,RingRadii.y);
    transmission+=spaceRingSolarTransmission(RingOpticalDepth,p,normalize(s+bitangent*SPACE_SUN_ANGLE*.7),RingRadii.x,RingRadii.y);
    transmission+=spaceRingSolarTransmission(RingOpticalDepth,p,normalize(s+(-.5*bitangent+.8660254*tangent)*SPACE_SUN_ANGLE*.7),RingRadii.x,RingRadii.y);
    transmission+=spaceRingSolarTransmission(RingOpticalDepth,p,normalize(s+(-.5*bitangent-.8660254*tangent)*SPACE_SUN_ANGLE*.7),RingRadii.x,RingRadii.y);
    return mix(1.0,transmission*.25,clamp(RingShadowEnabled,0.0,1.0));
}
