# ClipCut — Filters, Animated Effects & AI: build plan

Audited against the tree at `v1.0.0` (ClearCut 3.82.0 engine). Everything below cites the
file that has to change, so each item is an actual work order, not a wish list.

---

## 1. What ClipCut actually ships today

| Area | Reality in code | Verdict |
| --- | --- | --- |
| Effect types | `model/Effect.kt` — **40 `EffectType`s**: FILTER 14, COLOR 10, BLUR 6, DISTORTION 6, KEYING 2, SPEED 2. All 40 wired in `engine/EffectBuilder.kt`. | Real, but all *static* and generic (Sepia, Vignette, Posterize…). CapCut ships ~150 *named looks*. |
| Shader factory | `engine/ShaderEffect.kt` — `object EffectShaders` with **67 shader functions**. `ShaderEffect` already accepts `dynamicUniforms: ((Long) -> Map<String, Float>)`. | The animation hook already exists and is unused. |
| Transitions | `model/Timeline.kt:24` — **37 `TransitionType`s**, all implemented at `EffectBuilder.kt:404+` as wipes/slides/zooms/dissolves. Global (timeline-level) transitions: only **4** (`Timeline.kt:112`). | 37 basic 2D moves, zero stylised/3D/particle transitions. |
| Filter library (LUT) | `engine/LutEngine.kt` + `LutRegistry.kt` exist; **no bundled LUT presets anywhere** (`grep builtinLuts/presetLuts` → 0 hits). | User can import a cube file, but there is no library to browse. This is why "filters are few". |
| AI — working | `ai/AiFeatures.kt`: Whisper ASR captions (`engine/whisper/WhisperEngine.kt`, model **downloaded at runtime** from Hugging Face), scene detect, stabilise, denoise, smart crop, motion track, LaMa inpainting. | Works, but only after a model download. |
| AI — stubs | `FrameInterpolationEngine`, `UpscaleEngine`, `VideoMattingEngine`, `StyleTransferEngine` | **All four are stubs.** These are exactly CapCut's headline features. |
| Segmentation | `engine/segmentation/SegmentationEngine.kt` — MediaPipe selfie segmenter, downloaded tflite, gated behind a **consent toggle** in Settings. | Works, needs the toggle flipped. |

**Why it feels empty:** the four AI engines people expect from CapCut are stubs; captions
silently degrade to "timing only" when the Whisper model was never downloaded; and there is
no filter library at all — 40 generic sliders instead of a wall of looks.

---

## 2. Gap vs CapCut

| CapCut | ClipCut today | Close it with |
| --- | --- | --- |
| 150+ named filters (LUT looks) | 0 library, import only | LUT pack + thumbnail grid (§3.2) |
| 250+ transitions incl. stylised | 37 plain 2D | gl-transitions, 193 MIT shaders (§3.1) |
| Animated overlays / light leaks / particles | none | time-driven shaders (§3.3) |
| Auto cutout / background removal | stub + selfie-only seg | BiRefNet / IS-Net ONNX (§4.1) |
| Smooth slow-mo (frame interpolation) | stub | RIFE v4.9 ONNX (§4.2) |
| AI upscale | stub | Real-ESRGAN ncnn (§4.3) |
| Portrait blur / depth bokeh | none | Depth Anything V2 small (§4.4) |
| TTS voiceover | Android system TTS only | Piper via sherpa-onnx (§4.5) |
| Auto captions | Whisper (already) | ship the model, add VAD + model picker (§4.6) |
| Templates / one-tap edits | basic auto-edit planner | effect packs + keyframed stacks (§3.3) |

---

## 3. Visual: the fastest 10x (no ML, no new runtime)

### 3.1 193 transitions — `gl-transitions` (MIT)

<https://github.com/gl-transitions/gl-transitions> — 193 community transitions, MIT, and
the repo ships **`glsl3/<name>.glsl` explicitly for "OpenGL ES 3 and Android Media3"**. That
is the same GLSL ES 3.00 flavour ClipCut's `ShaderEffect` already compiles.

