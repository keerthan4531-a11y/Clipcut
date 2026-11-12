#version 300 es
// Fold — from gl-transitions (MIT), author: nwoeanhinnogaehr
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
// Description: The incoming image unfolds from the left while the outgoing one folds away to the right
// Tags: fold, slide, horizontal
// Ported from https://gist.github.com/nwoeanhinnogaehr/f6fc39f4cfcbb97f96a6

vec4 transition(vec2 uv) {
  // Only sample the visible side: the other one divides by zero at progress 0 and 1.
  if (progress == 0.0 || uv.x > progress) return getFromColor((uv - vec2(progress, 0.0)) / vec2(1.0 - progress, 1.0));
  return getToColor(uv / vec2(progress, 1.0));
}
void main() { fragColor = transition(vTexCoord); }
