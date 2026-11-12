#version 300 es
// DreamyZoom — from gl-transitions (MIT), author: Zeh Fernando
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float rotation;
uniform float scale;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: Zeh Fernando
// License: MIT
// Description: Zooms and rotates into a bright flash, then out to the incoming image
// Tags: zoom, rotate, flash

// Definitions --------
#define DEG2RAD 0.03926990816987241548078304229099 // 1/180*PI


// Transition parameters --------

// In degrees

// Multiplier


// The code proper --------

vec4 transition(vec2 uv) {
  // Massage parameters
  float phase = progress < 0.5 ? progress * 2.0 : (progress - 0.5) * 2.0;
  float angleOffset = progress < 0.5 ? mix(0.0, rotation * DEG2RAD, phase) : mix(-rotation * DEG2RAD, 0.0, phase);
  float newScale = progress < 0.5 ? mix(1.0, scale, phase) : mix(scale, 1.0, phase);
  
  vec2 center = vec2(0, 0);

  // Calculate the source point
  vec2 assumedCenter = vec2(0.5, 0.5);
  vec2 p = (uv.xy - vec2(0.5, 0.5)) / newScale * vec2(ratio, 1.0);

  // This can probably be optimized (with distance())
  float angle = atan(p.y, p.x) + angleOffset;
  float dist = distance(center, p);
  p.x = cos(angle) * dist / ratio + 0.5;
  p.y = sin(angle) * dist + 0.5;
  vec4 c = progress < 0.5 ? getFromColor(p) : getToColor(p);

  // Finally, apply the color
  // The flash lights up the image, not the transparent areas around it.
  return vec4(c.rgb + (progress < 0.5 ? mix(0.0, 1.0, phase) : mix(1.0, 0.0, phase)), c.a);
}
void main() { fragColor = transition(vTexCoord); }
