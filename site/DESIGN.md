# Oree — Website Design System

> **Scope:** This document defines the visual language, composition, content, and interaction principles for the Oree website. It does not govern the Android application, whose design is maintained separately in `docs/DESIGN.md`.
>
> **Status:** Applied to the website revision. Preserve working functionality and existing assets unless a change is justified.

## 1. Creative direction

### The product is the experience

Oree is a local-first Android TV launcher. Its website should reflect the same principles as the product: calm, precise, personal, and thoughtfully engineered.

**Creative north star: A quiet gallery for a living interface.**

Imagine an architectural exhibition where the object itself carries the visual interest. The surroundings are deliberately subdued, the typography is confident, and every detail has a purpose.

Oree's screenshots provide the color, depth, and personality. The website provides composition, typography, and context.

Three defining qualities:

- **Calm:** Deliberate whitespace, subdued surfaces, and minimal visual noise.
- **Tactile:** Precise borders, intentional interactions, and a subtle sense of physicality.
- **Authentic:** Real screenshots, concrete explanations, and transparent development status.

The website should feel premium without pretending to be a luxury brand, and technical without resembling developer documentation.

### Design priorities

1. **Product clarity:** A visitor understands what Oree is within seconds.
2. **Visual confidence:** The product occupies more attention than the marketing.
3. **Distinctive composition:** A carefully designed editorial layout, not a generic landing-page template.
4. **Credibility:** Every visible product claim is supported by actual implementation.
5. **Quality of execution:** Responsive behavior, accessibility, typography, and small details receive the same attention as the hero.

### What makes Oree recognizable

Its identity should emerge from a consistent combination of:

- Deep, cool graphite surfaces.
- Large, precisely framed screenshots.
- Restrained architectural spacing.
- Soft sage accents reserved for interactive meaning.
- Clean typography and minimal interface chrome.
- Subtle material cues inspired by the app's glass rendering, without replicating it throughout the website.

Avoid imitating Apple's typography, branding, hardware presentations, or exact layouts. Borrow the principles of confident product photography and disciplined information hierarchy, not its identity.

---

## 2. Content and narrative

The website should tell a short, coherent story rather than enumerate every feature.

### Navigation

A minimal header containing:

- Oree wordmark on the left.
- A source-code link and, when useful, a build/documentation link on the right.

Do not introduce unnecessary navigation or a mobile hamburger menu when the links fit naturally.

The header must never dominate the product presentation.

### Section 01 — Introduction

**Headline:**

A quieter home for Android TV.

**Supporting text:**

Oree brings your apps, favorites, and household profiles together in a clean, local-first TV launcher.

**Primary action:** Build & try Oree

**Status:** Development preview

Keep the status visible beside or immediately below the primary action. The CTA must point to actual build instructions. Do not present a development build as a stable consumer release.

The headline should be memorable, but the supporting copy must clearly explain the product.

**Composition:**

- Left-aligned introduction with a compact text column.
- Generous negative space to the right.
- Primary action immediately after the description.
- A large, authentic Home screenshot directly below.
- The beginning of the screenshot should be visible in the initial desktop viewport at approximately 1440 × 900.

The introduction and screenshot form one composition, not two unrelated sections.

### Section 02 — Home

Let the Home screenshot establish the product's visual identity.

Show it in its complete 16:9 composition, unobstructed by browser chrome, floating cards, marketing labels, or decorative overlays.

A small caption may read:

*Oree Home — development preview*

Use the actual screenshot as the dominant visual element. Do not construct a fictional TV frame, stand, or device.

### Section 03 — Personal spaces

**Suggested heading:** A home for everyone.

Describe local household profiles, separate favorites, and personalization using the real behavior implemented in Oree.

Use an asymmetric editorial composition:

- Text occupying approximately 40% of the layout.
- A real profile screenshot occupying approximately 60%.
- Visual alignment should follow the important content within the screenshot rather than blindly centering everything.

Avoid unnecessary text. One clear paragraph and an authentic image are preferable to multiple feature cards.

### Section 04 — Glass and visual quality

**Suggested heading:** Detail without distraction.

Introduce Oree's glass appearance and its adjustable rendering presets.

This section is the website's primary interactive demonstration. It should feel more considered than a conventional settings table.

Pair the preview with the existing six preset controls. Explain differences in appearance and relative rendering workload without inventing performance measurements.

See the dedicated glass specification below.

### Section 05 — Everyday navigation

Explain the experience of using Oree rather than advertising abstract features.

Describe only implemented interactions, such as navigation with a remote, favorites, app management, and switching profiles.

Use a compact editorial list with subtle separators, not three identical feature cards.

Each entry should contain a short heading and one meaningful sentence.

