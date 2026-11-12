#version 300 es
// StaticWipe — from gl-transitions (MIT), author: Ben Lucas
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform bool u_transitionUpToDown;
uniform float u_max_static_span;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: Ben Lucas
// License: MIT
// Description: A wipe from top to bottom with a band of TV static at its edge
// Tags: wipe, static, noise, vertical

#define PI 3.14159265359

float rnd (vec2 st) {
    return fract(sin(dot(st.xy,
                         vec2(10,70)))*
        12345.5453123);
}

// @param Wipe from top to bottom; false wipes from bottom to top
// @param Maximum height of the static band

vec4 transition (vec2 uv) {
  

  float span = u_max_static_span*pow(sin(PI*progress),0.5);
  
  float transitionEdge = u_transitionUpToDown ? 1.0-uv.y : uv.y;
  float mixRatio = 1.0 - step(progress, transitionEdge);

  vec4 transitionMix = mix(
    getFromColor(uv),
    getToColor(uv),
    mixRatio
  );
  
  float noiseEnvelope = smoothstep(progress-span, progress, transitionEdge) * (1.0 - smoothstep(progress, progress + span, transitionEdge));
  vec4 noise = vec4(vec3(rnd(uv*(1.0+progress))), transitionMix.a); // static where the image is
  

  return mix(transitionMix, noise, noiseEnvelope);
}
void main() { fragColor = transition(vTexCoord); }
