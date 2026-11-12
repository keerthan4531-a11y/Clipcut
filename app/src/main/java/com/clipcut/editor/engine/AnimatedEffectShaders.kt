package com.clipcut.editor.engine

/**
 * Time-driven "animated" effects.
 *
 * Every shader here is stateless: motion comes from `uTime`, which
 * [ShaderProgram] already feeds the frame's presentation timestamp into, so no per-frame
 * Bitmap or CPU work is involved and the same effect looks identical in preview and export.
 *
 * Two rules keep these cheap enough for mid-range Android GPUs:
 *  - each pixel does a fixed, small number of texture reads (12 max), never a loop over
 *    samples;
 *  - particle effects are procedural (hash-based) instead of texture lookups, so there is
 *    nothing to upload and no extra VRAM.
 */
object AnimatedEffectShaders {

    private val H = """#version 300 es
precision highp float;
uniform sampler2D uTexSampler;
uniform float uTime;
uniform float uIntensity;
in vec2 vTexCoord;
out vec4 fragColor;
"""

    private val HASH = """
float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}
float noise21(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}
float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }
"""

    /** Blocky datamosh-style tearing with an RGB split. */
    fun glitch(intensity: Float) = ShaderEffect(
        FRAG_GLITCH,
        mapOf("uIntensity" to intensity.coerceIn(0f, 1f)),
        effectName = "Glitch"
    )

    /** Tape-era look: chroma bleed, scanlines, rolling tracking bar. */
    fun vhs(intensity: Float) = ShaderEffect(
        FRAG_VHS,
        mapOf("uIntensity" to intensity.coerceIn(0f, 1f)),
        effectName = "VHS"
    )

    /** Animated luminance grain, strongest in the midtones. */
    fun filmGrain(amount: Float) = ShaderEffect(
        FRAG_GRAIN,
        mapOf("uIntensity" to amount.coerceIn(0f, 1f)),
        effectName = "Film grain"
    )

    /** Warm organic light leak sweeping across the frame. */
    fun lightLeak(intensity: Float) = ShaderEffect(
        FRAG_LIGHT_LEAK,
        mapOf("uIntensity" to intensity.coerceIn(0f, 1f)),
        effectName = "Light leak"
    )

    /** Drifting anamorphic flare with a horizontal streak. */
    fun lensFlare(intensity: Float) = ShaderEffect(
        FRAG_LENS_FLARE,
        mapOf("uIntensity" to intensity.coerceIn(0f, 1f)),
        effectName = "Lens flare"
    )

    fun snow(intensity: Float) = ShaderEffect(
        FRAG_SNOW,
        mapOf("uIntensity" to intensity.coerceIn(0f, 1f)),
        effectName = "Snow"
    )

    fun rain(intensity: Float) = ShaderEffect(
        FRAG_RAIN,
        mapOf("uIntensity" to intensity.coerceIn(0f, 1f)),
        effectName = "Rain"
    )

    fun sparks(intensity: Float) = ShaderEffect(
        FRAG_SPARKS,
        mapOf("uIntensity" to intensity.coerceIn(0f, 1f)),
        effectName = "Sparks"
    )

    fun bokeh(intensity: Float) = ShaderEffect(
        FRAG_BOKEH,
        mapOf("uIntensity" to intensity.coerceIn(0f, 1f)),
        effectName = "Bokeh"
    )

    /** Flickering projector: brightness pulsing, dust, scratches, heavy vignette. */
    fun oldFilm(intensity: Float) = ShaderEffect(
        FRAG_OLD_FILM,
        mapOf("uIntensity" to intensity.coerceIn(0f, 1f)),
        effectName = "Old film"
    )

    /** Pulsing neon edge glow driven by the frame's own edges. */
    fun neonPulse(intensity: Float) = ShaderEffect(
        FRAG_NEON,
        mapOf("uIntensity" to intensity.coerceIn(0f, 1f)),
        effectName = "Neon pulse"
    )

    /** Handheld camera jitter with a slight breathing zoom. */
    fun cameraShake(intensity: Float) = ShaderEffect(
        FRAG_SHAKE,
        mapOf("uIntensity" to intensity.coerceIn(0f, 1f)),
        effectName = "Camera shake"
    )

