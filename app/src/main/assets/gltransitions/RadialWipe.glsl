#version 300 es
// RadialWipe — from gl-transitions (MIT), author: Xaychru
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float smoothness;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// License: MIT
// Author: Xaychru
// Description: A radial wipe that sweeps around the center like a clock hand
// Tags: wipe, radial, clock
// ported by gre from https://gist.github.com/Xaychru/ce1d48f0ce00bb379750


const float PI = 3.141592653589;

vec4 transition(vec2 p) {
  vec2 rp = p*2.-1.;
  return mix(
    getToColor(p),
    getFromColor(p),
    smoothstep(0., smoothness, atan(rp.y,rp.x) - (progress-.5) * PI * 2.5)
  );
}
void main() { fragColor = transition(vTexCoord); }
