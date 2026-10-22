# Design

The interface is remote-first, with clear focus, generous spacing and predictable return paths. App-only mode must feel complete without media permissions or content providers. Keep visual changes separate from architecture and persistence changes.

## Home and organization

Use standard Foundation lazy layouts and TV Material3 controls. Favorites and folders use stable identities; each app appears in one organization position. Preserve absent-package positions in storage and choose a surviving logical neighbor when focused content disappears.

Focus enlarges artwork through graphics transforms while native input bounds stay fixed. Reserve room for scale/shadow and avoid clipping. Header controls remain discreet until focused. Keep sparse catalogs balanced; do not add fabricated content to fill space.

Move/reorder is an explicit local preview: arrows move the candidate, Select commits and Back cancels. Moving an app between catalog and favorites must preserve its logical focus even when the combined ID order is unchanged.

## Panels and profiles

Dialogs contain focus and return to their trigger. Nested Settings/diagnostics/photo panels retain their parent return target. Profiles keep selection separate from focus, and editing targets the captured profile ID. Draft names/avatar choices survive recreation. Invalid forms show clear disabled/error states.

Keep shared theme, glass, controls and artwork in the design system. Full settings screens and contrast/reduced-transparency paths remain readable without relying on sampled background colors. Do not reduce rendering quality as an unmeasured performance shortcut.

## Accessibility and data states

Maintain English and French strings. Support system and explicit reduced motion; retain visible focus when animation is disabled. Verify large text, RTL and contrast separately. Show loading, empty, cached failure, denied and unsupported states honestly. Status changes must not steal focus.

Image loading uses bounded asynchronous requests and static fallbacks. Only the focused visible loading app may show its existing one-pass feedback; stop it on success, error, blur, lifecycle stop or reduced motion. No permanent idle animation or network artwork backend is needed.
