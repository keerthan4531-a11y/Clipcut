#version 300 es
// FilmDissolve — from gl-transitions (MIT), author: gre
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float overlap;
uniform float smoothness;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: gre
// License: MIT
// Description: A cross-dissolve blended in linear light, as light adds up on film: the frame keeps its brightness all the way through and the highlights of each image shine through the other
// Tags: dissolve, fade, blend, light

// @range(0, 1) Extra light where the two images overlap at mid-dissolve, softly rolled off so it never clips
// @range(0, 1) Easing of the dissolve: 0 is linear, 1 eases in and out

// sRGB to linear light (the exact piecewise curve)
vec3 toLinear(vec3 c) {
  vec3 lo = c / 12.92;
  vec3 hi = pow((c + 0.055) / 1.055, vec3(2.4));
  return mix(lo, hi, vec3(greaterThan(c, vec3(0.04045))));
}

// linear light back to sRGB
vec3 toSrgb(vec3 c) {
  vec3 lo = c * 12.92;
  vec3 hi = 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055;
  return mix(lo, hi, vec3(greaterThan(c, vec3(0.0031308))));
}

vec4 transition(vec2 uv) {
  vec4 a = getFromColor(uv);
  vec4 b = getToColor(uv);
  if (progress <= 0.0) return a;
  if (progress >= 1.0) return b;

  float t = mix(progress, smoothstep(0.0, 1.0, progress), smoothness);

  // premultiplied linear light
  vec3 la = toLinear(clamp(a.rgb, 0.0, 1.0)) * a.a;
  vec3 lb = toLinear(clamp(b.rgb, 0.0, 1.0)) * b.a;
  vec3 base = mix(la, lb, t);
  float alpha = mix(a.a, b.a, t);

  // the overlap: each image stays brighter for longer, as in an additive dissolve,
  // rolled off toward full exposure so that bright areas glow instead of clipping
  float extra = overlap * min(t, 1.0 - t);
  vec3 excess = extra * (la + lb);
  vec3 room = max(vec3(alpha) - base, vec3(1e-5));
  vec3 color = base + room * (1.0 - exp(-excess / room));

  if (alpha <= 0.0) return vec4(0.0);
  return vec4(toSrgb(clamp(color / alpha, 0.0, 1.0)), alpha);
}
void main() { fragColor = transition(vTexCoord); }
