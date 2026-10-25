# Oree website design

This document governs the GitHub Pages website, not the Android app. Product behavior is defined in `docs/PRODUCT.md`; app design is defined in `docs/DESIGN.md`.

## Direction

A scenic introduction to a personal TV home: an immersive sky, centered typography, rounded actions and a prominent product image. Oree uses its own procedural cloud shader, orbit mark, screenshots, copy and CSS.

Use a near-black canvas, neutral surfaces, white headings, muted gray copy and generous space. Keep lower-page copy factual and concise; omit decorative eyebrow labels and slogan-heavy filler. A blue sky and soft cloud banks frame the introduction; later sections return to a solid background for reading. Slow cloud movement is explicitly requested for this website. Avoid parallax, invented device frames and decorative overlays on screenshots.

## Story

1. **Introduction:** Identify Android TV / Google TV, keep development-preview status visible and lead with “Your TV. A little more you.” Build instructions and source are the two actions.
2. **Home:** Show the real, complete 16:9 screenshot. Label sample apps and provide a full-size image link.
3. **Experience:** Explain organization, appearance and optional local photos in three compact columns.
4. **Profiles:** Pair the authentic profile screenshot with separate local layouts and appearance. Explain that streaming accounts remain in their apps.
5. **Remote:** A decorative CSS directional pad accompanies actual navigation behavior. It is hidden from assistive technology and is not an interactive control.
6. **Privacy:** Describe offline app launching, no advertising/telemetry/mandatory account, and optional provider artwork honestly.
7. **Questions:** Native HTML disclosures explain availability, streaming apps, permissions and the license. They must work without JavaScript.
8. **Build:** Close with real build/source links and repeat that no downloadable release is available yet.

Do not reintroduce the removed visual-quality comparison or imply that site imagery proves device performance. Avoid compatibility guarantees, download buttons without a release and invented measurements.

## Layout and style

- Content width: 1200px maximum, with 48px desktop and 20px mobile gutters.
- Opening: centered introduction, roughly 88px header, cloud sky fading into the canvas, dominant unobstructed screenshot.
- Main text: system sans-serif, 17px body, muted supporting copy. Headings use a medium weight and tight tracking.
- Tokens: canvas `#09090b`, text `#f4f7f5`, secondary `#a1a1aa`, accent `#e4e4e7`.
- Buttons: rounded pills, 48px minimum height, solid primary and subtly frosted secondary. The platform pill and header GitHub link share one blur/saturation layer with a thin rim. Hover changes only fill and border color; never displace the backdrop, shift geometry or move a custom cursor.
- Images: fixed intrinsic dimensions, 16:9 ratio, subtle border and 18px desktop / 10px mobile radius.
- Sections: approximately 104px vertical padding on desktop, 64px on mobile. Use left-aligned section introductions, compact neutral feature surfaces and a full-width profile screenshot.
- Below 768px: single-column sections, remote copy before the illustration, compact navigation. Keep Documentation and GitHub visible.
- Every image retains a full-size link. Do not crop the product UI.

## Engineering and accessibility

Use semantic HTML, CSS and a small optional WebGL enhancement without dependencies, external fonts, remote assets or analytics. Export only allowlisted files through `scripts/build-site.py`; preserve relative paths and the Content Security Policy. The sky is an original six-octave noise shader in `sky.js`, with no external assets. Render at most 1100 pixels wide, independent of device pixel ratio, and cap updates at 30 frames per second. Clouds gently lean toward a fine pointer with eased return. Pause animation when offscreen or the document is hidden. Reduced motion draws a still frame. Missing WebGL or context loss retains the CSS sky fallback.

The pills use CSS backdrop blur and a restrained inset highlight. SVG backdrop displacement was removed after visible rendering artifacts. Reduced transparency uses a solid fill.

The footer uses the project's real vector wordmark from `assets/branding/wordmark.svg`, with aligned base and outline layers. The lower part of the lettering meets a full-viewport hairline. A radial mask reveals the bright outline near the pointer, with a time-based eased follow that stops when settled. The entire footer is its pointer zone. Both footer dividers span the viewport while content keeps page gutters. Reduced motion keeps a static outline. Text, links and pointer geometry remain fixed.

Sections enter once with a short upward blur/fade; media surfaces rise and gently scale into place. Content already visible when the script starts is shown immediately. Without JavaScript everything is visible; reduced motion and keyboard focus reveal content immediately. Cards and screenshots brighten their borders on hover without resizing.

Keep native links and disclosures, a skip link, descriptive image text, visible focus rings and at least 44px practical targets. Decorative imagery has empty alternative text. Button feedback and cloud motion respect `prefers-reduced-motion`. Forced-colors users retain control borders.

## Verification

Run `python3 scripts/build-site.py` and `python3 scripts/check-site.py`. Review desktop and mobile rendering, keyboard navigation, expanded FAQ answers, full-size image links and 200% zoom. Inspect 320px, 390px, 768px, 1024px, 1440px and wide layouts when viewport controls are available. Record any verification limits accurately. Check all product statements against the app documentation. Public deployment requires explicit maintainer authorization.