Work order:
1. Vendor `glsl3/*.glsl` into `app/src/main/assets/gl-transitions/` (≈ 193 files, ~400 KB).
2. Add `engine/GlTransitionCatalog.kt` — parse `name`, `params`, and the
   `vec4 transition(vec2 uv)` body; wrap each into ClipCut's `ShaderEffect` (uniforms:
   `progress`, plus declared params with defaults).
3. Map catalog entries onto the existing selection path: `TransitionType` (`model/Timeline.kt:24`)
   is an enum, so either (a) generate one enum entry per transition, or (b) — better — add a
   `CUSTOM(shaderId: String)` entry and keep the catalog data-driven. Parsed JSON in
   `assets/` + a `TransitionCatalogEntry(id, displayName, params, category)` model.
4. Bridge in `EffectBuilder.kt:404`: `TransitionType.CUSTOM -> EffectShaders.glTransition(id, params, durationUs)`.
5. Import/export: `TimelineExchangeValidator` and `ProjectAutoSave` must accept the new id
   (unknown ids already fall back safely — verify the fallback is non-destructive).

Two-source blend caveat: gl-transitions take `from` + `to` textures and `progress`. ClipCut's
current transitions are single-input effects applied per clip. The clean integration is a
`GlTransitionEffect` that implements `GlShaderProgram` with two samplers and is fed
`(progress = presentationTimeUs / durationUs)`; the timeline already knows both neighbours at
a boundary, so the effect can be attached to the outgoing clip while the incoming frame is
bound to the second sampler.

### 3.2 Filter library — LUT looks (CapCut's actual "filters")

The engine already reads `.cube` LUTs (`engine/LutEngine.kt`, `LutRegistry.kt`). What is
missing is a *library*:

1. Ship 24–40 CC0 / self-authored `.cube` LUTs in `assets/luts/<pack>/` (2 KB each).
   **Do not** pull LUTs from random Pinterest packs — most are unlicensed. Safe sources:
   self-generated grades, CC0 packs, or LUTs whose licence file ships in the pack.
2. Add `model/FilterPreset.kt` + `engine/FilterPresetCatalog.kt` (id, name, category,
   LUT asset, intensity 0–1 → mix with the original pixel).
3. Import path: `lut3d` filter in FFmpeg (already bundled via FFmpegKitNext) **or** a GL
   `sampler3D` LUT shader for preview. Prefer the GL path so preview and export match.
4. UI: a `FilterLibraryPanel` — horizontally scrolling thumbnails rendered with
   `EffectPreviewRenderer` (already exists), intensity slider, "apply to all clips" action.
5. Extend `EffectShareEngine` packs so a user can share a filter pack.

This alone changes the app's perceived capability more than anything else: a wall of looks
beats 40 sliders.

### 3.3 Animated effects (light leaks, glitch, particles, film burns)

`ShaderEffect`/`EffectShaders` already take `dynamicUniforms` with `presentationTimeUs`. The
missing pieces are (a) assets and (b) authoring a few shaders that use `uTime`:

| Effect | Shader idea | Params |
| --- | --- | --- |
| Light leak | 3 moving radial gradient blobs, screen blend | speed, hue, intensity |
| Film burn | fbm noise thresholded, warm ramp | threshold, spread |
| Glitch / datamosh | row offset from hash(floor(t*24)), RGB split | amount, rate |
| VHS | scanlines + chroma shift + noise + tracking wobble | intensity |
| Particles (dust/snow/fireflies) | hash grid sprite points, additive | density, size, speed |
| Zoom pulse / ken-burns | animated uv scale anchored to a point | zoom, anchor |
| Shake / impact | per-frame bezier offset, motion-blur sample | amplitude, decay |

Add them as `EffectCategory.ANIMATED` in `Effect.kt` + branches in `EffectBuilder.kt`, and
gate on `RenderDegradationLedger` so heavy effects degrade instead of dropping frames.

---

## 4. AI: real models, licence-safe only

**Rule for this repo (MIT):** no GPL weights in the APK. That immediately rules out
**RVM / RobustVideoMatting (GPL-3.0)** which the upstream README proposed for matting, and
**MODNet weights (CC BY-NC-SA, non-commercial)**. Use the permissive column only.

