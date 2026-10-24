#!/usr/bin/env python3
"""Validate the built site's links, asset boundary, and image accessibility."""
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
        self.remote_assets = []

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
    assert {"main", "get-started"} <= document.ids
    expected = {"index.html", "styles.css", "favicon.svg", ".nojekyll", "assets/home.png", "assets/profiles.png"}
    assert {str(p.relative_to(ROOT)) for p in ROOT.rglob("*") if p.is_file()} == expected, "Unexpected public artifact file"
    assert "prefers-reduced-motion" in (ROOT / "styles.css").read_text()
    print("PASS: public allowlist, project-relative links, image labels and reduced-motion path")


if __name__ == "__main__":
    main()
