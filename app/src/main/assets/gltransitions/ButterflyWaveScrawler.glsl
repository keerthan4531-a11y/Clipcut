#version 300 es
// ButterflyWaveScrawler — from gl-transitions (MIT), author: mandubian
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float amplitude;
uniform float waves;
uniform float colorSeparation;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: mandubian
// License: MIT
// Description: Wavy, color-separated ripples scramble the outgoing image into the incoming one
// Tags: wave, distort, chromatic
const float PI = 3.14159265358979323846264;
float compute(vec2 p, float progress, vec2 center) {
vec2 o = p*sin(progress * amplitude)-center;
// horizontal vector
vec2 h = vec2(1., 0.);
// butterfly polar function (don't ask me why this one :))
float theta = acos(dot(o, h)) * waves;
float s = sin((2.*theta - PI) / 24.);
float s2 = s * s;
return (exp(cos(theta)) - 2.*cos(4.*theta) + s2 * s2 * s) / 10.;
}
vec4 transition(vec2 uv) {
  if (progress <= 0.0) return getFromColor(uv);
  if (progress >= 1.0) return getToColor(uv);
  vec2 p = uv;
  float inv = 1. - progress;
  float disp = compute(p, progress, vec2(0.5, 0.5));
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
