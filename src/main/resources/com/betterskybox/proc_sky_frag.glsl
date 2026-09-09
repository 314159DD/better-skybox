#version 330

// Procedural sky: gradient by sun altitude, sun glow and disk, clouds, stars, nebula, shooting stars,
// moon with phase, aurora. Gradient, haze, starfield, nebula and aurora math adapted from 117 HD's
// day/night cycle work (PR #655, 3-X); clouds and sun disk are our own.

uniform vec3 zenithColor;
uniform vec3 horizonColor;
uniform vec3 sunColor;
uniform vec3 sunDir;         // y up
uniform vec3 moonDir;        // y up
uniform vec3 moonColor;
uniform float moonVisibility;
uniform float moonSize;      // 1 = default disk
uniform float moonPhase;     // 0 new .. 1 full
uniform float starVisibility;
uniform float starBrightness;
uniform float shootingStars; // 0/1
uniform float nebulaVisibility;
uniform float auroraVisibility;
uniform float sunDisk;       // 0/1
uniform float cloudCover;    // 0..1
uniform float cloudTime;     // seconds * speed
uniform float time;
uniform vec4 fogColor;
uniform float horizonBlend;
uniform float horizonOffset; // sine of the angle the sky horizon, sun and moon are pushed below the geometric one
uniform float fogTint;
uniform float brightness;
uniform float flash;         // lightning, 0..1: lifts the whole sky towards white
uniform samplerCube starMap;   // NASA Deep Star Map, black where there is no star
uniform float starMapEnabled;  // 0/1
uniform mat3 starRot;          // world view dir (y down) -> star map lookup

in vec3 fDir;

out vec4 FragColor;

#define TAU 6.28318530718

// ---------------- hashing / noise ----------------
uvec3 pcg3d(uvec3 v) {
  v = v * 1664525u + 1013904223u;
  v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
  v ^= v >> 16u;
  v.x += v.y * v.z; v.y += v.z * v.x; v.z += v.x * v.y;
  return v;
}

float hash1(vec3 p) {
  uvec3 q = floatBitsToUint(p);
  uint h = q.x * 1664525u + q.y * 1013904223u + q.z;
  h ^= h >> 16u; h *= 0x7feb352du; h ^= h >> 15u; h *= 0x846ca68bu; h ^= h >> 16u;
  return float(h) * (1.0 / 4294967296.0);
}

float hash2(vec2 st) {
  return fract(sin(dot(st, vec2(12.9898, 78.233))) * 43758.5453123);
}

