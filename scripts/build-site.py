#!/usr/bin/env python3
"""Build the Pages artifact from an explicit public-file allowlist. No dependencies."""
import json
import re
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / ".site-build"
ASSETS = {
    "docs/screenshots/home.png": "assets/home.png",
    "docs/screenshots/profiles.png": "assets/profiles.png",
    "core/platform/src/main/res/drawable-nodpi/landscape_forest.jpg": "assets/landscape.jpg",
    "assets/avatars/masters/avatar_01.png": "assets/avatar-01.png",
    "assets/avatars/masters/avatar_03.png": "assets/avatar-03.png",
    "site/styles.css": "styles.css",
    "site/app.js": "app.js",
    "site/favicon.svg": "favicon.svg",
}


def main():
    presets = json.loads((ROOT / "site/presets.json").read_text())
    source = (ROOT / "core/model/src/main/java/fr/deuspheara/oree/core/model/VisualExperience.kt").read_text()
    expected = [
        (name, [float(value.strip().removesuffix("f")) for value in values.split(",")])
        for name, values in re.findall(r"^    ([A-Z_]+)\(([^)]+)\),$", source, re.M)
    ]
    actual = [(p["id"], [p["motion"], p["glass"], p["blur"]]) for p in presets]
    if actual != expected or len(presets) != 6:
        raise SystemExit("Site presets must match VisualExperience.kt, in slider order")
    resources = ET.parse(ROOT / "core/designsystem/src/main/res/values/strings.xml").getroot()
    labels = {item.get("name"): item.text for item in resources.findall("string")}
    for preset in presets:
        if preset["label"] != labels.get("experience_" + preset["id"].lower()):
            raise SystemExit("Site preset labels must match the English app strings")
    html = (ROOT / "site/index.html").read_text()
    if html.count("<!--PRESET_DATA-->") != 1:
        raise SystemExit("Expected exactly one preset data slot")
    data = json.dumps(presets, ensure_ascii=False).replace("</", "<\\/")
    html = html.replace("<!--PRESET_DATA-->", f'<script type="application/json" id="preset-data">{data}</script>')
    for source_path in ASSETS:
        if not (ROOT / source_path).is_file() or (ROOT / source_path).is_symlink():
            raise SystemExit(f"Missing or symbolic source: {source_path}")
    if OUTPUT.is_symlink():
        raise SystemExit("Refusing a symbolic build directory")
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    OUTPUT.mkdir()
    for source_path, target in ASSETS.items():
        destination = OUTPUT / target
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ROOT / source_path, destination)
    (OUTPUT / "index.html").write_text(html)
    (OUTPUT / ".nojekyll").write_text("")
    print(f"Built {len(ASSETS) + 2} allowlisted public files in .site-build/")


if __name__ == "__main__":
    main()
