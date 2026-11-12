#version 300 es
// DirectionalScaled — from gl-transitions (MIT), author: Thibaut Foussard
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform vec2 direction;
uniform float scale;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: Thibaut Foussard
// based on Directional transition by Gaëtan Renaudeau
// https://gl-transitions.com/editor/Directional
// License: MIT
// Description: Like Directional, with both images scaled down while they slide
// Tags: slide, push, zoom, directional

#define PI acos(-1.0)

// @param Direction of the movement

float parabola(float x) {
  float y = pow(sin(x * PI), 1.);
  return y;
}

vec4 transition (vec2 uv) {
  float easedProgress = pow(sin(progress  * PI / 2.), 3.);
  vec2 p = uv + easedProgress * sign(direction);
  vec2 f = fract(p);
  
  float s = 1. - (1. - (1. / scale)) * parabola(progress);
  f = (f - 0.5) * s  + 0.5;
  
  float mixer = step(0.0, p.y) * step(p.y, 1.0) * step(0.0, p.x) * step(p.x, 1.0);
  vec4 col = mix(getToColor(f), getFromColor(f), mixer);
  
  float border = step(0., f.x) * step(0., (1. - f.x)) * step(0., f.y) * step(0., 1. - f.y);
  col *= border;
  
  return col;
}
void main() { fragColor = transition(vTexCoord); }
