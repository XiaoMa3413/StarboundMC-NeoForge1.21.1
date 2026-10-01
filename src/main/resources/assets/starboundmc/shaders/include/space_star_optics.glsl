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

// Unresolved stars enlarge more slowly than the scene, retaining a focused core.
float spaceStarImageScale(float magnification) { return sqrt(max(1.0,magnification)); }

float spaceStarPixelFlux(vec2 offset, float sigma, float flux, float magnification) {
    // Magnify the optical image, retaining its radiance instead of spreading a
    // fixed flux into a larger, progressively darker disc. Pixel integration
    // still operates on the actual output pixel footprint.
    float scale=spaceStarImageScale(magnification);
    return flux*scale*scale*mix(spaceStarGaussianPixel(offset,sigma*scale),
            spaceStarGaussianPixel(offset,1.35*scale),spaceStarWing(flux));
}
