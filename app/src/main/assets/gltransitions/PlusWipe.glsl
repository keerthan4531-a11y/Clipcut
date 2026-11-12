#version 300 es
// PlusWipe — from gl-transitions (MIT), author: gre
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float smoothness;
uniform bool reverse;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: gre
// License: MIT
// Description: A plus-shaped cross grows from the center and widens its arms until the incoming image fills the frame
// Tags: shape, wipe, cross, plus

// @range(0, 0.1) Softness of the cross's edge
// @param Close four corner rectangles in toward the middle instead

float boxDist (vec2 p, vec2 halfSize) {
  vec2 d = abs(p) - halfSize;
  return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}

// 1 inside the cross at time t, 0 outside
float plus (vec2 q, float t, float s) {
  vec2 frame = vec2(0.5 * ratio, 0.5);
  float k = min(frame.x, frame.y);
  // The arms start equally thick, then widen with the frame's proportions,
  // so the four corners left uncovered vanish together
  vec2 halfWidth = k * t + (frame - k) * t * t;
  // The arms reach out at the same speed in every direction
  float halfLength = 3.0 * max(frame.x, frame.y) * t;
  float d = min(boxDist(q, vec2(halfLength, halfWidth.y)), boxDist(q, vec2(halfWidth.x, halfLength)));
  // The soft edge slides from outside to inside the cross, so nothing shows at
  // t = 0 and everything at t = 1, whatever the softness
  return 1.0 - smoothstep(0.0, s, d + s * (1.0 - t));
}

vec4 transition (vec2 uv) {
  // Measured in square pixels, so the cross is not stretched
  vec2 q = (uv - 0.5) * vec2(ratio, 1.0);
  float s = max(smoothness, 0.001);
  float m = reverse ? 1.0 - plus(q, 1.0 - progress, s) : plus(q, progress, s);
  return mix(getFromColor(uv), getToColor(uv), m);
}
void main() { fragColor = transition(vTexCoord); }
