#version 300 es
// Slides — from gl-transitions (MIT), author: Mark Craig
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
uniform int type;
uniform bool In;
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) { return texture(uFromTexSampler, uv); }
vec4 getToColor(vec2 uv) { return texture(uToTexSampler, uv); }
// Author: Mark Craig
// mrmcsoftware on github and youtube ( http://www.youtube.com/MrMcSoftware )
// License: MIT
// Description: The outgoing image slides out, or the incoming one slides in, toward an edge, a corner or the center
// Tags: slide, directional

// Slides Transition by Mark Craig (Copyright © 2022)

// @param Slide the incoming image in instead of the outgoing one out
// type: slide to/from which edge, which corner, or center
// In: if true slide new image in, otherwise slide old image out

#define rad2 rad / 2.0

vec4 transition(vec2 uv)
{
vec2 uv0 = uv;
float rad = In ? progress : 1.0 - progress;
float xc1, yc1;
// I used if/else instead of switch in case it's an old GPU
if (type == 0) { xc1 = .5 - rad2; yc1 = 0.0; }
else if (type == 1) { xc1 = 1.0 - rad; yc1 = .5 - rad2; }
else if (type == 2) { xc1 = .5 - rad2; yc1 = 1.0 - rad; }
else if (type == 3) { xc1 = 0.0; yc1 = .5 - rad2; }
else if (type == 4) { xc1 = 1.0 - rad; yc1 = 0.0; }
else if (type == 5) { xc1 = 1.0 - rad; yc1 = 1.0 - rad; }
else if (type == 6) { xc1 = 0.0; yc1 = 1.0 - rad; }
else if (type == 7) { xc1 = 0.0; yc1 = 0.0; }
else if (type == 8) { xc1 = .5 - rad2; yc1 = .5 - rad2; }
uv.y = 1.0 - uv.y;
vec2 uv2;
if ((uv.x >= xc1) && (uv.x <= xc1 + rad) && (uv.y >= yc1) && (uv.y <= yc1 + rad))
	{
	uv2 = vec2((uv.x - xc1) / rad, 1.0 - (uv.y - yc1) / rad);
	return(In ? getToColor(uv2) : getFromColor(uv2));
	}
return(In ? getFromColor(uv0) : getToColor(uv0));
}
void main() { fragColor = transition(vTexCoord); }
