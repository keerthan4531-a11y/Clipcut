#version 300 es
// PolkaDotsCurtain — from gl-transitions (MIT), author: bobylito
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform float dots;
uniform vec2 center;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: bobylito
// License: MIT
// Description: The incoming image appears through growing polka dots
// Tags: dots, pattern
const float SQRT_2 = 1.414213562373;
// @param Number of dots across the image

vec4 transition(vec2 uv) {
  bool nextImage = distance(fract(uv * dots), vec2(0.5, 0.5)) < ( progress / distance(uv, center));
  return nextImage ? getToColor(uv) : getFromColor(uv);
}
void main() { fragColor = transition(vTexCoord); }
