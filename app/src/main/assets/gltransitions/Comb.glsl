#version 300 es
// Comb — from gl-transitions (MIT), author: gre
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform int strips;
uniform bool vertical;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: gre
// License: MIT
// Description: The frame is cut into interleaved strips that slide in opposite directions, each pushing the outgoing image out and the incoming one in
// Tags: slide, stripes, push, horizontal

// @range(2, 40, 1) Number of strips
// @param Cut the frame into columns that slide up and down instead

vec4 transition (vec2 uv) {
  // a: the axis along which the strips slide, b: the axis across them
  float a = vertical ? uv.y : uv.x;
  float b = vertical ? uv.x : uv.y;
  // alternate strips slide in opposite directions
  float dir = mod(floor(b * float(strips)), 2.0) < 0.5 ? 1.0 : -1.0;
  // ease in and out, but keep moving from the first frame
  float t = mix(progress, progress * progress * (3.0 - 2.0 * progress), 0.6);
  float fa = a - dir * t;
  if (fa >= 0.0 && fa <= 1.0) {
    return getFromColor(vertical ? vec2(b, fa) : vec2(fa, b));
  }
  float ta = fa + dir;
  return getToColor(vertical ? vec2(b, ta) : vec2(ta, b));
}
void main() { fragColor = transition(vTexCoord); }
