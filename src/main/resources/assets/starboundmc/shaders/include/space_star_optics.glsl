// Pixel-integrated, unit-flux point spread. Shared by direct stars and future ray sampling.
float spaceStarErf(float x) {
    float t = 1.0 / (1.0 + .3275911*abs(x));
    float polynomial = (((((1.061405429*t-1.453152027)*t)+1.421413741)*t-.284496736)*t+.254829592)*t;
    return sign(x)*(1.0-polynomial*exp(-x*x));
}

float spaceStarGaussianPixel(vec2 offset, float sigma) {
    vec2 a=(offset-vec2(.5))/(1.41421356237*sigma);
    vec2 b=(offset+vec2(.5))/(1.41421356237*sigma);
    vec2 integral=.5*vec2(spaceStarErf(b.x)-spaceStarErf(a.x),spaceStarErf(b.y)-spaceStarErf(a.y));
    return max(0.0,integral.x*integral.y);
}

float spaceStarWing(float flux) { return .035*smoothstep(.15,3.0,flux); }

float spaceStarPixelFlux(vec2 offset, float sigma, float flux) {
    return flux*mix(spaceStarGaussianPixel(offset,sigma),spaceStarGaussianPixel(offset,1.35),spaceStarWing(flux));
}