### Section 06 — Build Oree

End with a concise invitation to inspect, build, or contribute to the project.

The content should communicate that Oree is an actual software project, not a concept rendering.

Link to real source code and build documentation. Keep detailed installation requirements, device compatibility, and developer workflows in the documentation.

---

## 3. Layout and composition

### Grid

| Property | Desktop | Mobile |
|---|---|---|
| Maximum content width | 1280px | Fluid |
| Outer gutter | 48px | 20px |
| Main columns | 12-column reference grid | Single column |
| Section separation | 96–128px | 56–80px |
| Content-to-image gap | 48–80px | 24–32px |
| Header height | Approximately 72px | Approximately 64px |
| Layout collapse | Below 768px | — |

Use an underlying 8px spacing rhythm, with 4px increments for fine adjustments.

Use a consistent alignment system: navigation, headings, body text, dividers, and image edges should share deliberate vertical guides.

### Composition rules

**Asymmetry creates interest; alignment creates order.**

- Avoid perfectly centered text blocks throughout the site.
- Vary the scale and layout of consecutive sections.
- Pair large media with compact explanatory text.
- Use open space as an intentional element, not as a substitute for composition.
- Avoid repeating the same image-left/text-right arrangement indefinitely.
- Let one image dominate each major section instead of competing with multiple equally weighted visuals.

The page should have an identifiable visual rhythm:

**Introduction → Hero image → Intimate feature → Interactive demonstration → Compact details → Closing invitation.**

### Responsive behavior

At widths below 768px:

- Collapse editorial layouts into a single column.
- Preserve the reading order: heading, explanation, associated image.
- Maintain authentic screenshot proportions.
- Reduce spacing proportionally without compressing typography excessively.
- Allow preset controls to reflow into two-column or stacked groups.
- Never rely on horizontal page scrolling.

On narrow screens, a full 16:9 screenshot will naturally appear small. Provide an accessible way to view it at its original detail rather than distorting it or replacing it with a fabricated mobile composition.

Test at 320px, 390px, 768px, 1024px, and 1440px, including wider desktop screens.

---

## 4. Color system

Oree uses a cool, nearly neutral palette. The colors of the application screenshots create visual richness.

| Token | Value | Role |
|---|---|---|
| Canvas | `#101214` | Main page background |
| Surface | `#191C1F` | Raised and interactive areas |
| Text primary | `#F5F6F7` | Headings, primary content |
| Text secondary | `#B7BEC4` | Supporting descriptions |
| Border | `#353A40` | Dividers and media boundaries |
| Accent | `#BBD9C5` | Selected states and focus |
| Primary action | `#F5F6F7` | Primary button background |
| Primary action text | `#101214` | Primary button text |

### Application

- Use the canvas as the dominant background.
- Use raised surfaces only when they communicate grouping or interaction.
- Avoid placing every section inside a card.
- Use the accent sparingly. It should mean something, not merely decorate.
- Prefer borders, spacing, and contrast to heavy shadows.
- Avoid introducing unrelated accent colors.

Glass treatments belong principally inside the application imagery and dedicated material demonstration. The website's text, navigation, and structural containers remain opaque.

Contrast must be checked for actual text/background combinations, including hover, selected, disabled, and focus states.

---

## 5. Typography

Use a native system sans-serif stack:

`-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`

This is a deliberate product decision: fast loading, excellent readability, and a familiar appearance across platforms.

Distinctiveness comes from typography composition, not decorative font selection.

### Type scale

| Role | Size | Weight | Line height |
|---|---|---|---|
| Hero | `clamp(40px, 5vw, 72px)` | 600 | 1.06 |
| Section heading | `clamp(30px, 3.5vw, 48px)` | 600 | 1.12 |
| Feature heading | 22–28px | 550–600 | 1.25 |
| Body | 18px desktop / 16px mobile | 400 | 1.55 |
| Supporting text | 16px | 400 | 1.5 |
| Caption | 14px | 400–500 | 1.45 |

### Rules

- Keep long-form lines within approximately 60–65 characters.
- Tight tracking is acceptable for large headings, no tighter than `-0.025em`.
- Avoid negative tracking on body text.
- Prefer sentence case over repeated uppercase labels.
- Do not use multiple font families merely to manufacture hierarchy.
- Avoid exaggerated headlines that force the product below the first viewport.

Keep headings compact and deliberate. Line breaks should feel intentional rather than produced by arbitrary fixed widths.

---

## 6. Imagery and product presentation

### Authenticity

Every image representing the Oree application must come from an actual build or be explicitly identified as illustrative.

Do not recreate the application interface using website components and present it as a screenshot.

### Screenshot treatment

