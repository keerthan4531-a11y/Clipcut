#version 300 es
// Fisheye — from gl-transitions (MIT), author: gre
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float strength;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: gre
// License: MIT
// Description: The outgoing image bulges toward the viewer through a strong fisheye lens, then the incoming one flattens back into place
// Tags: lens, distort, zoom, blend

// @range(0, 1) Strength of the lens at its peak

vec4 transition(vec2 uv) {
  // lens amount: grows, peaks in the middle, then relaxes, eased so that
  // the magnification of the center (1 / (1 - k)) changes at an even pace
  float h = smoothstep(0.0, 1.0, 1.0 - abs(2.0 * progress - 1.0));
  float k = 1.0 - pow(max(1.0 - strength, 0.01), h);
  // round lens: aspect-corrected coordinates, 1.0 at the corners
  vec2 aspect = vec2(ratio, 1.0);
  vec2 c = (uv - 0.5) * aspect;
  float r = length(c) / length(0.5 * aspect);
  // barrel mapping that keeps the corners in place: the center is magnified,
  // the edges squeezed, and every sample stays inside the image
  vec2 q = 0.5 + (uv - 0.5) * (1.0 - k + k * r * r);
  return mix(getFromColor(q), getToColor(q), smoothstep(0.38, 0.62, progress));
}
void main() { fragColor = transition(vTexCoord); }
