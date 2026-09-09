#version 330

uniform samplerCube cubemap;
uniform samplerCube prevCubemap; // cubemap shown before the last area change
uniform float blend;            // 0 = prevCubemap, 1 = cubemap
uniform vec4 fogColor;
uniform float horizonBlend;  // width of the fog -> sky fade above the horizon, in units of the up component
uniform float horizonOffset; // sine of the angle the sky horizon is pushed below the geometric one, so it starts behind the terrain edge
uniform float fogTint;       // 0..1, share of fog colour mixed into the whole sky
uniform float brightness;    // multiplier
uniform float rotation;      // radians around the vertical axis

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
  sky = mix(sky, fogColor.rgb, fogTint);

  float t = smoothstep(0.0, max(horizonBlend, 0.001), up);
  FragColor = vec4(mix(fogColor.rgb, sky, t), 1.0);
}