float noise2(vec2 st) {
  vec2 i = floor(st);
  vec2 f = fract(st);
  float a = hash2(i), b = hash2(i + vec2(1, 0)), c = hash2(i + vec2(0, 1)), d = hash2(i + vec2(1, 1));
  vec2 u = f * f * (3.0 - 2.0 * f);
  return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm2(vec2 st) {
  float v = 0.0, amp = 0.5;
  for (int i = 0; i < 5; i++) { v += amp * noise2(st); st *= 2.0; amp *= 0.5; }
  return v;
}

float noise3(vec3 p) {
  vec3 i = floor(p);
  vec3 f = fract(p);
  f = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
  return mix(
    mix(mix(hash1(i + vec3(0,0,0)), hash1(i + vec3(1,0,0)), f.x),
        mix(hash1(i + vec3(0,1,0)), hash1(i + vec3(1,1,0)), f.x), f.y),
    mix(mix(hash1(i + vec3(0,0,1)), hash1(i + vec3(1,0,1)), f.x),
        mix(hash1(i + vec3(0,1,1)), hash1(i + vec3(1,1,1)), f.x), f.y),
    f.z);
}

float fbm3(vec3 p, int octaves) {
  float sum = 0.0, amp = 0.5, norm = 0.0;
  for (int o = 0; o < octaves; o++) {
    sum += amp * noise3(p);
    norm += amp;
    p *= 2.02;
    amp *= 0.5;
  }
  return sum / norm;
}

// ---------------- stars ----------------
vec3 starfield(vec3 dir) {
  vec3 starColor = vec3(0.0);
  for (int layer = 0; layer < 2; layer++) {
    float gridScale = layer == 0 ? 80.0 : 200.0;
    float sparsity = layer == 0 ? 0.82 : 0.76;
    float maxBrightness = layer == 0 ? 1.2 : 0.4;
    float starRadius = layer == 0 ? 0.45 : 0.30;

    vec3 scaledDir = dir * gridScale;
    vec3 cell = floor(scaledDir);
    vec3 frac = scaledDir - cell;
    vec3 nearStep = step(0.5, frac) * 2.0 - 1.0;
    for (int ix = 0; ix <= 1; ix++)
    for (int iy = 0; iy <= 1; iy++)
    for (int iz = 0; iz <= 1; iz++) {
      vec3 nc = cell + vec3(ix, iy, iz) * nearStep;
      uvec3 h = pcg3d(floatBitsToUint(nc));
      const float inv16 = 1.0 / 65536.0;
      vec3 lo = vec3(h & 0xFFFFu) * inv16;
      vec3 hi = vec3(h >> 16u) * inv16;
      float cellRand = lo.x;
      if (cellRand < sparsity) continue;
      vec3 starPos = nc + vec3(hi.x, lo.y, hi.y);
      float thisRadius = starRadius * (0.5 + lo.z * 0.5);
      float dist = length(starPos - scaledDir);
      if (dist > thisRadius) continue;
      float seed = (cellRand - sparsity) / (1.0 - sparsity);
      float b = seed * seed * sqrt(seed) * maxBrightness;
      float falloff = 1.0 - smoothstep(0.0, thisRadius, dist);
      falloff *= falloff; falloff *= falloff; falloff *= falloff;
      b *= falloff;
      float cs = hi.z;
      vec3 tint = vec3(1.0, 0.7, 0.45);
      tint = mix(tint, vec3(1.0, 0.9, 0.65), step(0.06, cs));
      tint = mix(tint, vec3(1.0, 0.95, 0.85), step(0.18, cs));
      tint = mix(tint, vec3(1.0), step(0.30, cs));
      tint = mix(tint, vec3(0.85, 0.92, 1.0), step(0.70, cs));
      tint = mix(tint, vec3(0.7, 0.8, 1.0), step(0.85, cs));
      starColor += tint * b;
    }
  }
  return starColor;
}

// ---------------- nebula ----------------
float nebulaClusterInfluence(vec3 dir) {
  vec4 clusters[3] = vec4[3](
    vec4(normalize(vec3(0.3, 0.8, 0.5)), 0.5),
    vec4(normalize(vec3(-0.6, 0.6, -0.4)), 0.4),
    vec4(normalize(vec3(0.7, 0.5, -0.5)), 0.35));
  float influence = 0.0;
  for (int i = 0; i < 3; i++) {
    float sigma = clusters[i].w * 6.0;
    float angleSq = max(0.0, (1.0 - dot(dir, clusters[i].xyz)) * 2.0);
    influence = max(influence, exp(-angleSq / (2.0 * sigma * sigma)));
  }
  return influence;
}

vec3 nebula(vec3 dir) {
  vec3 warp = vec3(fbm3(dir * 2.0 + vec3(11.3), 1), fbm3(dir * 2.0 + vec3(47.1), 1), fbm3(dir * 2.0 + vec3(83.7), 1));
  vec3 wdir = dir + (warp - 0.5) * 0.9;
  float clusterBias = nebulaClusterInfluence(dir);
  float region = fbm3(wdir * 2.5 + vec3(50.0), 3);
  region = smoothstep(0.45, 0.78, (region + clusterBias) * 0.45);
  if (region <= 0.0) return vec3(0.0);
  float wisps = smoothstep(0.35, 0.75, fbm3(wdir * 9.0 + vec3(100.0), 3));
  float detail = fbm3(wdir * 28.0 + vec3(200.0), 2);
  float intensity = region * (0.55 + 0.45 * wisps) * (0.6 + 0.4 * detail) * 1.9;
  intensity *= 1.0 + clusterBias * 1.2;
  vec3 teal = vec3(0.008, 0.025, 0.035);
  vec3 purple = vec3(0.02, 0.01, 0.035);
  float variation = fbm3(wdir * 4.0 + vec3(77.0), 2);
  // scaled up: we output sRGB directly, 117 HD works in linear light
  return mix(teal, purple, variation * 0.5) * intensity * 7.0;
}

// ---------------- shooting stars ----------------
vec3 shootingStarColor(vec3 viewDir, float t0) {
  vec3 color = vec3(0.0);
  const float SLOT = 15.0;
  for (int channel = 0; channel < 3; channel++) {
    float t = t0 - float(channel) * 5.0;
    float slot = floor(t / SLOT);
    float phase = fract(t / SLOT);
    vec3 seed = vec3(slot, float(channel) * 137.0 + 42.0, 7.0);
    if (hash1(seed) > 0.12) continue;
    float theta = hash1(seed + vec3(1.0, 0.0, 0.0)) * TAU;
    float cosElev = 1.0 - hash1(seed + vec3(2.0, 0.0, 0.0)) * 0.65;
    float sinElev = sqrt(1.0 - cosElev * cosElev);
    vec3 startPos = normalize(vec3(sinElev * cos(theta), -cosElev, sinElev * sin(theta)));
    float tTheta = hash1(seed + vec3(3.0, 0.0, 0.0)) * TAU;
    float tPhi = 0.3 + hash1(seed + vec3(4.0, 0.0, 0.0)) * 0.5;
    vec3 travelDir = normalize(vec3(sin(tPhi) * cos(tTheta), cos(tPhi), sin(tPhi) * sin(tTheta)));
    float speed = 0.08 + hash1(seed + vec3(5.0, 0.0, 0.0)) * 0.06;
    float lifetime = 0.8 + hash1(seed + vec3(6.0, 0.0, 0.0)) * 0.7;
    float maxBright = 0.6 + hash1(seed + vec3(7.0, 0.0, 0.0)) * 0.6;
    float age = phase * SLOT - 0.1 * SLOT;
    if (age < 0.0 || age > lifetime) continue;
    float alpha = smoothstep(0.0, 0.15, age) * smoothstep(0.0, 0.3, lifetime - age);
    float headDist = age * speed;
    float trailLen = speed * 0.7 * alpha;
    vec3 headPos = normalize(startPos + travelDir * headDist);
    vec3 tailPos = normalize(startPos + travelDir * max(0.0, headDist - trailLen));
    vec3 seg = headPos - tailPos;
    float segLen = length(seg);
    if (segLen < 0.0001) continue;
    vec3 segN = seg / segLen;
    float tProj = clamp(dot(viewDir - tailPos, segN), 0.0, segLen);
    vec3 closest = tailPos + segN * tProj;
    float angDist = acos(clamp(dot(viewDir, normalize(closest)), 0.0, 1.0));
    float streak = smoothstep(0.0015, 0.0015 * 0.15, angDist);
    if (streak < 0.001) continue;
    float headGrad = tProj / segLen;
    float b = (smoothstep(0.7, 1.0, headGrad) * 2.0 + headGrad * 0.6) * streak * alpha * maxBright;
    color += vec3(1.0, 0.95, 0.8) * b;
  }
  return color;
}

// ---------------- aurora ----------------
float au_hash21(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123); }

