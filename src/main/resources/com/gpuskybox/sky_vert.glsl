#version 330

// Fullscreen quad in clip space. Each corner is unprojected onto a plane in front of the
// camera; the interpolated point is the view ray for the fragment shader.
layout(location = 0) in vec3 vertf;

uniform mat4 skyProj;

out vec3 fDir;

void main() {
  vec4 p = inverse(skyProj) * vec4(vertf.xy, 1.0, 1.0);
  fDir = p.xyz / p.w;
  gl_Position = vec4(vertf.xy, 0.0, 1.0);
}
