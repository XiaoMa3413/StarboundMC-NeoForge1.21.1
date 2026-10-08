// Thin cloud-layer approximation. The authored alpha is its normal-incidence
// coverage; oblique paths integrate the same optical depth without a hard rim.
float spaceCloudOpticalDepth(float coverage, float opacity) {
    return -log(max(1.0-clamp(coverage*opacity,0.0,1.0),.01));
}
float spaceCloudPath(float cosine) {
    // Curvature limits the plane-parallel grazing path instead of diverging.
    return inversesqrt(cosine*cosine+.02);
}
float spaceCloudTransmission(float depth, float cosine) {
    return exp(-depth*spaceCloudPath(cosine));
}
float spaceCloudPhase(float cosine) {
    const float G = .7;
    return (1.0-G*G)/(12.5663706*pow(max(1.0+G*G-2.0*G*cosine,.001),1.5));
}