- Preserve the original 16:9 ratio for complete TV screenshots.
- Use 12px rounded corners for primary screenshots.
- Apply a fine, subtle border.
- Avoid large drop shadows, glows, perspective distortions, and device mockups.
- Do not modify UI details, blur out inconvenient sections, or invent content.
- Ensure images remain visually distinct from the surrounding canvas.

Small, tightly cropped details may be used in secondary sections, provided they come from actual captures and are not misleading.

### Media behavior

- Provide suitable responsive image sizes and efficient formats.
- Prioritize the hero image.
- Lazy-load media below the initial viewport.
- Preserve dimensions to prevent layout shifts.
- Provide descriptive alternative text.
- Offer an accessible full-size viewing experience where interface details would otherwise be unreadable.

Use real screenshots as the primary visual proof. Do not add illustrative assets simply to fill space.

---

## 7. Glass preset experience

The six existing, source-backed rendering presets are a distinctive part of the website.

Retain all six, preserving their actual names, order, and behavior as defined in the implementation.

### Design

Treat the selector as a compact material comparison interface.

Each preset must communicate:

1. Its name.
2. Its visual characteristics.
3. Its relative rendering workload, when supported by implementation evidence.
4. Whether the displayed media is a real capture or an illustration.

### Layout

On desktop, place a large preview alongside a compact selector and explanatory content.

On mobile, stack the preview above accessible, appropriately sized preset controls.

The selected preset should be immediately distinguishable through shape, border, and contrast—not through color alone.

### Visual integrity

Prefer actual captures for appearance comparisons.

If a real capture is unavailable, a CSS approximation is permitted only when prominently identified as illustrative. It must not imply pixel-accurate reproduction of the Android renderer.

The website must not suggest that changing a CSS preview proves changes in television rendering speed.

### Performance language

Live Liquid Glass is the highest-workload preset and retains its live visual behavior during sustained arrow-key navigation in the application.

Describe this as application behavior, not as an animation being performed by the website.

Avoid unsupported claims about frame rate, latency, GPU usage, battery consumption, or device compatibility.

Do not fabricate relative performance percentages or assert that an appearance preset guarantees smoothness on all hardware.

### Interaction

- Support pointer, touch, and keyboard selection.
- Use appropriate semantic controls, preferably a radio group when choosing exactly one preset.
- Preserve visible selection and focus independently.
- Support expected keyboard interactions without capturing arrow keys globally.
- Avoid unnecessary transitions between nearly identical visual states.
- Ensure the explanation updates with the selected preset.

The demonstration should increase understanding rather than become a visual gimmick.

---

## 8. Component language

### Buttons

Primary buttons use a light background, dark text, and restrained geometry.

- Height: at least 48px.
- Horizontal padding: approximately 20–24px.
- Radius: 10px.
- Font weight: 600.
- No glowing shadow or gradient fill.

Hover and active states should feel tactile but understated.

Secondary navigation should generally use text links rather than competing outlined buttons.

### Links

Links must be recognizable and have meaningful focus and hover states.

Text links should not depend on color alone to indicate interaction.

### Dividers

Use 1px structural lines to separate related content.

Avoid using full borders around every item when a single divider provides sufficient organization.

### Image frames

Use a consistent media treatment throughout the site. Primary images should be visually stronger than supplementary details.

### Preset controls

Use a shared radius, consistent padding, and stable dimensions. The selected state should feel precise and integrated into the visual system.

### Status indicators

Development status should be communicated with text, not an unexplained colored dot or decorative badge.

---

## 9. Motion and interaction principles

Motion should improve understanding and feedback.

**The application demonstrates movement. The website remains composed.**

### Timing

- Hover and focus transitions: 120–180ms.
- Selection changes: 160–240ms.
- No obligatory entrance animations for page content.

Use smooth, restrained easing. Avoid bouncing, elastic movement, perpetual floating elements, and decorative parallax.

### Behavior

- Interactions begin with user input.
- Content remains available without animation.
- Respect `prefers-reduced-motion`.
- Disable or simplify nonessential animated material previews when reduced motion is requested.
- Avoid scroll-linked effects that interfere with reading.
- Never animate the layout in ways that cause content shifts.

The interface should feel responsive because it reacts immediately, not because everything moves.

---

## 10. Accessibility

Target WCAG 2.2 AA as the accessibility baseline, with additional usability requirements where appropriate.

- Provide semantic heading hierarchy and landmarks.
- Preserve native keyboard navigation.
- Use a clearly visible focus indicator, preferably a 2px accent outline with sufficient separation from the component.
- Design interactive targets to be at least 44 × 44 CSS pixels where practical.
- Maintain readable text contrast.
- Ensure controls expose their actual labels and selected states.
- Avoid placing important text directly over complex screenshots.
- Do not use animation, color, or hover as the only way to communicate information.
- Ensure full-size image viewing is keyboard-accessible and does not trap focus.

