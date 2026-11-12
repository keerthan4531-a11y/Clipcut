#version 300 es
// RotateScaleVanish — from gl-transitions (MIT), author: Mark Craig
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform bool FadeInSecond;
uniform bool ReverseEffect;
uniform bool ReverseRotation;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: Mark Craig
// mrmcsoftware on github and youtube ( http://www.youtube.com/MrMcSoftware )
// License: MIT
// Description: The outgoing image spins and shrinks away over the incoming one
// Tags: rotate, zoom, spin

// RotateScaleVanish Transition by Mark Craig (Copyright © 2022)

// @param Fade in the incoming image
// @param Run the effect in reverse
// @param Rotate the other way

#define M_PI 3.14159265358979323846
#define _TWOPI 6.283185307179586476925286766559

vec4 transition(vec2 uv)
{
vec2 iResolution = vec2(ratio, 1.0);
float t = ReverseEffect ? 1.0 - progress : progress;
float theta = ReverseRotation ? _TWOPI * t : -_TWOPI * t;
float c1 = cos(theta);
float s1 = sin(theta);
float rad = max(0.00001, 1.0 - t);
float xc1 = (uv.x - 0.5) * iResolution.x;
float yc1 = (uv.y - 0.5) * iResolution.y;
float xc2 = (xc1 * c1 - yc1 * s1) / rad;
float yc2 = (xc1 * s1 + yc1 * c1) / rad;
vec2 uv2 = vec2(xc2 + iResolution.x / 2.0, yc2 + iResolution.y / 2.0);
vec4 col3;
vec4 ColorTo = ReverseEffect ? getFromColor(uv) : getToColor(uv);
if ((uv2.x >= 0.0) && (uv2.x <= iResolution.x) && (uv2.y >= 0.0) && (uv2.y <= iResolution.y))
	{
	uv2 /= iResolution;
	col3 = ReverseEffect ? getToColor(uv2) : getFromColor(uv2);
	}
else { col3 = FadeInSecond ? vec4(0.0) : ColorTo; } // transparent around the shrinking image
return((1.0 - t) * col3 + t * ColorTo); // could have used mix
}
void main() { fragColor = transition(vTexCoord); }
