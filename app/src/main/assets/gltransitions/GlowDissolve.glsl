#version 300 es
// GlowDissolve — from gl-transitions (MIT), author: hjm1fb
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float uLineWidth;
uniform vec3 uSpreadClr;
uniform vec3 uHotClr;
uniform float uPow;
uniform float uIntensity;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: hjm1fb
// License: MIT
// Description: The incoming image spreads over the outgoing one with a hot, glowing edge
// Tags: dissolve, burn, glow

// @color Color of the spreading edge
// @color Color of the hottest part of the edge

vec2 hash(vec2 p)  // replace this by something better
{
  p = vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)));
  return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float noise(in vec2 p) {
  const float K1 = 0.366025404;  // (sqrt(3)-1)/2;
  const float K2 = 0.211324865;  // (3-sqrt(3))/6;

  vec2 i = floor(p + (p.x + p.y) * K1);
  vec2 a = p - i + (i.x + i.y) * K2;
  float m = step(a.y, a.x);
  vec2 o = vec2(m, 1.0 - m);
  vec2 b = a - o + K2;
  vec2 c = a - 1.0 + 2.0 * K2;
  vec3 h = max(0.5 - vec3(dot(a, a), dot(b, b), dot(c, c)), 0.0);
  vec3 n = h * h * h * h * vec3(dot(a, hash(i + 0.0)), dot(b, hash(i + o)), dot(c, hash(i + 1.0)));
  return dot(n, vec3(70.0));
}

vec4 transition(vec2 uv) {
  vec4 from = getFromColor(uv);
  vec4 to = getToColor(uv);
  vec4 outColor;
  float burn;
  burn = 0.5 + 0.5 * (0.299 * from.r + 0.587 * from.g + 0.114 * from.b);

  float show = burn - progress;
  if (show < 0.001) {
    outColor = to;
  } else {
    float factor = 1.0 - smoothstep(0.0, uLineWidth, show);
    vec3 burnColor = mix(uSpreadClr, uHotClr, factor);
    burnColor = pow(burnColor, vec3(uPow)) * uIntensity;
    vec3 finalRGB = mix(from.rgb, burnColor, factor * step(0.0001, progress));
    outColor = vec4(finalRGB, from.a); // straight alpha, like the images
  }
  return outColor;
}
void main() { fragColor = transition(vTexCoord); }
