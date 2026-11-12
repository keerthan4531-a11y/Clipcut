#version 300 es
// HorizontalOpen — from gl-transitions (MIT), author: martiniti
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
// Description: The incoming image opens from a line in the middle toward the top and bottom edges
// Tags: wipe, split

vec4 transition (vec2 uv) {
  
  float regress = 1.0 - progress;

  float s = 2.0 - abs((uv.y - 0.5) / (regress - 1.0)) - 2.0 * regress;
  
  return mix(
    getFromColor(uv),
    getToColor(uv),
    smoothstep(0.0, 0.5, s)
  );
}
void main() { fragColor = transition(vTexCoord); }