    private val FRAG_GLITCH = H + HASH + """
void main() {
    vec2 uv = vTexCoord;
    float band = floor(uv.y * 26.0);
    float tick = floor(uTime * 11.0);
    float jitter = (hash21(vec2(band, tick)) - 0.5) * 0.16 * uIntensity;
    float gate = step(0.55, hash21(vec2(band * 1.7, tick * 0.5)));
    uv.x = clamp(uv.x + jitter * gate, 0.0, 1.0);
    float split = (0.0025 + 0.004 * gate) * uIntensity;
    vec4 base = texture(uTexSampler, uv);
    float r = texture(uTexSampler, clamp(uv + vec2(split, 0.0), 0.0, 1.0)).r;
    float b = texture(uTexSampler, clamp(uv - vec2(split, 0.0), 0.0, 1.0)).b;
    vec3 c = vec3(r, base.g, b);
    // Occasional bright tracking band.
    float scan = step(0.93, hash21(vec2(floor(uTime * 6.0), band)));
    c += scan * gate * 0.08 * uIntensity;
    fragColor = vec4(c, base.a);
}
"""

    private val FRAG_VHS = H + HASH + """
void main() {
    vec2 uv = vTexCoord;
    float wobble = (noise21(vec2(uv.y * 40.0, uTime * 3.0)) - 0.5) * 0.01 * uIntensity;
    uv.x = clamp(uv.x + wobble, 0.0, 1.0);
    float bleed = 0.004 * uIntensity;
    vec3 c;
    c.r = texture(uTexSampler, clamp(uv + vec2(bleed, 0.0), 0.0, 1.0)).r;
    c.g = texture(uTexSampler, uv).g;
    c.b = texture(uTexSampler, clamp(uv - vec2(bleed, 0.0), 0.0, 1.0)).b;
    // Rolling tracking bar.
    float bar = fract(uv.y + uTime * 0.35);
    float tracking = smoothstep(0.0, 0.06, bar) * (1.0 - smoothstep(0.06, 0.14, bar));
    c += tracking * 0.12 * uIntensity;
    // Scanlines.
    float scan = 0.5 + 0.5 * sin(uv.y * 900.0);
    c *= 1.0 - 0.08 * scan * uIntensity;
    // Tape noise.
    float grain = (hash21(uv * vec2(720.0, 480.0) + uTime) - 0.5) * 0.14 * uIntensity;
    c += grain;
    fragColor = vec4(clamp(c, 0.0, 1.0), texture(uTexSampler, uv).a);
}
"""

    private val FRAG_GRAIN = H + HASH + """
void main() {
    vec4 base = texture(uTexSampler, vTexCoord);
    float frame = floor(uTime * 24.0);
    float n = hash21(vTexCoord * vec2(1920.0, 1080.0) + frame * 37.0) - 0.5;
    float l = luma(base.rgb);
    // Grain peaks in the midtones and fades out of pure black/white.
    float weight = 1.0 - abs(l - 0.5) * 1.4;
    vec3 c = base.rgb + n * 0.22 * uIntensity * clamp(weight, 0.15, 1.0);
    fragColor = vec4(clamp(c, 0.0, 1.0), base.a);
}
"""

    private val FRAG_LIGHT_LEAK = H + HASH + """
void main() {
    vec4 base = texture(uTexSampler, vTexCoord);
    float t = uTime * 0.22;
    vec2 p = vTexCoord - vec2(0.5 + 0.45 * sin(t), 0.5 + 0.3 * cos(t * 0.8));
    float d = length(p * vec2(1.6, 1.0));
    float leak = exp(-d * 2.6);
    vec3 tint = mix(vec3(1.0, 0.55, 0.2), vec3(1.0, 0.85, 0.45), 0.5 + 0.5 * sin(t));
    vec3 c = base.rgb + tint * leak * 0.75 * uIntensity;
    // Screen blend keeps highlights from clipping to flat white.
    c = 1.0 - (1.0 - base.rgb) * (1.0 - clamp(tint * leak * uIntensity, 0.0, 1.0));
    fragColor = vec4(clamp(c, 0.0, 1.0), base.a);
}
"""