float au_noise(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  f = f * f * (3.0 - 2.0 * f);
  return mix(mix(au_hash21(i), au_hash21(i + vec2(1.0, 0.0)), f.x),
             mix(au_hash21(i + vec2(0.0, 1.0)), au_hash21(i + vec2(1.0, 1.0)), f.x), f.y);
}

float auroraCurtain(float x, float t) {
  float wave = sin(x * 1.8 - t * 0.4) * 0.5;
  wave += sin(x * 3.1 + t * 0.25) * 0.25;
  wave += sin(x * 7.0 - t * 0.6) * 0.08;
  wave += au_noise(vec2(x * 0.8 + t * 0.05, t * 0.03)) * 0.4;
  return wave;
}

vec3 auroraLayer(vec3 rd, float planeY, float t, float layerSeed) {
  if (rd.y >= 0.0) return vec3(0.0);
  float dist = planeY / rd.y;
  if (dist < 0.0) return vec3(0.0);
  vec3 hit = rd * dist;
  float curtainX = hit.x * 0.0005 + layerSeed * 3.7;
  float curtainZ = hit.z * 0.0005;
  float wave = auroraCurtain(curtainX, t + layerSeed * 5.0);
  float d = abs(curtainZ - wave);
  float curtainMask = exp(-d * d * 55.0);
  float rays = mix(0.55, 1.0, smoothstep(0.2, 0.8, au_noise(vec2(curtainX * 12.0 + layerSeed * 10.0, t * 0.1))));
  float bright = 0.4 + au_noise(vec2(curtainX * 2.0 - t * 0.02, layerSeed)) * 0.6;
  float distFade = exp(-dist * dist * 0.0000001);
  float intensity = curtainMask * rays * bright * distFade;
  float hueNoise = au_noise(vec2(curtainX * 1.3 + layerSeed * 4.0, t * 0.04 + layerSeed));
  vec3 green = mix(vec3(0.0, 1.5, 0.9), vec3(0.35, 1.7, 0.25), smoothstep(0.25, 0.75, hueNoise));
  float heightBlend = (planeY - 600.0) / 600.0;
  vec3 col = mix(green, vec3(0.9, 0.2, 1.2), smoothstep(0.0, 1.0, heightBlend));
  return col * intensity;
}

