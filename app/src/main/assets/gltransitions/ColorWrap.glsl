#version 300 es
// ColorWrap — from gl-transitions (MIT), author: Yoni Maltsman @friendlyspinach
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float r_strength;
uniform float g_strength;
uniform float b_strength;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: Yoni Maltsman @friendlyspinach
// License: MIT
// Description: Each color channel of the outgoing image is shifted and wraps around into psychedelic bands, while crossfading to the incoming image
// Tags: color, glitch, psychedelic, fade

// @range(-2, 2) Shift of the red channel over the transition
// @range(-2, 2) Shift of the green channel over the transition
// @range(-2, 2) Shift of the blue channel over the transition

vec4 transition (vec2 uv) {
  vec4 from = getFromColor(uv);
  vec4 to = getToColor(uv);
  vec3 c = from.rgb + vec3(r_strength, g_strength, b_strength)*progress;
  // The wrap flips bright (or, with negative strengths, dark) channels at once: fade it in over the first frames
  from.rgb = mix(from.rgb, mod(c, 1.0), smoothstep(0.0, 0.1, progress));
  return mix(from, to, progress);
}
void main() { fragColor = transition(vTexCoord); }