    private val FRAG_LENS_FLARE = H + HASH + """
void main() {
    vec4 base = texture(uTexSampler, vTexCoord);
    float t = uTime * 0.35;
    vec2 center = vec2(0.5 + 0.32 * sin(t), 0.55 + 0.22 * sin(t * 1.7));
    vec2 d = (vTexCoord - center) * vec2(1.7, 1.0);
    float core = exp(-length(d) * 22.0);
    float halo = exp(-length(d) * 5.0) * 0.35;
    float streak = exp(-abs(d.y) * 90.0) * exp(-abs(d.x) * 2.2) * 0.5;
    float ghost = exp(-length(vTexCoord - (1.0 - center)) * 9.0) * 0.18;
    vec3 flare = vec3(1.0, 0.93, 0.8) * (core + halo + streak) + vec3(0.5, 0.7, 1.0) * ghost;
    vec3 c = 1.0 - (1.0 - base.rgb) * (1.0 - clamp(flare * uIntensity, 0.0, 1.0));
    fragColor = vec4(clamp(c, 0.0, 1.0), base.a);
}
"""

    private val FRAG_SNOW = H + HASH + """
float flakes(vec2 uv, float scale, float speed, float seed) {
    vec2 grid = uv * scale;
    vec2 cell = floor(grid);
    vec2 f = fract(grid);
    float r = hash21(cell + seed);
    vec2 pos = vec2(hash21(cell + seed + 3.1), fract(r - uTime * speed));
    float d = length((f - pos) * vec2(1.0, 1.0));
    return smoothstep(0.16, 0.0, d) * step(0.45, r);
}
void main() {
    vec4 base = texture(uTexSampler, vTexCoord);
    float s = 0.0;
    s += flakes(vTexCoord, 14.0, 0.09, 0.0) * 0.9;
    s += flakes(vTexCoord + vec2(0.13, 0.0), 22.0, 0.15, 5.0) * 0.6;
    s += flakes(vTexCoord + vec2(0.37, 0.0), 34.0, 0.23, 9.0) * 0.4;
    vec3 c = mix(base.rgb, vec3(1.0), clamp(s * uIntensity, 0.0, 1.0));
    fragColor = vec4(c, base.a);
}
"""

    private val FRAG_RAIN = H + HASH + """
void main() {
    vec2 uv = vTexCoord;
    vec4 base = texture(uTexSampler, uv);
    float rain = 0.0;
    for (int i = 0; i < 3; i++) {
        float scale = 26.0 + float(i) * 22.0;
        float speed = 2.2 + float(i) * 0.9;
        vec2 grid = vec2(uv.x * scale, uv.y * scale * 0.35);
        grid.y += uTime * speed;
        vec2 cell = floor(grid);
        vec2 f = fract(grid);
        float r = hash21(cell + float(i) * 13.0);
        float streak = smoothstep(0.06, 0.0, abs(f.x - 0.5)) * smoothstep(0.9, 0.2, f.y);
        rain += streak * step(0.62, r) * (1.0 - float(i) * 0.25);
    }
    vec3 c = base.rgb * (1.0 - 0.16 * uIntensity) + vec3(0.75, 0.82, 0.9) * rain * 0.5 * uIntensity;
    fragColor = vec4(clamp(c, 0.0, 1.0), base.a);
}
"""

    private val FRAG_SPARKS = H + HASH + """
void main() {
    vec4 base = texture(uTexSampler, vTexCoord);
    float glow = 0.0;
    for (int i = 0; i < 3; i++) {
        float scale = 12.0 + float(i) * 9.0;
        vec2 grid = vTexCoord * vec2(scale, scale * 0.8);
        vec2 cell = floor(grid);
        vec2 f = fract(grid);
        float r = hash21(cell + float(i) * 7.3);
        float rise = fract(r + uTime * (0.25 + 0.15 * float(i)));
        vec2 pos = vec2(0.5 + (hash21(cell + 2.0) - 0.5) * 0.6, rise);
        float d = length(f - pos);
        float ember = smoothstep(0.12, 0.0, d) * step(0.55, r);
        ember *= smoothstep(1.0, 0.55, rise) * smoothstep(0.0, 0.15, rise);
        glow += ember;
    }
    vec3 c = base.rgb + vec3(1.0, 0.55, 0.15) * glow * 1.4 * uIntensity;
    fragColor = vec4(clamp(c, 0.0, 1.0), base.a);
}
"""

