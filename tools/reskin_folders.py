#!/usr/bin/env python3
"""Re-skin the Material Icon Theme folder icons with IntelliJ's folder silhouette.

Upstream draws a folder body (`<path id="folder">`, sometimes unlabelled) plus a
motive (`<path id="motive">`, `<g id="motive">`, or - for a few folders - an
unlabelled group). This tool keeps the artwork around the body, replaces the
body with the IntelliJ Platform folder shape, and re-fits the artwork into a
shared box so every folder type has the same anchor and the same extents:

    scale = min(BOX_WIDTH / width, BOX_HEIGHT / height), centred on (8.0, 9.0)

The box stays inside the 1 px outline of the IntelliJ shape (its inner face
spans x 2.0..14.0, y 5.0..13.0), so no glyph ever paints over the stroke.

Each folder gets a light and a dark file (`folder-src.svg`, `folder-src_dark.svg`).
The platform picks the dark one automatically. Only the outline colour differs:
a few motives deliberately draw in the body colour (VuePress' cut-out, Form's
cube) and a few are darker than the body (Bower), so recolouring the body for
dark themes would erase them.

Motives are measured in Chromium via `getBBox()`, which handles arcs, groups,
`<use>` and nested transforms. Measurements are cached in
`tools/folder-geometry.json`, so the result is reproducible without a browser.

Usage (normally called by tools/import-material-theme.sh):

    tools/reskin_folders.py --theme-dir src/main/resources/themes/material
    tools/reskin_folders.py --theme-dir ... --no-measure   # reuse cached geometry

Requires python3 and, for measuring, Chrome or Chromium.
"""
from __future__ import annotations

import argparse
import html as html_module
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile

# IntelliJ Platform expui/nodes/folder.svg, Apache-2.0, JetBrains s.r.o.
# See THIRD_PARTY_NOTICES.md. Only the geometry is reused; colours come from upstream.
BODY_PATH = (
    "M8.10584 4.34613L8.25344 4.5H8.46667H13C13.8284 4.5 14.5 5.17157 14.5 6V12.1333"
    "C14.5 12.9529 13.932 13.5 13.3667 13.5H2.63333C2.06804 13.5 1.5 12.9529 1.5 12.1333V3.86667"
    "C1.5 3.04707 2.06804 2.5 2.63333 2.5H6.1217C6.25792 2.5 6.38824 2.55557 6.48253 2.65387L8.10584 4.34613Z"
)
BODY_SIGNATURE = "M8.10584 4.34613"

BOX_WIDTH, BOX_HEIGHT = 9.6, 7.8
ANCHOR_X, ANCHOR_Y = 8.0, 9.0
LIGHT_STROKE = ((0, 0, 0), 0.36)
DARK_STROKE = ((255, 255, 255), 0.45)

FOLDER_PATH = re.compile(r'<path id="folder"[^>]*?/>')
DEFS = re.compile(r"<defs\b.*?</defs>", re.S)
ROOT = re.compile(r"<svg\b[^>]*>")
HEX_FILL = re.compile(r'fill="(#[0-9a-fA-F]{3,8})"')
MOTIVE_MARK = 'data-motive="1"'

MEASURE_JS = """
const rows = [];
function corners(b, m) {
  const pts = [[b.x, b.y], [b.x + b.width, b.y], [b.x, b.y + b.height],
               [b.x + b.width, b.y + b.height]];
  const out = pts.map(p => ({x: m.a * p[0] + m.c * p[1] + m.e,
                             y: m.b * p[0] + m.d * p[1] + m.f}));
  const xs = out.map(p => p.x), ys = out.map(p => p.y);
  return [Math.min(...xs), Math.min(...ys), Math.max(...xs), Math.max(...ys)];
}
for (const svg of document.querySelectorAll('svg[data-icon]')) {
  const m = svg.querySelector('[data-motive]');
  if (!m) continue;
  const rel = svg.getCTM().inverse().multiply(m.getCTM());
  const c = corners(m.getBBox(), rel);
  const x0 = Math.max(0, c[0]), y0 = Math.max(0, c[1]);
  const x1 = Math.min(16, c[2]), y1 = Math.min(16, c[3]);
  rows.push([svg.dataset.icon, x0, y0, x1 - x0, y1 - y0].join(' '));
}
document.getElementById('out').textContent = rows.join(String.fromCharCode(10));
"""


def hex_to_rgb(value: str) -> tuple[int, int, int]:
    value = value.lstrip("#")
    if len(value) == 3:
        value = "".join(ch * 2 for ch in value)
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))  # type: ignore[return-value]


def mix(color: str, target: tuple[int, int, int], amount: float) -> str:
    return "#%02x%02x%02x" % tuple(
        round(v + (t - v) * amount) for v, t in zip(hex_to_rgb(color), target)
    )


