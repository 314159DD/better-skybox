#version 330

uniform samplerCube cubemap;
uniform samplerCube prevCubemap; // cubemap shown before the last area change
uniform float blend;            // 0 = prevCubemap, 1 = cubemap
uniform vec4 fogColor;
uniform float horizonBlend;  // width of the fog -> sky fade above the horizon, in units of the up component
uniform float horizonOffset; // sine of the angle the sky horizon is pushed below the geometric one, so it starts behind the terrain edge
uniform float fogTint;       // 0..1, share of fog colour mixed into the whole sky
uniform float brightness;    // multiplier
uniform float flash;         // lightning, 0..1: lifts the whole sky towards white
uniform float rotation;      // radians around the vertical axis
uniform samplerCube starMap; // NASA Deep Star Map, black where there is no star
uniform mat3 starRot;        // world view dir (y down) -> star map lookup, as in proc_sky_frag.glsl
uniform float starAmount;    // 0..1, how much of the sky on screen is a night one; crossfades with blend
uniform float starBrightness; // multiplier, the Star brightness setting
uniform float starDim;       // 0..1, how far the cubemap is darkened where the stars are drawn

in vec3 fDir;

out vec4 FragColor;

void main() {
  vec3 d = normalize(fDir);
  // world space has +y pointing down; cubemaps expect +y up
  float up = -d.y + horizonOffset;

  float c = cos(rotation);
  float s = sin(rotation);
  vec3 sampleDir = vec3(c * d.x - s * d.z, up, s * d.x + c * d.z);

  vec3 sky = mix(texture(prevCubemap, sampleDir).rgb, texture(cubemap, sampleDir).rgb, blend) * brightness;

  // the photo's own stars are blurry blobs, so sink them and put the real sky on top, fading out into the fog
  float t = smoothstep(0.0, max(horizonBlend, 0.001), up);
  float stars = starAmount * t;
  if (stars > 0.001) {
    sky *= mix(1.0, 1.0 - starDim, stars);
    sky += texture(starMap, starRot * d).rgb * starBrightness * 1.6 * stars;
  }

  sky = mix(sky, fogColor.rgb, fogTint);
  vec3 outSky = mix(fogColor.rgb, sky, t);
  FragColor = vec4(mix(outSky, vec3(1.0), flash * 0.7), 1.0);
}