    private val FRAG_BOKEH = H + HASH + """
void main() {
    vec4 base = texture(uTexSampler, vTexCoord);
    float glow = 0.0;
    for (int i = 0; i < 4; i++) {
        float scale = 5.0 + float(i) * 4.0;
        vec2 grid = vTexCoord * scale;
        vec2 cell = floor(grid);
        vec2 f = fract(grid);
        float r = hash21(cell + float(i) * 11.7);
        vec2 drift = vec2(
            hash21(cell + 1.0) + 0.05 * sin(uTime * 0.6 + r * 6.28),
            hash21(cell + 4.0) + 0.05 * cos(uTime * 0.5 + r * 6.28)
        );
        float d = length(f - drift);
        float circle = smoothstep(0.42, 0.36, d);
        glow += circle * step(0.72, r) * (1.0 - float(i) * 0.18);
    }
    vec3 tint = mix(vec3(1.0, 0.85, 0.6), vec3(0.65, 0.8, 1.0), 0.45);
    vec3 c = 1.0 - (1.0 - base.rgb) * (1.0 - clamp(glow * 0.55 * uIntensity * tint, 0.0, 1.0));
    fragColor = vec4(clamp(c, 0.0, 1.0), base.a);
}
"""

    private val FRAG_OLD_FILM = H + HASH + """
void main() {
    vec2 uv = vTexCoord;
    // Gate weave: the frame drifts very slightly like a loose projector.
    uv.x += (noise21(vec2(uTime * 2.0, 1.0)) - 0.5) * 0.0025 * uIntensity;
    vec4 base = texture(uTexSampler, clamp(uv, 0.0, 1.0));
    // Flicker.
    float flicker = 1.0 + (noise21(vec2(uTime * 18.0, 3.0)) - 0.5) * 0.16 * uIntensity;
    vec3 c = base.rgb * flicker;
    // Dust and short scratches.
    float dust = step(0.9975, hash21(floor(uv * vec2(420.0, 260.0)) + floor(uTime * 20.0)));
    c = mix(c, vec3(1.0), dust * 0.7 * uIntensity);
    float scratchX = hash21(vec2(floor(uTime * 3.0), 7.0));
    float scratch = smoothstep(0.0022, 0.0, abs(uv.x - scratchX)) * uIntensity * 0.35;
    c = mix(c, vec3(0.95, 0.95, 0.9), scratch);
    // Vignette.
    float vig = smoothstep(1.15, 0.35, length((uv - 0.5) * vec2(1.5, 1.0)));
    c *= mix(1.0, vig, 0.65 * uIntensity);
    fragColor = vec4(clamp(c, 0.0, 1.0), base.a);
}
"""

    private val FRAG_NEON = H + HASH + """
void main() {
    vec2 uv = vTexCoord;
    vec4 base = texture(uTexSampler, uv);
    float stepU = 1.0 / 540.0;
    float l = luma(base.rgb);
    float lx = luma(texture(uTexSampler, uv + vec2(stepU, 0.0)).rgb);
    float ly = luma(texture(uTexSampler, uv + vec2(0.0, stepU)).rgb);
    float edge = clamp((abs(l - lx) + abs(l - ly)) * 6.0, 0.0, 1.0);
    float pulse = 0.55 + 0.45 * sin(uTime * 3.2);
    vec3 neon = mix(vec3(0.2, 0.9, 1.0), vec3(1.0, 0.25, 0.85), 0.5 + 0.5 * sin(uTime * 1.7));
    vec3 c = base.rgb + neon * edge * pulse * 1.6 * uIntensity;
    fragColor = vec4(clamp(c, 0.0, 1.0), base.a);
}
"""

    private val FRAG_SHAKE = H + HASH + """
void main() {
    float amp = 0.012 * uIntensity;
    float t = uTime * 9.0;
    vec2 offset = vec2(noise21(vec2(t, 1.0)) - 0.5, noise21(vec2(2.0, t)) - 0.5) * 2.0 * amp;
    // Breathing zoom keeps the shifted edges covered.
    float zoom = 1.0 + amp * 2.0;
    vec2 uv = clamp((vTexCoord - 0.5) / zoom + 0.5 + offset, 0.0, 1.0);
    fragColor = texture(uTexSampler, uv);
}
"""
}
