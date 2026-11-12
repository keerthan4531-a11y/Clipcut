#version 300 es
// LeafWipe — from gl-transitions (MIT), author: @Flexi23
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program

in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: @Flexi23
// License: MIT
// Description: A leaf shape grows from the center to reveal the incoming image
// Tags: shape, wipe

// inspired by http://www.wolframalpha.com/input/?i=cannabis+curve

vec4 transition (vec2 uv) {
  if(progress == 0.0){
    return getFromColor(uv);
  }
  vec2 leaf_uv = (uv - vec2(0.5))/10./pow(progress,3.5);
	leaf_uv.y += 0.35;
	float r = 0.18;
	float o = atan(leaf_uv.y, leaf_uv.x);
  return mix(getFromColor(uv), getToColor(uv), 1.-step(1. - length(leaf_uv)+r*(1.+sin(o))*(1.+0.9 * cos(8.*o))*(1.+0.1*cos(24.*o))*(0.9+0.05*cos(200.*o)), 1.));
}
void main() { fragColor = transition(vTexCoord); }