| # | Feature | Model | Licence | Runtime | Size | Integration point |
| --- | --- | --- | --- | --- | --- | --- |
| 4.1 | Cutout / background remove-replace-blur | **BiRefNet** (birefnet-general-lite) ONNX, or IS-Net (Apache-2.0), or existing MediaPipe selfie seg for the cheap path | MIT / Apache-2.0 | ONNX Runtime (bundled) | 40–90 MB | `VideoMattingEngine` (stub) → matte → `SegmentationGlEffect` compositing |
| 4.2 | Smooth slow-mo / FPS boost | **RIFE v4.9 ONNX** (`rife49.onnx`, weights MIT) or `rife-ncnn-vulkan` (MIT) | MIT | ONNX Runtime / ncnn+Vulkan | 20 MB | `FrameInterpolationEngine` (stub); timeline already has speed effects (`EffectCategory.SPEED`) |
| 4.3 | AI upscale | **Real-ESRGAN** (`real-esrgan-ncnn-vulkan`, MIT; ncnn is BSD-3) | MIT / BSD-3 | ncnn Vulkan | 20–40 MB | `UpscaleEngine` (stub) → per-frame tile upscale before mux |
| 4.4 | Portrait blur / depth bokeh | **Depth Anything V2 small** ONNX | Apache-2.0 | ONNX Runtime | 25 MB | new `DepthBokehEngine` → depth map → blur shader with depth ramp |
| 4.5 | Voiceover (real TTS voices) | **Piper** voices via `sherpa-onnx` (AAR already referenced in `whisper/SherpaAsrEngine.kt`) | Apache-2.0 | sherpa-onnx | 20–60 MB/voice | `TtsEngine` (system TTS today) |
| 4.6 | Captions | **Whisper tiny/base/small** ONNX (already wired) + Silero VAD | MIT | ONNX Runtime | 40–240 MB | `whisper/WhisperEngine.kt` — add model picker + VAD so 30-min clips stop timing out |
| 4.7 | Face retouch / beauty | **MediaPipe Face Landmarker** + shader warp | Apache-2.0 | MediaPipe (bundled) | 4 MB | new panel + `ShaderEffect` (skin smooth, teeth, eye size) |
| 4.8 | Smart cutout by tap | **MediaPipe Interactive Segmenter** | Apache-2.0 | MediaPipe (bundled) | 6 MB | pairs with existing LaMa inpainting for object *removal* |
| 4.9 | Style transfer | **AnimeGANv2** ONNX (code MIT — verify weights before shipping) | MIT? | ONNX Runtime | 8 MB | `StyleTransferEngine` (stub) |

**Delivery:** every model stays out of the APK. Extend the existing
`engine/ModelDownloadManager.kt` into a proper catalogue: id, sha256, size, licence text,
download URL, Wi-Fi-only toggle, resumable download, integrity check, delete. The app
already has the gate pattern (`WhisperEngine.isReady()`) — reuse it, and make the UI say
"AI model not downloaded" instead of silently falling back to a fake result.

---

## 5. Phased roadmap

| Phase | Scope | Effort | What the user sees |
| --- | --- | --- | --- |
| **0** | Audit fixes: surface all 40 effects in the library panel, say "model not downloaded" instead of timing-only captions, one-tap MediaPipe consent prompt | 2–3 days | Nothing crashes, nothing lies |
| **1** | gl-transitions (193) + 24 LUT filters + 12 animated effects + filter/transition browser UI | 2–3 weeks | "Whoa, this is CapCut" — biggest perceived jump, zero ML |
| **2** | Matting (BiRefNet) + depth bokeh + RIFE slow-mo | 3–4 weeks | Background removal, 60 fps slow-mo |
| **3** | Upscale + style transfer + Piper TTS + face retouch | 3–4 weeks | "Better than CapCut" territory |
| **4** | Effect packs/templates, keyframed effect stacks, GPU budget governor, per-effect thumbnail cache | 2–3 weeks | One-tap templates, shareable looks |

**Where ClipCut can beat CapCut rather than copy it:** everything on-device and offline
(CapCut exports most AI to the cloud + a subscription), no watermark, open effect-pack
format, project files you own, and ClipCut's already-superior export honesty
(`ExportColorPreflight`, verified export history).

---

## 6. Constraints to respect in every phase

