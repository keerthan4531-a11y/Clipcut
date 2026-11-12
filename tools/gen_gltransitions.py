#!/usr/bin/env python3
"""Convert gl-transitions (MIT) from GLSL ES 1.00 to ClipCut's GLSL ES 3.00
two-texture transition form, and emit a catalogue.

Source: https://github.com/gl-transitions/gl-transitions (MIT, see LICENSE).
Each .glsl carries its own Author/License header; those are preserved in the
catalogue so per-transition attribution survives into the app.
"""
import json, os, re, sys

SRC = "/tmp/glt/gl-transitions-master/transitions"
OUT_ASSETS = "/home/user/clipcut2/app/src/main/assets/gltransitions"
OUT_CATALOG = os.path.join(OUT_ASSETS, "catalog.json")

UNIFORM_RE = re.compile(
    r"^\s*uniform\s+(float|vec2|vec3|vec4|int|bool)\s+([A-Za-z_][A-Za-z0-9_]*)\s*;\s*(//.*)?$")


def parse_default(comment):
    """`// = vec2(1.0, 0.0)` / `// = 0.4` / `// @range(0, 1)` -> (default, min, max)."""
    default, lo, hi = None, None, None
    if comment:
        m = re.search(r"//\s*=\s*(.+?)\s*$", comment)
        if m:
            default = m.group(1).strip()
        m = re.search(r"@range\(\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*\)", comment)
        if m:
            lo, hi = float(m.group(1)), float(m.group(2))
    return default, lo, hi


def convert(path):
    src = open(path, encoding="utf-8").read()
    name = os.path.splitext(os.path.basename(path))[0]
    meta = {"id": name, "name": re.sub(r"(?<!^)(?=[A-Z])", " ", name).strip(),
            "author": "", "license": "MIT", "description": "", "tags": []}
    for line in src.splitlines():
        m = re.match(r"^//\s*(Author|License|Description|Tags)\s*:\s*(.*)$", line)
        if not m:
            continue
        key, val = m.group(1).lower(), m.group(2).strip()
        if key == "tags":
            meta["tags"] = [t.strip().lower() for t in val.split(",") if t.strip()]
        elif key in ("author", "license", "description"):
            meta[key] = val

    params, body_lines = [], []
    for line in src.splitlines():
        m = UNIFORM_RE.match(line)
        if m:
            ptype, pname, comment = m.group(1), m.group(2), m.group(3)
            if pname in ("progress",):
                continue
            default, lo, hi = parse_default(comment)
            params.append({"name": pname, "type": ptype, "default": default, "min": lo, "max": hi})
            continue
        body_lines.append(line)
    body = "\n".join(body_lines)

    # strip // comments for construct detection (several transitions mention
    # texture2D/from/to only inside commented-out example code)
    code_only = "\n".join(re.sub(r"//.*$", "", line) for line in body.splitlines())
    problems = []
    if "vec4 transition" not in code_only:
        problems.append("no transition() entry point")
    for bad in ("uniform sampler2D from", "uniform sampler2D to", "texture2D(", "gl_FragColor", "uniform sampler2D"):
        if bad in code_only:
            problems.append(f"uses {bad.strip()}")
    if "#version" in body:
        problems.append("carries its own #version")

    # GLSL ES 1.00 -> 3.00 spellings inside the body
    body = body.replace("texture2D(", "texture(")
    body = body.replace("varying ", "in ")

    uniforms = "\n".join(f"uniform {p['type']} {p['name']};" for p in params)
    shader = f"""#version 300 es
// {name} — from gl-transitions (MIT), author: {meta['author'] or 'unknown'}
// Converted to GLSL ES 3.00 for ClipCut's two-texture transition program.
precision highp float;
precision highp sampler2D;
uniform sampler2D uFromTexSampler;
uniform sampler2D uToTexSampler;
uniform float progress;
uniform float ratio;      // viewport aspect ratio, provided by the host program
{uniforms}
in vec2 vTexCoord;
out vec4 fragColor;
vec4 getFromColor(vec2 uv) {{ return texture(uFromTexSampler, uv); }}
vec4 getToColor(vec2 uv) {{ return texture(uToTexSampler, uv); }}
{body}
void main() {{ fragColor = transition(vTexCoord); }}
"""
    return name, meta, params, shader, problems


def main():
    os.makedirs(OUT_ASSETS, exist_ok=True)
    entries, failed = [], []
    for fn in sorted(os.listdir(SRC)):
        if not fn.endswith(".glsl"):
            continue
        name, meta, params, shader, problems = convert(os.path.join(SRC, fn))
        if problems:
            failed.append((name, problems))
            continue
        with open(os.path.join(OUT_ASSETS, name + ".glsl"), "w", encoding="utf-8") as fh:
            fh.write(shader)
        meta["params"] = params
        entries.append(meta)

    catalog = {
        "schemaVersion": 1,
        "source": "https://github.com/gl-transitions/gl-transitions",
        "license": "MIT",
        "copyright": "Copyright (c) 2017-present gl-transitions contributors",
        "transitions": entries,
    }
    with open(OUT_CATALOG, "w", encoding="utf-8") as fh:
        json.dump(catalog, fh, indent=1, ensure_ascii=False)
    print(f"converted {len(entries)} transitions, {len(failed)} skipped")
    for n, p in failed:
        print("  SKIP", n, "|", "; ".join(p))
    sizes = sum(os.path.getsize(os.path.join(OUT_ASSETS, f)) for f in os.listdir(OUT_ASSETS))
    print(f"assets: {len(os.listdir(OUT_ASSETS))} files, {sizes/1024:.0f} KB")


if __name__ == "__main__":
    main()
