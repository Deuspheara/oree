# Design

Oree is designed for a TV remote: clear focus, readable text, enough space around selected artwork and predictable Back behavior. App-only Home must remain useful without optional content or photo access.

## Home and apps

- Keep the Home grid, favorites dock and wallpaper. Use standard Foundation lazy layouts and TV Material3 controls with stable keys.
- Enlarge focused artwork through graphics transforms over fixed input bounds. Reserve room for its scale and shadow; do not clip it against neighboring content.
- Keep focus local to the card. Selection and activation are separate: moving focus must not launch an app or save a setting.
- Show genuine available content. Empty providers do not need invented cards to fill the screen.
- Continue Watching can span the screen while the first resting card aligns with its heading.

## Menus, profiles and folders

Dialogs contain focus and return it to their trigger. Nested panels return one level at a time. Opening a new dialog must consume the held key that opened it.

The profile switcher shows floating avatar and Add Profile circles without a rectangular backing. The Add Profile circle samples the visible Home scene, including Watch Next artwork, and keeps focused/pressed fills transparent. The editor has its own panel. The active-profile marker is independent of focus; editing targets the captured profile ID. Name/avatar drafts survive recreation.

Folders open a bounded app area. Their title is editable, and options provide explicit management actions. Removing a folder returns its apps to Home. Reorder is a preview: arrows move the candidate, Select commits and Back cancels. See the [folder guide](FOLDER_MANAGEMENT.md).

## Settings and visual quality

Settings shows one centered column at a time: categories, category contents or choices. Back returns one level and restores the prior logical focus and viewport. A stored selection remains distinct from keyboard focus. Settings rows have no left accent stripe.

The six-step [Visual experience slider](PERFORMANCE.md) previews a draft above a bounded sample. Only Apply/Finish submits it. Accessibility settings take priority over the preset. Profiles keep their own settings.

Full settings pages and reduced-transparency paths must stay readable without sampled background colors. Keep glass and shared controls in the design system. Do not lower rendering quality as an unmeasured shortcut.

## Search, photos and other screens

Search groups local apps, folders, actions, optional content and accessible photos. Keep results tied to their current request and preserve a surviving logical selection as data changes. Android owns text entry and external search.

Photo Viewer uses Left/Right to browse, Select for controls and Back to close controls before the viewer. It has no slideshow timer. Wallpaper-gallery Select imports/applies an image directly. Manual Ambient has a separate fresh-gesture exit and slideshow; Android owns screensaver scheduling.

Backup import shows a review and cancel-first confirmation before an atomic restore, with one-step undo. Missing images use a local fallback. Operation progress and failures stay visible without stealing focus.

## Accessibility and data states

Maintain English and French UI strings. Check large text, RTL, high contrast, reduced motion and reduced transparency. Keep visible focus when animation is off.

Show loading, empty, cached failure, denied and unsupported states honestly. Use bounded image requests and static fallbacks. Only the focused visible loading app may show its one-pass feedback; stop it on success, failure, blur, lifecycle stop or reduced motion. Do not add permanent idle animation.

For implementation rules, see [Architecture](ARCHITECTURE.md), [Motion](MOTION.md) and [Testing](TEST_PLAN.md). Historical visual studies remain in Git history; they do not define exact platform dimensions or timings.
