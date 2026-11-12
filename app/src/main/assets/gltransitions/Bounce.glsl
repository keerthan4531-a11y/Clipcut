#version 300 es
// Bounce — from gl-transitions (MIT), author: Adrian Purser
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform vec4 shadow_colour;
uniform float shadow_height;
uniform float bounces;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: Adrian Purser
// License: MIT
// Description: The outgoing image drops out of the frame with a few bounces, revealing the incoming one
// Tags: bounce, slide, vertical, shadow

// @color Color of the shadow cast on the incoming image
// @range(0.01, 0.5) Height of the shadow, relative to the image height
// @range(1, 10, 1) Number of bounces before settling

const float PI = 3.14159265358;

vec4 transition (vec2 uv) {
  float time = progress;
  float stime = sin(time * PI / 2.);
  float phase = time * PI * bounces;
  float y = (abs(cos(phase))) * (1.0 - stime);
  float d = uv.y - y;
  vec4 to = getToColor(uv);
  return mix(
    mix(
      to,
      vec4(shadow_colour.rgb, 1.0), // shadow_colour.a is in the amount: the shadow darkens, it does not make the image transparent
      step(d, shadow_height) * (1. - mix(
        ((d / shadow_height) * shadow_colour.a) + (1.0 - shadow_colour.a),
        1.0,
        smoothstep(0.95, 1., progress) // fade-out the shadow at the end
      )) * to.a // cast on the incoming image, not on its transparent areas
    ),
    getFromColor(vec2(uv.x, uv.y + (1.0 - y))),
    step(d, 0.0)
  );
}
void main() { fragColor = transition(vTexCoord); }