- **APK size**: the universal APK is already 350 MB. Models and even big effect packs must be
  runtime downloads, never bundled.
- **Colour pipeline**: `ShaderEffect` runs in linear RGB when `useHdr`; every new shader needs
  the same `linearToSrgb`/`srgbToLinear` discipline or HDR footage will look wrong.
- **16 KB page alignment** and the native supply-chain gate (`scripts/check_16kb_alignment.py`,
  `verify_native_supply_chain.py`) apply to any new `.so` (ncnn for RIFE/Real-ESRGAN).
- **Dependency verification** is pinned in `gradle/verification-metadata.xml`; new deps need
  their checksums added.
- **Licences**: keep the §4 permissive column; add each model's licence to `NOTICE` and to the
  in-app open-source screen (`generateRuntimeOpenSourceNotices`).
- **Thermal**: a phone cannot run RIFE at 1080p30 continuously. Design as an offline
  "process then play" pass, not live preview, and show a progress + ETA like the existing
  export sheet does.

---

## Appendix A — starter code

### A.1 A time-driven (animated) filter, in ClipCut's existing style

`EffectShaders` already passes `uTime` through `dynamicUniforms`; the shader below drops
straight into `engine/ShaderEffect.kt` and is referenced from `EffectBuilder.kt`.

```glsl
precision mediump float;
uniform sampler2D uTexSampler;
uniform float uTime;        // seconds, from dynamicUniforms
uniform float uAmount;      // 0..1 strip intensity
varying vec2 vTexSamplingCoord;

float hash(float n) { return fract(sin(n) * 43758.5453); }

void main() {
    vec2 uv = vTexSamplingCoord;
    // per-row horizontal tearing, re-seeded 24 times a second
    float row = floor(uv.y * 60.0);
    float tick = floor(uTime * 24.0);
    float tear = (hash(row + tick * 0.618) - 0.5) * 0.08 * uAmount;
    uv.x += tear;

    vec4 c;
    c.r = texture2D(uTexSampler, uv + vec2(0.004 * uAmount, 0.0)).r;  // chroma split
    c.g = texture2D(uTexSampler, uv).g;
    c.b = texture2D(uTexSampler, uv - vec2(0.004 * uAmount, 0.0)).b;
    c.a = 1.0;

    // scanlines + faint noise
    float scan = 0.94 + 0.06 * sin(uv.y * 900.0 + uTime * 12.0);
    c.rgb *= scan;
    c.rgb += (hash(uv.x * uv.y + tick) - 0.5) * 0.05 * uAmount;
    gl_FragColor = c;
}
```

### A.2 The two-input transition adapter (gl-transitions → Media3)

```kotlin
@UnstableApi
class GlTransitionEffect(
    private val fragmentShader: String,          // "vec4 transition(vec2 uv){...}" + uniforms
    private val durationUs: Long,
) : GlEffect {

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        ShaderProgram(fragmentShader, uniforms = emptyMap(),
            dynamicUniforms = { timeUs ->
                mapOf("progress" to (timeUs.toFloat() / durationUs).coerceIn(0f, 1f))
            },
            useHdr = useHdr,
            effectName = "GL transition")
}
```

Two notes that bite:
1. gl-transitions sample two textures (`from`, `to`). Media3's progress-based effects receive
   the *composited* frame, so either feed the neighbour frame through an extra `OverlayEffect`
   sampler, or pre-render the boundary pair. ClipCut already resolves both clips at a
   boundary, so expose that second texture explicitly rather than faking it.
2. Convert each `glsl3/*.glsl` to ClipCut's attribute/uniform names (`uTexSampler`,
   `vTexSamplingCoord`) — the bodies are portable, the I/O names are not.

### A.3 LUT look with intensity

```glsl
precision mediump float;
uniform sampler2D uTexSampler;
uniform sampler3D uLut;      // 33^3 via a 2D atlas on older GL
uniform float uIntensity;    // 0 = original, 1 = full look
varying vec2 vTexSamplingCoord;

void main() {
    vec4 src = texture2D(uTexSampler, vTexSamplingCoord);
    vec3 graded = texture(uLut, src.rgb).rgb;
    gl_FragColor = vec4(mix(src.rgb, graded, uIntensity), src.a);
}
```
