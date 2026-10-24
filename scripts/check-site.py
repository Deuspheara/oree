#!/usr/bin/env python3
"""Validate the built site's links, asset boundary, and required accessible controls."""
import json
import re
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1] / ".site-build"


class Document(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = set()
        self.references = []
        self.pickers = []
        self.remote_assets = []
        self.preset_json = ""
        self.reading_presets = False

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if "id" in attrs:
            assert attrs["id"] not in self.ids, f'Duplicate ID: {attrs["id"]}'
            self.ids.add(attrs["id"])
        for key in ("href", "src"):
            if attrs.get(key):
                self.references.append(attrs[key])
        if tag in ("script", "img", "link"):
            self.remote_assets.extend(v for k, v in attrs.items() if k in ("src", "href") and v.startswith(("http", "//")))
        if tag == "img":
            assert "alt" in attrs and "width" in attrs and "height" in attrs, "Images need alt and explicit dimensions"
        if tag == "input" and "data-level" in attrs:
            assert attrs.get("type") == "radio" and attrs.get("name") == "preset" and attrs.get("aria-label"), "Preset needs a named radio"
            self.pickers.append(attrs)
        if tag == "script" and attrs.get("id") == "preset-data":
            self.reading_presets = True

    def handle_endtag(self, tag):
        if tag == "script":
            self.reading_presets = False

    def handle_data(self, data):
        if self.reading_presets:
            self.preset_json += data


def main():
    document = Document()
    document.feed((ROOT / "index.html").read_text())
    assert not document.remote_assets, "No external fonts, images or scripts belong in this site"
    for target in document.references:
        url = urlsplit(target)
        if url.scheme or url.netloc:
            assert url.scheme == "https" and url.netloc == "github.com", f"Unexpected external link: {target}"
            continue
        assert not url.path.startswith("/"), f"Use project-Pages-safe relative assets: {target}"
        if url.path:
            resolved = (ROOT / url.path).resolve()
            assert ROOT.resolve() in resolved.parents and resolved.is_file(), f"Missing/unsafe asset: {target}"
        elif url.fragment:
            assert url.fragment in document.ids, f"Missing anchor: {target}"
    for asset in re.findall(r'url\(["\']?([^\)"\']+)', (ROOT / "styles.css").read_text()):
        assert not urlsplit(asset).scheme and (ROOT / asset).is_file(), f"Missing CSS asset: {asset}"
    presets = json.loads(document.preset_json)
    assert len(presets) == 6 and len(document.pickers) == 6
    assert [p["data-level"] for p in document.pickers] == list(map(str, range(6)))
    assert sum("checked" in p for p in document.pickers) == 1
    assert "checked" in document.pickers[2], "Balanced must be the initial illustration"
    for picker, preset in zip(document.pickers, presets):
        assert preset["label"] in picker["aria-label"], "Preset radio and source label differ"
    assert {"main", "quality", "get-started", "preset-title", "preset-copy"} <= document.ids
    expected = {"index.html", "styles.css", "app.js", "favicon.svg", ".nojekyll", "assets/home.png", "assets/profiles.png", "assets/landscape.jpg", "assets/avatar-01.png", "assets/avatar-03.png"}
    assert {str(p.relative_to(ROOT)) for p in ROOT.rglob("*") if p.is_file()} == expected, "Unexpected public artifact file"
    assert "prefers-reduced-motion" in (ROOT / "styles.css").read_text()
    print("PASS: public allowlist, project-relative links, six source-backed presets, image labels and reduced-motion path")


if __name__ == "__main__":
    main()
