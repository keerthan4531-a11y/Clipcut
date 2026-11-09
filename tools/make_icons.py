#!/usr/bin/env python3
"""Render the ClipCut icon system from one geometry definition.

The mark: a cream film frame with three sprocket holes top and bottom, cut cleanly
by a diagonal blade of light graded sapphire -> mauve. Everything is drawn in a
1000 x 1000 design space and supersampled 4x, so the same source produces the
adaptive foreground, the legacy launcher icons and the monochrome layer.
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import numpy as np

OUT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SS = 2  # supersample factor (renderer runs in a 1 GB sandbox)
N = 1000  # design space

CREAM = (245, 241, 232, 255)
PLATE_TOP = (19, 26, 48)      # #131A30
PLATE_BOTTOM = (8, 12, 26)    # #080C1A
PLATE_FLAT = (11, 16, 32)     # #0B1020  (adaptive background colour)
BLUE = (137, 180, 250)        # #89B4FA sapphire
MAUVE = (203, 166, 247)       # #CBA6F7 mauve

# ---- geometry (design units) ----
FRAME_OUT = (238, 762)        # frame outer box
FRAME_STROKE = 52
FRAME_RX = 104
HOLE = 52
HOLE_CENTERS = (373, 500, 627)
HOLE_INSET = 62               # gap between frame inner edge and hole row
HOLES_TOP_Y = (238 + FRAME_STROKE + HOLE_INSET,
               238 + FRAME_STROKE + HOLE_INSET + HOLE)
HOLES_BOT_Y = (1000 - (238 + FRAME_STROKE + HOLE_INSET + HOLE),
               1000 - (238 + FRAME_STROKE + HOLE_INSET))
BLADE_W = 38
GAP_W = 60
BLADE_P0 = (272, 728)         # just past the frame's bottom-left corner
BLADE_P1 = (728, 272)         # ...and its top-right corner
BLADE_CAP = 22
PLATE_R = 230


def _px(v, size):
    return v * size * SS / N


def gradient(size, c0, c1, p0, p1):
    """Linear gradient along the p0->p1 direction over a size x size RGBA image."""
    s = size * SS
    dx, dy = float(p1[0] - p0[0]), float(p1[1] - p0[1])
    norm = dx * dx + dy * dy
    scale = (s / N) ** 2
    xs = (np.arange(s, dtype=np.float32) - float(_px(p0[0], size))) * np.float32(dx)
    ys = (np.arange(s, dtype=np.float32) - float(_px(p0[1], size))) * np.float32(dy)
    t = (ys[:, None] + xs[None, :]) / np.float32(norm * scale)
    np.clip(t, 0.0, 1.0, out=t)
    a = np.asarray(c0[:3], dtype=np.float32)
    b = np.asarray(c1[:3], dtype=np.float32)
    out = np.empty((s, s, 4), dtype=np.uint8)
    for ch in range(3):
        out[..., ch] = (a[ch] + (b[ch] - a[ch]) * t).astype(np.uint8)
    out[..., 3] = 255
    del xs, ys, t
    return Image.fromarray(out, "RGBA")


def blade_mask(size, width):
    """White band where the blade runs, black elsewhere."""
    s = size * SS
    m = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(m)
    p0 = (_px(BLADE_P0[0], size), _px(BLADE_P0[1], size))
    p1 = (_px(BLADE_P1[0], size), _px(BLADE_P1[1], size))
    w = int(_px(width, size))
    d.line([p0, p1], fill=255, width=w)
    # round the ends only on the outermost band so the cut stays crisp
    if width <= GAP_W:
        r = w / 2
        for (cx, cy) in (p0, p1):
            d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    return m


def mark_layer(size, mono=False, with_glow=True):
    """The mark on transparency: frame + holes, cut by the blade."""
    s = size * SS
    layer = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)

    ink = (255, 255, 255, 255) if mono else CREAM
    sw = _px(FRAME_STROKE, size)
    d.rounded_rectangle(
        [_px(FRAME_OUT[0], size) + sw / 2, _px(FRAME_OUT[0], size) + sw / 2,
         _px(FRAME_OUT[1], size) - sw / 2, _px(FRAME_OUT[1], size) - sw / 2],
        radius=_px(FRAME_RX, size), outline=ink, width=int(round(sw)),
    )
    for cy in (HOLES_TOP_Y, HOLES_BOT_Y):
        for cx in HOLE_CENTERS:
            d.rounded_rectangle(
                [_px(cx - HOLE / 2, size), _px(cy[0], size),
                 _px(cx + HOLE / 2, size), _px(cy[1], size)],
                radius=_px(8, size), fill=ink,
            )

    # The cut: erase a band so the frame and holes are cleanly severed.
    layer.paste(Image.new("RGBA", (s, s), (0, 0, 0, 0)), (0, 0), blade_mask(size, GAP_W))

    # Blade of light.
    grad = gradient(size, BLUE, MAUVE, (BLADE_P0[0], BLADE_P0[1]), (BLADE_P1[0], BLADE_P1[1]))
    if mono:
        grad = Image.new("RGBA", (s, s), (255, 255, 255, 255))
    band = blade_mask(size, BLADE_W)
    if with_glow and not mono:
        glow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        glow.paste(grad, (0, 0), blade_mask(size, BLADE_W * 2.6))
        glow = glow.filter(ImageFilter.GaussianBlur(_px(14, size)))
        ga = glow.split()[3].point(lambda v: int(v * 0.30))
        glow.putalpha(ga)
        layer = Image.alpha_composite(layer, glow)
    layer.paste(grad, (0, 0), band)
    return layer


def plate(size, rounded=True):
    """The midnight plate the mark sits on."""
    s = size * SS
    base = gradient(size, PLATE_TOP, PLATE_BOTTOM, (0, 0), (N, N))
    glow = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    cx, cy, r = _px(500, size), _px(420, size), _px(430, size)
    gd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(120, 140, 220, 60))
    glow = glow.filter(ImageFilter.GaussianBlur(_px(120, size)))
    base = Image.alpha_composite(base, glow)

    mask = Image.new("L", (s, s), 0)
    md = ImageDraw.Draw(mask)
    if rounded:
        md.rounded_rectangle([0, 0, s - 1, s - 1], radius=_px(PLATE_R, size), fill=255)
    else:
        md.ellipse([0, 0, s - 1, s - 1], fill=255)
    base.putalpha(mask)
    return base


def compose(size, plate_rounded=True, mark_scale=1.0, glow=True, mono=False):
    """Full icon: plate + mark."""
    s = size * SS
    canvas = plate(size, rounded=plate_rounded)
    if mark_scale == 1.0:
        canvas = Image.alpha_composite(canvas, mark_layer(size, mono=mono, with_glow=glow))
    else:
        inner = int(s * mark_scale)
        m = mark_layer(int(size * mark_scale), mono=mono, with_glow=glow)
        off = (s - inner) // 2
        canvas.alpha_composite(m, (off, off))
    return canvas.resize((size, size), Image.LANCZOS)


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "PNG", optimize=True)
    print(f"{path}  {img.size} {img.mode}", flush=True)


def main():
    # Legacy / store icons: plate is drawn, corners are transparent.
    launcher = {48: "mdpi", 72: "hdpi", 96: "xhdpi", 144: "xxhdpi", 192: "xxxhdpi"}
    for px, bucket in launcher.items():
        save(compose(px), f"{OUT}/app/src/main/res/mipmap-{bucket}/ic_launcher.png")
        save(compose(px, plate_rounded=False), f"{OUT}/app/src/main/res/mipmap-{bucket}/ic_launcher_round.png")

    # Adaptive foreground: transparent, mark inside the 66dp safe zone of 108dp.
    for px, bucket in {108: "mdpi", 162: "hdpi", 216: "xhdpi", 324: "xxhdpi", 432: "xxxhdpi"}.items():
        canvas = Image.new("RGBA", (px * SS, px * SS), (0, 0, 0, 0))
        inner = int(px * SS * 0.615)
        m = mark_layer(int(px * 0.615), with_glow=True)
        off = (px * SS - inner) // 2
        canvas.alpha_composite(m, (off, off))
        save(canvas.resize((px, px), Image.LANCZOS),
             f"{OUT}/app/src/main/res/mipmap-{bucket}/ic_launcher_foreground.png")

        # Monochrome: same mark, single ink, no glow.
        mcan = Image.new("RGBA", (px * SS, px * SS), (0, 0, 0, 0))
        mm = mark_layer(int(px * 0.615), mono=True, with_glow=False)
        mcan.alpha_composite(mm, (off, off))
        mono = mcan.resize((px, px), Image.LANCZOS).convert("LA")
        save(mono, f"{OUT}/app/src/main/res/mipmap-{bucket}/ic_launcher_monochrome.png")

    # Repository / docs icons.
    save(compose(1024), f"{OUT}/icon.png")
    save(compose(1024), f"{OUT}/docs/branding/clipcut-icon-1024.png")
    save(compose(512), f"{OUT}/fastlane/metadata/android/en-US/images/icon.png")

    # README banner + Play feature graphic.
    save(banner(1600, 520), f"{OUT}/docs/branding/clipcut-banner.png")
    save(feature_graphic(1024, 500), f"{OUT}/fastlane/metadata/android/en-US/images/featureGraphic.png")


SERIF_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
SANS = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def tracked_text(draw, xy, text, font, fill, tracking=0):
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=font, fill=fill)
        x += draw.textlength(ch, font=font) + tracking
    return x


def gradient_text(size, text, font, c0, c1):
    """Wordmark with a left-to-right cream -> mauve grade."""
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.text((0, 0), text, font=font, fill=(255, 255, 255, 255))
    grad = gradient(1, c1, c0, (0, 0), (1, 1)).resize(size, Image.BILINEAR)
    grad.putalpha(img.split()[3])
    return grad


def banner(w, h):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=36, fill=(10, 13, 24, 255))
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=36, outline=(60, 70, 100, 160), width=2)

    mark = compose(int(h * 0.62))
    img.alpha_composite(mark, (58, (h - mark.size[1]) // 2))

    x = 58 + mark.size[0] + 62
    word_font = ImageFont.truetype(SERIF_BOLD, 128)
    word = gradient_text((760, 160), "ClipCut", word_font, (245, 241, 232, 255), (203, 166, 247, 255))
    img.alpha_composite(word, (x, 118))
    tag_font = ImageFont.truetype(SANS, 27)
    tracked_text(d, (x + 6, 292), "PREMIUM MOBILE VIDEO EDITING", tag_font, (169, 176, 188, 255), tracking=9)
    d.line([x + 6, 340, x + 420, 340], fill=(137, 180, 250, 200), width=3)
    d.line([x + 444, 340, x + 560, 340], fill=(224, 214, 196, 90), width=3)
    return img.convert("RGB")


def feature_graphic(w, h):
    img = Image.new("RGB", (w, h), (10, 13, 24))
    d = ImageDraw.Draw(img)
    for i in range(h):  # vertical wash
        t = i / h
        d.line([0, i, w, i], fill=(int(12 + 8 * t), int(16 + 10 * t), int(30 + 16 * t)))
    mark = compose(int(h * 0.66))
    img.paste(mark, (70, (h - mark.size[1]) // 2), mark)
    x = 70 + mark.size[0] + 56
    word = gradient_text((640, 150), "ClipCut", ImageFont.truetype(SERIF_BOLD, 116),
                         (245, 241, 232, 255), (203, 166, 247, 255))
    img.paste(word, (x, 150), word)
    tracked_text(d, (x + 5, 312), "MULTI-TRACK EDITOR  ·  ON-DEVICE AI", ImageFont.truetype(SANS, 24),
                 (169, 176, 188), tracking=6)
    return img


if __name__ == "__main__":
    main()
