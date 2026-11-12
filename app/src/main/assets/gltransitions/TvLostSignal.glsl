#version 300 es
// TvLostSignal — from gl-transitions (MIT), author: mernking (gitlab.com/Godswork)
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
// Author: mernking (gitlab.com/Godswork)
// License: MIT
// Description: A crossfade with drifting tracking lines and tearing, like a TV losing its signal
// Tags: retro, glitch, scanlines

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

vec4 transition(vec2 uv) {

    float p = progress;
    float strength = sin(p * 3.14159265);

    vec2 tv = uv;

    vec4 fromColor = getFromColor(tv);
    vec4 toColor   = getToColor(tv);

    vec4 color = mix(fromColor, toColor, p);

    // horizontal tracking lines (key effect)
    float lineY = floor(tv.y * 120.0);

    float noise = hash(vec2(lineY, p * 20.0));

    float line = step(0.92, noise);

    // make lines drift during transition
    float drift =
        sin(tv.y * 30.0 + p * 10.0)
        * 0.02
        * strength;

    vec4 shiftedFrom = getFromColor(tv + vec2(drift, 0.0));
    vec4 shiftedTo   = getToColor(tv + vec2(drift, 0.0));

    vec4 lineColor = mix(shiftedFrom, shiftedTo, p);

    // apply tearing only on selected scanlines
    color = mix(color, lineColor, line * strength);

    // mild scanline darkening (CRT feel)
    float scan =
        sin(tv.y * 900.0) * 0.03;

    color.rgb -= scan * strength;

    return color;
}
void main() { fragColor = transition(vTexCoord); }
