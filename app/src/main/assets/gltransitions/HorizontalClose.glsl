#version 300 es
// HorizontalClose — from gl-transitions (MIT), author: martiniti
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
// Author: martiniti
// License: MIT
// Description: The incoming image closes in from the top and bottom edges toward the middle
// Tags: wipe, split

vec4 transition (vec2 uv) {

  float s = 2.0 - abs((uv.y - 0.5) / (progress - 1.0)) - 2.0 * progress;
  
  return mix(
    getFromColor(uv),
    getToColor(uv),
    smoothstep(0.5, 0.0, s)
  ); 
}
void main() { fragColor = transition(vTexCoord); }
