#version 300 es
// TopBottom — from gl-transitions (MIT), author: zhmy
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
// Author: zhmy
// License: MIT
// Description: The outgoing image shrinks away upward while the incoming one slides in from the bottom
// Tags: slide, zoom, vertical

const vec4 black = vec4(0.0); // where no image is drawn: transparent
const vec2 boundMin = vec2(0.0, 0.0);
const vec2 boundMax = vec2(1.0, 1.0);

bool inBounds (vec2 p) {
    return all(lessThan(boundMin, p)) && all(lessThan(p, boundMax));
}

vec4 transition (vec2 uv) {
    vec2 spfr,spto = vec2(-1.);
    float size = mix(1.0, 3.0, progress*0.2);
    spto = (uv + vec2(-0.5,-0.5))*vec2(size,size)+vec2(0.5,0.5);
    spfr = (uv + vec2(0.0, 1.0 - progress));
    if(inBounds(spfr)){
        return getToColor(spfr);
    } else if(inBounds(spto)){
        return getFromColor(spto) * (1.0 - progress);
    } else{
        return black;
    }
}
void main() { fragColor = transition(vTexCoord); }