vec3 aurora(vec3 viewDir, float t) {
  float up = -viewDir.y;
  if (up < 0.01) return vec3(0.0);
  float northFacing = viewDir.z;
  if (northFacing < -0.2) return vec3(0.0);
  float northBlend = smoothstep(-0.2, 0.3, northFacing);
  float horizonFade = smoothstep(0.01, 0.06, up);
  vec3 a = auroraLayer(viewDir, -600.0, t, 1.0) * 0.75
         + auroraLayer(viewDir, -800.0, t, 2.0) * 1.0
         + auroraLayer(viewDir, -1050.0, t, 3.0) * 0.5;
  return a * northBlend * horizonFade * 0.26;
}

void main() {
  vec3 viewDir = normalize(fDir);       // world space, y down
  float up = -viewDir.y + horizonOffset;

  vec3 sun = normalize(vec3(sunDir.x, -sunDir.y + horizonOffset, sunDir.z));

  // ---- gradient ----
  vec2 viewHoriz = vec2(viewDir.x, viewDir.z);
  float viewHorizLen = length(viewHoriz);
  vec3 viewHorizontal = viewHorizLen > 1e-4 ? vec3(viewHoriz.x, 0.0, viewHoriz.y) / viewHorizLen : vec3(0.0);
  vec3 sunHorizontal = normalize(vec3(sun.x, 0.0, sun.z) + vec3(1e-5, 0.0, 0.0));
  float sunFacing = dot(viewHorizontal, sunHorizontal) * smoothstep(0.0, 0.35, viewHorizLen);
  float sunSideBlend = smoothstep(0.0, 1.0, (sunFacing + 1.0) * 0.5);
  float zenithBlend = smoothstep(-0.1, 0.7, up);

  float sunAltitude = clamp(sunDir.y, 0.0, 1.0);
  float daytimeFactor = smoothstep(0.0, 0.64, sunAltitude);
  float dimFadeout = smoothstep(0.0, 0.34, sunAltitude);
  float darkSideDim = mix(0.7, 1.0, dimFadeout);
  vec3 darkSideColor = mix(zenithColor * darkSideDim, horizonColor, daytimeFactor);
  float nightFade = smoothstep(-0.26, 0.0, sunDir.y);

  vec3 hColor = mix(darkSideColor, horizonColor, sunSideBlend);
  hColor = mix(zenithColor, hColor, nightFade);
  vec3 sky = mix(hColor, zenithColor, zenithBlend);

  float sunDot = dot(viewDir, sun);
  if (sunDot > 0.0) {
    float s2 = sunDot * sunDot, s4 = s2 * s2, s8 = s4 * s4, s16 = s8 * s8, s32 = s16 * s16;
    float s128 = s32 * s32; s128 = s128 * s128;
    sky += sunColor * (s128 * 0.4 + s32 * 0.25 + s8 * 0.15 + s2 * sunDot * sqrt(sunDot) * 0.08);
  }
  vec3 skyPreStars = sky;

  // ---- night sky: stars, nebula ----
  float nightFactor = pow(1.0 - nightFade, mix(0.4, 0.9, sunSideBlend * (1.0 - zenithBlend)));
  float horizonStarFade = smoothstep(-0.1, 0.07, up);
  float starBlend = nightFactor * starVisibility * horizonStarFade;
  if (starBlend > 0.001) {
    float a = time * 0.002;
    vec3 sd = vec3(cos(a) * viewDir.x - sin(a) * viewDir.z, viewDir.y, sin(a) * viewDir.x + cos(a) * viewDir.z);
    vec3 night = starMapEnabled > 0.5
      ? texture(starMap, starRot * viewDir).rgb * starBrightness * 1.6
      : starfield(sd) * starBrightness;
    if (nebulaVisibility > 0.001)
      night += nebula(sd) * nebulaVisibility;
    sky += night * starBlend;
  }

  // ---- moon ----
  if (moonVisibility > 0.001) {
    vec3 moon = normalize(vec3(moonDir.x, -moonDir.y + horizonOffset, moonDir.z));
    float moonDot = dot(viewDir, moon);
    float luma = dot(skyPreStars, vec3(0.299, 0.587, 0.114));
    float dayVis = 1.0 / (1.0 + luma * 12.0);
    dayVis *= smoothstep(0.9, 0.7, dot(moon, sun));
    if (moonDot > 0.0 && dayVis > 0.001) {
      float radius = cos(acos(0.99945) * moonSize);
      float edge = fwidth(moonDot) * 1.5;
      float disk = smoothstep(radius - edge, radius, moonDot);
      if (disk > 0.0) {
        float angDist = acos(clamp(moonDot, 0.0, 1.0));
        float moonRadius = acos(radius);
        vec3 moonUp = abs(moon.y) < 0.999 ? vec3(0.0, 1.0, 0.0) : vec3(0.0, 0.0, 1.0);
        vec3 moonRight = normalize(cross(moonUp, moon));
        moonUp = normalize(cross(moon, moonRight));
        vec3 toView = normalize(viewDir - moon * moonDot);
        vec2 local = vec2(dot(toView, moonRight), dot(toView, moonUp)) * angDist / moonRadius;
        float localZ = sqrt(max(0.0, 1.0 - dot(local, local)));
        vec3 n = vec3(local, localZ);

        // phase: light comes from the sun's side of the moon, illumination from the slider
        vec2 moonToSun = vec2(dot(sun, moonRight), dot(sun, moonUp));
        float l = length(moonToSun);
        moonToSun = l > 1e-4 ? moonToSun / l : vec2(1.0, 0.0);
        float phaseCos = 2.0 * moonPhase - 1.0;
        float phaseSin = sqrt(max(0.0, 1.0 - phaseCos * phaseCos));
        vec3 lightDir = vec3(moonToSun * phaseSin, phaseCos);
        float lambert = dot(n, lightDir);
        float lit = smoothstep(-0.05, 0.2, lambert) * (0.35 + 0.65 * clamp(lambert, 0.0, 1.0));

        vec2 uv = local * 4.0 + vec2(50.0);
        float surface = fbm2(uv * 0.4) * 0.4 + fbm2(uv * 1.5) * 0.4 + fbm2(uv * 5.0) * 0.2;
        surface = mix(0.6, 1.0, surface);
        float seas = smoothstep(0.50, 0.40, fbm2(uv * 0.8 + vec2(30.0, 70.0)));
        surface *= mix(1.0, 0.88, seas);

        vec3 darkSide = skyPreStars * 0.9;
        vec3 moonCol = mix(darkSide, moonColor * surface, lit);
        sky = mix(sky, moonCol, disk * moonVisibility * dayVis * horizonStarFade);
      }
      float glow = pow(moonDot, 256.0 / max(moonSize, 0.001)) * 0.05 * moonPhase * dayVis * moonVisibility * horizonStarFade;
      sky += moonColor * glow;
    }
  }

  // ---- sun disk ----
  if (sunDisk > 0.5 && sunDot > 0.0) {
    float disk = smoothstep(cos(0.032), cos(0.024), sunDot);
    float aboveHorizon = smoothstep(-0.03, 0.03, sunDir.y);
    sky = mix(sky, sunColor * 1.6, disk * aboveHorizon);
  }

  // ---- shooting stars, aurora ----
  if (shootingStars > 0.5 && starBlend > 0.001 && up > 0.05)
    sky += shootingStarColor(viewDir, time) * starBlend;
  if (auroraVisibility > 0.001 && nightFactor > 0.001)
    sky += aurora(viewDir, time) * nightFactor * auroraVisibility;

  // ---- clouds ----
  if (cloudCover > 0.001 && up > 0.0) {
    vec2 p = viewDir.xz / (up + 0.15) * 1.6 + cloudTime * vec2(0.03, 0.012);
    float n = fbm2(p * 1.4);
    float threshold = mix(0.78, 0.30, cloudCover);
    float c = smoothstep(threshold, threshold + 0.18, n);
    float thick = smoothstep(threshold, threshold + 0.45, n);
    float cloudHorizonFade = smoothstep(0.02, 0.18, up);
    float dayLight = clamp(sunDir.y * 3.0 + 0.3, 0.08, 1.0);
    vec3 cloudCol = mix(horizonColor, vec3(1.0), 0.65) * dayLight;
    float lowSun = 1.0 - clamp(sunDir.y * 2.5, 0.0, 1.0);
    cloudCol = mix(cloudCol, sunColor * dayLight, 0.4 * sunSideBlend * lowSun);
    cloudCol *= mix(1.0, 0.72, thick);
    sky = mix(sky, cloudCol, c * cloudHorizonFade * 0.95);
  }

  // ---- haze ----
  float horizonHaze = 1.0 - abs(up);
  horizonHaze = horizonHaze * horizonHaze * sqrt(horizonHaze) * 0.15;
  vec3 hazeColor = mix(horizonColor * 0.8, horizonColor * 1.3, sunSideBlend);
  sky = mix(sky, hazeColor, horizonHaze);
  float scatter = sunSideBlend * (1.0 - zenithBlend) * 0.2;
  sky = mix(sky, sunColor * 0.5 + horizonColor * 0.5, scatter);

  sky *= brightness;
  sky = mix(sky, fogColor.rgb, fogTint);
  float t = smoothstep(0.0, max(horizonBlend, 0.001), up);
  sky = mix(fogColor.rgb, sky, t);

  sky = mix(sky, vec3(1.0), flash * 0.7);
  sky += (hash2(gl_FragCoord.xy) - 0.5) / 255.0;
  FragColor = vec4(clamp(sky, 0.0, 1.0), 1.0);
}
