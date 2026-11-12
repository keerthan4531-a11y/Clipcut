#version 300 es
// DiamondWipe — from gl-transitions (MIT), author: gre
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform vec2 center;
uniform float smoothness;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: gre
// License: MIT
// Description: A soft-edged diamond grows from the center to reveal the incoming image
// Tags: shape, wipe, iris

// @param Center of the diamond
// @range(0, 0.5) Softness of the diamond's edge

vec4 transition (vec2 uv) {
  // Measured in square pixels, so the diamond is not stretched
  vec2 d = abs(uv - center) * vec2(ratio, 1.0);
  // The diamond covers the frame once it reaches the farthest corner
  vec2 far = max(center, 1.0 - center) * vec2(ratio, 1.0);
  float dist = (d.x + d.y) / (far.x + far.y);
  float s = max(smoothness, 0.001);
  float m = smoothstep(0.0, s, progress * (1.0 + s) - dist);
  return mix(getFromColor(uv), getToColor(uv), m);
}
void main() { fragColor = transition(vTexCoord); }
