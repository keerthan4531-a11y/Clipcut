#version 300 es
// CrazyParametricFun — from gl-transitions (MIT), author: mandubian
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float a;
uniform float b;
uniform float amplitude;
uniform float smoothness;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: mandubian
// License: MIT
// Description: The outgoing image is pulled along a parametric curve into a psychedelic swirl
// Tags: distort, psychedelic


vec4 transition(vec2 uv) {
  vec2 p = uv.xy / vec2(1.0).xy;
  vec2 dir = p - vec2(.5);
  float dist = length(dir);
  float x = (a - b) * cos(progress) + b * cos(progress * ((a / b) - 1.) );
  float y = (a - b) * sin(progress) - b * sin(progress * ((a / b) - 1.));
  vec2 offset = dir * vec2(sin(progress  * dist * amplitude * x), sin(progress * dist * amplitude * y)) / smoothness;
  return mix(getFromColor(p + offset), getToColor(p), smoothstep(0.2, 1.0, progress));
}
void main() { fragColor = transition(vTexCoord); }