def split(svg: str) -> tuple[str, str, str | None]:
    """Returns (root tag, defs, motive). The motive is everything but defs and the folder body.

    Upstream is not consistent: most files label the motive, `folder-opencode` leaves it as a
    bare `<g>`, and the generic `folder.svg` is a lone unlabelled folder path. Treating the
    remainder as the motive covers all three without a per-file table.
    """
    root = ROOT.search(svg)
    end = svg.rindex("</svg>")
    if root is None or end < root.end():
        raise ValueError("unexpected SVG structure")

    inner = svg[root.end():end]
    defs = "\n".join(DEFS.findall(inner))
    rest = DEFS.sub("", inner)

    has_body = FOLDER_PATH.search(rest) is not None
    rest = FOLDER_PATH.sub("", rest)
    if not has_body:
        # generic folders: the unlabelled path is the upstream body, so there is no motive
        if not HEX_FILL.search(rest):
            raise ValueError("no hex fill")
        return root.group(0), defs, None

    motive = rest.strip()
    return root.group(0), defs, motive or None


def colour_of(svg: str) -> str:
    body = re.search(r'<path id="folder"[^>]*?fill="(#[0-9a-fA-F]{3,8})"', svg)
    match = body or HEX_FILL.search(svg)
    if match is None:
        raise ValueError("no hex fill")
    return match.group(1)


def compose(root: str, defs: str, motive: str | None, fill: str, dark: bool,
            transform: str = "", mark: bool = False) -> str:
    target, amount = DARK_STROKE if dark else LIGHT_STROKE
    stroke = mix(fill, target, amount)
    if "width=" not in root:
        root = root.replace("<svg", '<svg width="16" height="16"', 1)

    parts = [root]
    if defs:
        parts.append(defs)
    parts.append(f'<path d="{BODY_PATH}" fill="{fill}" stroke="{stroke}"/>')
    if motive:
        marker = f"{MOTIVE_MARK} " if mark else ""
        parts.append(f'<g {marker}transform="{transform}">{motive}</g>')
    parts.append("</svg>")
    return "\n".join(parts)


def fit(bbox: dict) -> str:
    width = max(bbox["w"], 0.01)
    height = max(bbox["h"], 0.01)
    scale = min(BOX_WIDTH / width, BOX_HEIGHT / height)
    tx = ANCHOR_X - (bbox["x"] + width / 2) * scale
    ty = ANCHOR_Y - (bbox["y"] + height / 2) * scale
    return f"translate({tx:.4f} {ty:.4f}) scale({scale:.6f})"


def used_folders(manifest: dict) -> dict[str, str]:
    """Icon id -> file basename, for every folder the plugin can actually select."""
    definitions = manifest.get("iconDefinitions", {})
    ids = set(manifest.get("folderNames", {}).values())
    if manifest.get("folder"):
        ids.add(manifest["folder"])

    names: dict[str, str] = {}
    for icon_id in sorted(ids):
        definition = definitions.get(icon_id)
        if definition is None:
            raise SystemExit(f"manifest references undefined folder icon '{icon_id}'")
        names[icon_id] = os.path.basename(definition["iconPath"]).removesuffix(".svg")
    return names


def find_chrome(explicit: str | None) -> str | None:
    if explicit:
        return shutil.which(explicit)
    for candidate in ("google-chrome", "chromium", "chromium-browser", "chrome"):
        found = shutil.which(candidate)
        if found:
            return found
    return None


def prefix_ids(svg: str, prefix: str) -> str:
    for attr in sorted(set(re.findall(r'\sid="([^"]+)"', svg)), key=len, reverse=True):
        svg = svg.replace(f'id="{attr}"', f'id="{prefix}{attr}"')
        svg = svg.replace(f'href="#{attr}"', f'href="#{prefix}{attr}"')
        svg = svg.replace(f"url(#{attr})", f"url(#{prefix}{attr})")
    return svg


