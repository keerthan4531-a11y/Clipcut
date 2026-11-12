#version 300 es
// DirectionalEased — from gl-transitions (MIT), author: Max Plotnikov
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform vec2 direction;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: Max Plotnikov
// License: MIT
// Description: Like Directional, with an eased movement
// Tags: slide, push, directional

// @param Direction of the movement

vec4 transition (vec2 uv) {
  float easing = sqrt((2.0 - progress) * progress);
  vec2 p = uv + easing * sign(direction);
  vec2 f = fract(p);
  return mix(
    getToColor(f),
    getFromColor(f),
    step(0.0, p.y) * step(p.y, 1.0) * step(0.0, p.x) * step(p.x, 1.0)
  );
}
void main() { fragColor = transition(vTexCoord); }
