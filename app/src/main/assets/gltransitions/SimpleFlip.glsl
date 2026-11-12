#version 300 es
// SimpleFlip — from gl-transitions (MIT), author: nwoeanhinnogaehr
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program

in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: nwoeanhinnogaehr
// License: MIT
// Description: The outgoing image flips around the vertical axis into the incoming one
// Tags: flip, horizontal
// Ported from https://gist.github.com/nwoeanhinnogaehr/408045772d255df97520

vec4 transition(vec2 uv) {
  vec2 q = uv;
  uv.x = (uv.x - 0.5) / abs(progress - 0.5) * 0.5 + 0.5;
  vec4 a = getFromColor(uv);
  vec4 b = getToColor(uv);
  // Transparent beside the flipping image
  return mix(a, b, step(0.5, progress)) * step(abs(q.x - 0.5), abs(progress - 0.5));
}
void main() { fragColor = transition(vTexCoord); }
