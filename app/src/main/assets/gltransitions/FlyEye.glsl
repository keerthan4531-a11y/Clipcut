#version 300 es
// FlyEye — from gl-transitions (MIT), author: gre
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float size;
uniform float zoom;
uniform float colorSeparation;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: gre
// License: MIT
// Description: The image breaks into a fly's-eye grid of lenses with color separation
// Tags: lens, distort, chromatic

vec4 transition(vec2 p) {
  float inv = 1. - progress;
  vec2 disp = size*vec2(cos(zoom*p.x), sin(zoom*p.y));
  vec4 texTo = getToColor(p + inv*disp);
  vec4 fromCenter = getFromColor(p + progress*disp);
  vec4 texFrom = vec4(
    getFromColor(p + progress*disp*(1.0 - colorSeparation)).r,
    fromCenter.g,
    getFromColor(p + progress*disp*(1.0 + colorSeparation)).b,
    fromCenter.a);
  return texTo*progress + texFrom*inv;
}
void main() { fragColor = transition(vTexCoord); }