def measure(icons_dir: str, names: dict[str, str], chrome: str) -> dict[str, dict]:
    """Measures every motive in one headless page, with ids prefixed to avoid collisions."""
    parts = []
    for index, (icon_id, basename) in enumerate(sorted(names.items())):
        svg = open(os.path.join(icons_dir, basename + ".svg"), encoding="utf-8").read()
        root, defs, motive = split(svg)
        if motive is None:
            continue
        # identity transform: getBBox() then returns the motive box in root coordinates
        prepared = compose(root, defs, motive, colour_of(svg), dark=False,
                           transform="translate(0 0)", mark=True)
        prepared = re.sub(r"^<svg\b", f'<svg data-icon="{icon_id}"', prepared, count=1)
        parts.append(f'<div class="box">{prefix_ids(prepared, f"p{index}-")}</div>')

    page = (
        '<!doctype html><meta charset="utf-8"><style>'
        ".box{position:absolute;left:-9999px;top:0;width:16px;height:16px}"
        f'</style><div>{"".join(parts)}</div><pre id="out"></pre><script>{MEASURE_JS}</script>'
    )

    with tempfile.TemporaryDirectory(prefix="reskin-") as work:
        html_path = os.path.join(work, "measure.html")
        dump_path = os.path.join(work, "dump.html")
        open(html_path, "w", encoding="utf-8").write(page)
        with open(dump_path, "w", encoding="utf-8") as dump:
            subprocess.run(
                [chrome, "--headless=new", "--no-sandbox", "--disable-gpu", "--hide-scrollbars",
                 "--window-size=400,300", "--dump-dom", f"file://{html_path}"],
                stdout=dump, stderr=subprocess.DEVNULL, check=True, timeout=180,
            )
        dom = open(dump_path, encoding="utf-8").read()

    pre = re.search(r'<pre id="out">(.*?)</pre>', dom, re.S)
    if pre is None:
        raise SystemExit("Chromium did not run the measuring script")

    boxes: dict[str, dict] = {}
    for line in html_module.unescape(pre.group(1)).strip().splitlines():
        bits = line.split()
        if len(bits) != 5:
            continue
        x, y, w, h = (float(v) for v in bits[1:])
        if w > 0 and h > 0:
            boxes[bits[0]] = {"x": round(x, 4), "y": round(y, 4), "w": round(w, 4), "h": round(h, 4)}
    return boxes


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--theme-dir", required=True,
                        help="directory containing material-icons.json and icons/")
    parser.add_argument("--geometry", default="tools/folder-geometry.json",
                        help="cache for measured motive boxes (default: %(default)s)")
    parser.add_argument("--no-measure", action="store_true",
                        help="reuse the cached geometry instead of asking Chromium")
    parser.add_argument("--chrome", default=None, help="Chrome/Chromium executable")
    args = parser.parse_args()

    manifest = json.load(open(os.path.join(args.theme_dir, "material-icons.json"), encoding="utf-8"))
    icons_dir = os.path.join(args.theme_dir, "icons")
    names = used_folders(manifest)

    sources: dict[str, dict] = {}
    for icon_id, basename in sorted(names.items()):
        path = os.path.join(icons_dir, basename + ".svg")
        svg = open(path, encoding="utf-8").read()
        if BODY_SIGNATURE in svg:
            raise SystemExit(f"{path} is already re-skinned")
        root, defs, motive = split(svg)
        sources[icon_id] = {"path": path, "basename": basename, "root": root, "defs": defs,
                            "motive": motive, "fill": colour_of(svg)}
    needing_geometry = {icon_id for icon_id, source in sources.items() if source["motive"]}

    geometry: dict[str, dict] = {}
    if os.path.exists(args.geometry):
        geometry = json.load(open(args.geometry, encoding="utf-8"))

    if not args.no_measure:
        chrome = find_chrome(args.chrome)
        if chrome is None:
            print("No Chrome/Chromium found; using cached geometry")
        else:
            print(f"Measuring {len(needing_geometry)} folder motives with {chrome}...")
            geometry = measure(icons_dir, names, chrome)
            missing = needing_geometry - set(geometry)
            if missing:
                raise SystemExit(f"could not measure {len(missing)} motives: {sorted(missing)[:5]}")
            with open(args.geometry, "w", encoding="utf-8") as out:
                json.dump(geometry, out, indent=1, sort_keys=True)
                out.write("\n")
            print(f"Wrote {args.geometry}")

    missing = sorted(needing_geometry - set(geometry))
    if missing:
        raise SystemExit(
            f"no geometry for {len(missing)} folder icons ({missing[:5]}); "
            "run without --no-measure to measure them"
        )

    written = 0
    body_only = []
    for icon_id, source in sources.items():
        path = source["path"]
        motive = source["motive"]
        transform = fit(geometry[icon_id]) if motive else ""
        light = compose(source["root"], source["defs"], motive, source["fill"],
                        dark=False, transform=transform)
        dark = compose(source["root"], source["defs"], motive, source["fill"],
                       dark=True, transform=transform)

        open(path, "w", encoding="utf-8").write(light + "\n")
        open(os.path.join(icons_dir, source["basename"] + "_dark.svg"), "w",
             encoding="utf-8").write(dark + "\n")
        written += 1
        if motive is None:
            body_only.append(icon_id)

    print(f"Re-skinned {written} folders ({written} light + {written} dark files)")
    if body_only:
        print(f"Body only, no artwork: {', '.join(sorted(body_only))}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
