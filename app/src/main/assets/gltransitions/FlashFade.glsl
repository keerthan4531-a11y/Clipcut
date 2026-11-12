#version 300 es
// FlashFade — from gl-transitions (MIT), author: gre
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
// Author: gre
// License: MIT
// Description: A crossfade that brightens toward a color in the middle
// Tags: fade, color, flash
// @color Color the images brighten toward
uniform vec3 color /* = vec3(0.9, 0.4, 0.2) */;
vec4 transition (vec2 uv) {
  return mix(
    getFromColor(uv) + vec4(progress*color, 0.0),
    getToColor(uv) + vec4((1.0-progress)*color, 0.0),
    progress
  );
}
void main() { fragColor = transition(vTexCoord); }
