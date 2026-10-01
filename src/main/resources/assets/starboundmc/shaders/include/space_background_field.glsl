// Linear radiance by universe direction. The footprint is supplied by the ray
// sampler so a future curved-ray renderer can filter the same stellar field.
float spaceBackgroundHash(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}
float spaceBackgroundNoise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(spaceBackgroundHash(i), spaceBackgroundHash(i + vec3(1,0,0)), f.x),
                   mix(spaceBackgroundHash(i + vec3(0,1,0)), spaceBackgroundHash(i + vec3(1,1,0)), f.x), f.y),
               mix(mix(spaceBackgroundHash(i + vec3(0,0,1)), spaceBackgroundHash(i + vec3(1,0,1)), f.x),
                   mix(spaceBackgroundHash(i + vec3(0,1,1)), spaceBackgroundHash(i + vec3(1,1,1)), f.x), f.y), f.z);
}

float spaceBackgroundDustDetail(vec3 p) {
    return spaceBackgroundNoise(p)*.5 + spaceBackgroundNoise(p*2.37+11.7)*.3
            + spaceBackgroundNoise(p*5.91+37.1)*.2;
}
// A periodic spherical catalogue of subpixel stars, filtered analytically.
// Integrating each Gaussian over the pixel footprint preserves mean radiance
// when resizing; this grain belongs to the sky, rather than to screen pixels.
float spaceBackgroundStellarGrain(float longitude, float latitude, float footprint) {
    const float PI = 3.14159265359;
    const float COLUMNS = 4096.0;
    const float SIGMA = .13;
    const float MEAN = 2.0 * PI * SIGMA * SIGMA * .4375 * .45;
    float pixelSigma = footprint * (COLUMNS / (2.0 * PI)) * .42;
    float variance = SIGMA * SIGMA + pixelSigma * pixelSigma;
    float fade = 1.0 - smoothstep(.45, .8, sqrt(variance));
    if (fade <= 0.0) return 1.0;
    vec2 p = vec2(longitude / (2.0 * PI) + .5, latitude / PI + .5) * vec2(COLUMNS,2048.0);
    vec2 cell = floor(p);
    float radiance = 0.0;
    for (int y=-1;y<=1;y++) for (int x=-1;x<=1;x++) {
        vec2 neighbour = cell + vec2(x,y);
        vec3 key = vec3(mod(neighbour.x,COLUMNS),neighbour.y,47.3);
        vec2 point = neighbour + vec2(spaceBackgroundHash(key),spaceBackgroundHash(key+17.1));
        vec2 distance = p-point;
        distance.x *= cos(latitude);
        float brightness = .25 + .75 * pow(spaceBackgroundHash(key+39.7),3.0);
        brightness *= step(.55,spaceBackgroundHash(key+73.1));
        radiance += brightness * (SIGMA*SIGMA/variance) * exp(-dot(distance,distance)/(2.0*variance));
    }
    return mix(1.0,radiance/MEAN,fade);
}

// Shared foreground extinction for the unresolved galaxy and distant resolved stars.
vec3 spaceBackgroundTransmission(vec3 direction, float detailLevel) {
    vec3 axis = normalize(vec3(.24,.87,.43));
    vec3 centre = normalize(vec3(.87,-.24,0));
    vec3 tangent = normalize(cross(axis,centre));
    float latitude = asin(clamp(dot(direction,axis),-1.0,1.0));
    float inner = pow(max(dot(direction,centre),0.0),5.0);
    float broad = spaceBackgroundNoise(direction*8.0+vec3(17.2,4.1,9.7));
    float middle = spaceBackgroundNoise(direction*31.0+vec3(7.3,19.1,3.5));
    // Extinction has a varying width and regional gaps. A separate offset
    // branch prevents the old continuous, mechanically straight black slit.
    vec3 dustPoint = direction*22.0;
    float dustShape = detailLevel > .5 ? spaceBackgroundDustDetail(dustPoint+vec3(21.7,5.3,9.1))
            : spaceBackgroundNoise(dustPoint+vec3(21.7,5.3,9.1));
    float meander = (broad-.5)*.070 + (middle-.5)*.018;
    float laneWidth = .012 + .040*dustShape + .010*inner;
    float lane = exp(-pow((latitude-meander)/laneWidth,2.0));
    float region = smoothstep(.40,.68,spaceBackgroundNoise(direction*5.7+vec3(4.8,25.7,11.3)));
    float branchCentre = meander + .035 + .045*(middle-.5);
    float branch = exp(-pow((latitude-branchCentre)/(.014+.018*broad),2.0));
    float fray = spaceBackgroundNoise(dustPoint*2.1+vec3(53.1,11.5,7.9));
    float opticalDepth = (lane*(.40+region*3.0) + branch*smoothstep(.38,.65,fray)*1.3)
            * (.25+1.3*dustShape) * (.6+.8*fray);
    return exp(-opticalDepth*vec3(.85,1.10,1.50));
}

vec3 spaceBackgroundRadiance(vec3 direction, vec3 tintColor, float tintAmount,
                             float detailLevel, float angularFootprint) {
    vec3 axis = normalize(vec3(.24,.87,.43));
    vec3 centre = normalize(vec3(.87,-.24,0));
    vec3 tangent = normalize(cross(axis,centre));
    float latitude = asin(clamp(dot(direction,axis),-1.0,1.0));
    vec2 plane = vec2(dot(direction,centre),dot(direction,tangent));
    float longitude = dot(plane,plane) > .00000001 ? atan(plane.y,plane.x) : 0.0;
    float inner = pow(max(dot(direction,centre),0.0),5.0);
    float width = .065 + .055*inner;
    float disk = exp(-latitude*latitude/(width*width));
    float bulge = exp(-longitude*longitude/.50 - latitude*latitude/.025);
    vec3 black = vec3(.00025,.00032,.00042);
    if (max(disk,bulge) < .0005)
        return mix(black,black*(.75+tintColor*.25),clamp(tintAmount,0.0,1.0));
    float broad = spaceBackgroundNoise(direction*8.0+vec3(17.2,4.1,9.7));
    float middle = spaceBackgroundNoise(direction*31.0+vec3(7.3,19.1,3.5));
    float structure = mix(broad,middle,.45);
    if (detailLevel > .5)
        structure = structure*.75 + spaceBackgroundNoise(direction*83.0+vec3(3.7,19.0,4.1))*.25;
    float starCloud = .22 + 1.8*structure*structure;
    vec3 transmission = spaceBackgroundTransmission(direction,detailLevel);
    vec3 population = mix(vec3(.82,.88,1.0),vec3(1.0,.88,.72),inner*.65);
    vec3 stellarLight = population * (disk*.020*starCloud + bulge*.028*(.45+.8*structure));
    if (detailLevel > .5)
        stellarLight *= .75 + .25*spaceBackgroundStellarGrain(longitude,latitude,angularFootprint);
    vec3 color = black + stellarLight*transmission;
    return mix(color,color*(.75+tintColor*.25),clamp(tintAmount,0.0,1.0));
}
