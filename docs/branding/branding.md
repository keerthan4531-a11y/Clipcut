# ClipCut brand sheet

The ClipCut identity is a **film frame severed by a blade of light**. The frame keeps the
idea of a timeline strip; the diagonal cut is the edit itself. It stays legible down to
48 px, and every layer below is generated from one geometry definition, so the launcher
icon, the store graphic and the vector master never drift apart.

## Files

| File | Use |
| --- | --- |
| `clipcut-logo.png` | Hero logo (rendered artwork, 1250 px) |
| `clipcut-logo.svg` | Vector banner master: mark + wordmark + tagline |
| `clipcut-mark.svg` | Vector app mark on its midnight plate |
| `clipcut-icon-1024.png` | Square app icon master |
| `clipcut-banner.png` | README banner (1600 × 520) |
| `../../icon.png` | Repository icon (1024 px) |
| `../../app/src/main/res/mipmap-*/` | Launcher icons: legacy, round, adaptive foreground, monochrome |
| `../../fastlane/metadata/android/en-US/images/` | Play icon (512 px) and feature graphic (1024 × 500) |

Regenerate the raster set with `python3 tools/make_icons.py` from the repository root
(requires Pillow and NumPy). The script supersamples 2× and emits every mipmap density,
keeping the mark inside the 66 dp adaptive safe zone.

## Palette

| Token | Hex | Where |
| --- | --- | --- |
| Midnight plate (top) | `#131A30` | icon plate gradient start |
| Midnight plate (bottom) | `#080C1A` | icon plate gradient end |
| Adaptive background | `#0B1020` | `ic_launcher_background`, plate mid-tone |
| Cream frame | `#F5F1E8` | the film frame and sprocket holes |
| Sapphire | `#89B4FA` | blade gradient start (Catppuccin *blue*) |
| Mauve | `#CBA6F7` | blade gradient end (Catppuccin *mauve*) |
| Tagline grey | `#A9B0BC` | banner supporting type |

The accents are the Catppuccin Mocha tokens the app UI already uses (`ui/theme/Tokens.kt`,
`res/values/colors.xml`), so the icon sits inside the product's own colour system rather
than on top of it.

## Geometry

Design space is 1000 × 1000 with the plate corner radius at 230.

- Frame: outer box 238 → 762, corner radius 104, stroke 52, cream.
- Sprocket holes: six 52 × 52 squares, centres at x = 373 / 500 / 627, y rows at 352 and 596.
- Blade: a 38-wide line from (272, 728) to (728, 272) — i.e. exactly corner to corner, so
  the cut severs both corners symmetrically — graded sapphire → mauve, round caps.
- Cut gap: 60 wide, erased from the frame and holes with a mask before the blade is drawn.
- Glow: the blade duplicated at 30 % opacity with a 14-unit blur.

## Typography

The wordmark is set in a transitional serif (Georgia, falling back to DejaVu Serif Bold)
with −2 tracking at display size: it reads as a premium tool, not a phone toy. The tagline
is a 600-weight grotesque in wide caps (`letter-spacing: 7`).

## Usage rules

- Keep clear space of at least 25 % of the mark's width on every side.
- Never place the mark on a light background; the plate is part of the identity.
- Never recolour the blade outside the sapphire → mauve range, and never rotate the mark.
- Never crop the plate to a square corner; it is always rounded (or masked by the system).
- The monochrome layer is white ink only — no glow, no gradient.
