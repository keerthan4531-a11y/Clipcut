#version 300 es
// GlitchBands — from gl-transitions (MIT), author: Yoni Maltsman @friendlyspinach
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float ampx;
uniform float ampy;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: Yoni Maltsman @friendlyspinach
// License: MIT
// Description: A crossfade with glitchy horizontal bands and color shifts
// Tags: glitch, blend



// @param Horizontal amplitude of the glitch
// @param Vertical amplitude of the glitch

vec4 transition (vec2 uv) {
  vec4 from = getFromColor(uv);
  vec4 to = getToColor(uv);
  float r = from.r;
  float g = from.g;
  float b = from.b;
  float sphere = r*r + g*g + b*b - 1.0; //3 to 1
  float spiralX = cos(sphere - uv.x/(progress + .01));
  float spiralY = sin(sphere - uv.y/(progress+.01));
  vec2 st = uv;
  st.x = fract(ampx*st.x*spiralX); //1 to 2
  st.y = fract(ampy*st.y*spiralY);
  vec2 diff = uv - st;
  from = getFromColor(uv + progress*diff);
  return mix(from, to, progress);
}
void main() { fragColor = transition(vTexCoord); }
