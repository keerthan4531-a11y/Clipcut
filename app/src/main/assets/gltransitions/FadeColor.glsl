#version 300 es
// FadeColor — from gl-transitions (MIT), author: gre
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform vec4 color;
uniform float colorPhase;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: gre
// License: MIT
// Description: A fade through a solid color, black by default
// Tags: fade, color
// @color Color to fade through
// @range(0, 1) How long the color shows in the middle
vec4 transition (vec2 uv) {
  return mix(
    mix(color, getFromColor(uv), smoothstep(1.0-colorPhase, 0.0, progress)),
    mix(color, getToColor(uv), smoothstep(    colorPhase, 1.0, progress)),
    progress);
}
void main() { fragColor = transition(vTexCoord); }
