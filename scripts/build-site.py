#!/usr/bin/env python3
"""Build the Pages artifact from an explicit public-file allowlist. No dependencies."""
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / ".site-build"
ASSETS = {
    "site/sky.js": "sky.js",
    "site/interactions.js": "interactions.js",
    "docs/screenshots/home.png": "assets/home.png",
    "docs/screenshots/profiles.png": "assets/profiles.png",
    "site/styles.css": "styles.css",
    "site/favicon.svg": "favicon.svg",
}


def main():
    html = (ROOT / "site/index.html").read_text()
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
