#version 300 es
// ColorDistance — from gl-transitions (MIT), author: P-Seebauer
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float power;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// License: MIT
// Author: P-Seebauer
// Description: Pixels switch to the incoming image in order of how close their colors are
// Tags: blend, color
// ported by gre from https://gist.github.com/P-Seebauer/2a5fa2f77c883dd661f9


vec4 transition(vec2 p) {
  vec4 fTex = getFromColor(p);
  vec4 tTex = getToColor(p);
  float m = step(distance(fTex, tTex), progress);
  return mix(
    mix(fTex, tTex, m),
    tTex,
    pow(progress, power)
  );
}
void main() { fragColor = transition(vTexCoord); }
