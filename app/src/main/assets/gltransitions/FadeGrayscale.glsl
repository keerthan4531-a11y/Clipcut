#version 300 es
// FadeGrayscale — from gl-transitions (MIT), author: gre
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float intensity;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: gre
// License: MIT
// Description: A crossfade through grayscale
// Tags: fade, grayscale, color

// @range(0, 1) How long the grayscale phase lasts
 
vec3 grayscale (vec3 color) {
  return vec3(0.2126*color.r + 0.7152*color.g + 0.0722*color.b);
}
 
vec4 transition (vec2 uv) {
  vec4 fc = getFromColor(uv);
  vec4 tc = getToColor(uv);
  return mix(
    mix(vec4(grayscale(fc.rgb), fc.a), fc, smoothstep(1.0-intensity, 0.0, progress)),
    mix(vec4(grayscale(tc.rgb), tc.a), tc, smoothstep(    intensity, 1.0, progress)),
    progress);
}
void main() { fragColor = transition(vTexCoord); }