Test with keyboard-only navigation and at 200% browser zoom.

Accessibility should be part of component design, not a later cosmetic adjustment.

---

## 11. Content and language

### Voice

Oree communicates with simplicity and technical confidence.

Prefer:

- Direct statements.
- Short, descriptive headings.
- Concrete features.
- Transparent development status.
- Everyday language over rendering terminology in primary marketing copy.

Technical explanations are appropriate when they clarify actual behavior.

### Avoid

- Generic phrases such as "next-generation experience," "reimagine entertainment," or "unleash the future."
- Repeating the same slogan in multiple sections.
- Invented statistics and performance comparisons.
- Unverified installation or device-support claims.
- Testimonials without real attribution.
- Unsupported descriptions of features under development.
- Presenting application screenshots as if they are universally representative of every device.

All primary copy should remain understandable to someone unfamiliar with Android development.

---

## 12. Engineering principles

Visual quality must not come at the cost of maintainability or loading performance.

- Work within the existing website stack.
- Prefer semantic HTML and straightforward CSS.
- Avoid introducing heavy animation or UI libraries for simple interactions.
- Keep JavaScript focused on behavior that actually requires it.
- Reuse design tokens rather than scattering one-off values.
- Build responsive layouts with CSS Grid and Flexbox where appropriate.
- Minimize unnecessary client-side rendering and layout work.
- Maintain stable media dimensions.
- Avoid decorative effects that require frequent repaints.
- Use progressive enhancement: primary content must remain accessible when nonessential JavaScript fails.

Do not rewrite working architecture simply to achieve a visual effect.

The site should be lightweight enough that the application imagery, rather than the website framework, determines most of its visual loading cost.

---

## 13. Quality and acceptance

Before declaring the design revision complete:

### Visual review

- [ ] Oree has a recognizable visual identity independent of Apple or generic developer websites.
- [ ] The opening viewport clearly identifies the product and reveals actual application imagery.
- [ ] Typography, alignment, whitespace, and framing are consistent.
- [ ] The page has a deliberate visual rhythm rather than repetitive feature templates.
- [ ] Real screenshots remain legible and unobstructed.
- [ ] Mobile layouts are composed intentionally rather than simply compressed desktop layouts.

### Functional review

- [ ] The build and source links resolve to real destinations.
- [ ] All six glass presets work with pointer, touch, and keyboard input.
- [ ] Preset descriptions match the current source implementation.
- [ ] Responsive layouts have no unintended horizontal overflow.
- [ ] Screenshots can be inspected at sufficient detail.
- [ ] Reduced-motion behavior is correct.
- [ ] There are no relevant console errors or broken assets.

### Accuracy review

- [ ] Every product claim reflects implemented behavior.
- [ ] All illustrative visuals are clearly identified.
- [ ] Performance descriptions distinguish known facts from approximations.
- [ ] Development preview status remains clearly visible.
- [ ] No invented metrics, compatibility guarantees, or release promises have been introduced.

### Final visual test

Inspect at 320px, 390px, 768px, 1024px, 1440px, and a wide desktop viewport.

Review real rendered results, not only source code. Correct awkward line breaks, alignment inconsistencies, cramped mobile arrangements, overly generous empty space, and poor image scaling.

If a rule in this document produces an inferior result, improve the implementation while preserving the underlying design intent. The goal is not rigid conformity to numbers; it is a coherent, recognizable, exceptionally well-executed product website.

---

## 14. References

These references inform the design process but are not dependencies or instructions to reproduce another project's identity.

- [Impeccable — DESIGN.md](https://github.com/pbakaus/impeccable/blob/main/DESIGN.md) — Explicit design language, typography, geometry, and component rules.
- [Google Stitch — taste-design](https://github.com/google-labs-code/stitch-skills/blob/main/plugins/stitch-utilities/skills/taste-design/SKILL.md) — Critical evaluation of generic compositions and decorative excess.
- [Google Stitch — DESIGN.md](https://github.com/google-labs-code/stitch-skills/blob/main/plugins/stitch-utilities/skills/design-md/SKILL.md) — Structured documentation of visual decisions.
- [WCAG 2.2](https://www.w3.org/TR/WCAG22/) — Accessibility requirements.

**Source of truth:** Actual Oree functionality, screenshots, existing website architecture, and the Android application's `docs/DESIGN.md`.

**Final principle:** Oree should not need to tell visitors that it is beautiful or thoughtfully engineered. The interface, presentation, and attention to detail should demonstrate those qualities themselves.
