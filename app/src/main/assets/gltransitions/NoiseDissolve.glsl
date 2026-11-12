#version 300 es
// NoiseDissolve — from gl-transitions (MIT), author: towrabbit
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
// Author: towrabbit
// License: MIT
// Description: The incoming image appears pixel by pixel in random noise
// Tags: dissolve, noise

float random (vec2 st) {
    return fract(sin(dot(st.xy,vec2(12.9898,78.233)))*43758.5453123);
}
vec4 transition (vec2 uv) {
  vec4 leftSide = getFromColor(uv);
  vec4 rightSide = getToColor(uv);
  float uvz = floor(random(uv)+progress);
  return mix(leftSide,rightSide,uvz);
}
void main() { fragColor = transition(vTexCoord); }
