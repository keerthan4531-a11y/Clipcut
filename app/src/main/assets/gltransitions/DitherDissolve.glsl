#version 300 es
// DitherDissolve — from gl-transitions (MIT), author: gre
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float dotSize;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: gre
// License: MIT
// Description: The incoming image appears through a regular 8x8 ordered dither pattern, like a retro computer screen
// Tags: dissolve, pattern, retro, pixelate

// @range(0.002, 0.05) Size of a dither dot, relative to the height

// Threshold of the 8x8 Bayer matrix at integer cell c, in (0, 1)
float bayer(vec2 c) {
  float v = 0.0;
  float scale = 16.0;
  for (int i = 0; i < 3; i++) {
    vec2 bit = mod(floor(c), 2.0);
    // a 2x2 step: 0 2 / 3 1, i.e. 2 * (x xor y) + y
    v += scale * (2.0 * abs(bit.x - bit.y) + bit.y);
    c *= 0.5;
    scale *= 0.25;
  }
  return (v + 0.5) / 64.0;
}

vec4 transition(vec2 uv) {
  vec2 cell = floor(uv * vec2(ratio, 1.0) / dotSize);
  return progress < bayer(cell) ? getFromColor(uv) : getToColor(uv);
}
void main() { fragColor = transition(vTexCoord); }
