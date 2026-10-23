# Folder management

## Reference and adaptation

The [tvOS 27 organization guide](https://support.apple.com/guide/tv/atvbad14dc6a/27/tvos/27)
documents held Select for actions, explicit Move to destinations, an editable folder title
and Back to Home. The [Apple focus HIG](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection)
distinguishes focus from activation and asks layouts to reserve room for focused artwork.
These establish interaction principles, not measured Apple dimensions or animation timings.

Oree keeps its Home grid, dock, shared artwork lift and local profiles. Select opens a folder;
held Select opens quick actions with explicit Peek, Open full folder and Folder options.
The opening held release is consumed before another action can run. First
entry focuses the first installed member. Up from the first row reaches the editable title.
Folder options exposes cancel-first removal; apps return to Home without uninstalling.
Empty folders remain editable, preserving Oree's existing stored identities and behavior.

Add apps opens an explicit single-app transfer list, excluding current members and showing
Home or the source folder. Selecting an app moves it into the open folder through the existing
serialized organization transaction. Success and cancellation restore the Add apps trigger.
Apps retain their existing context actions for Home, another folder, a new folder and reorder.
Directions preview reorder; Select commits; Back cancels. Missing-package positions remain
stored. No persistence format or repository contract changes.

Name forms retain drafts and show a visible field label/focus border. Entering New folder
focuses its field. Back first follows Android IME dismissal, then returns to destinations;
the next Back closes the transfer panel. Forms use the shared interruptible transition and
explicit reduced-motion choice. Folder header/app focus is restored independently: unrelated
catalog changes cannot refocus the grid while the user is on a surviving header control.
A native window's automatic initial title focus must not override the intended member target.

## Implementation audit

- Per-card focus and press remain local, over fixed native input bounds. Focused folder art
  draws above siblings. Preview cells keep fixed proportions for sparse folders, with bounded
  128×128 requests for square application icons and resource/version cache keys. The shared 12 MiB image cache remains unchanged.
- The existing app artwork separates decode/palette updates from input. Compiler reports
  mark folder/card scopes restartable and skippable. This describes compiler capability;
  it is not a measured runtime recomposition count or a frame-time result.
- Folder reorder previously rebuilt a full catalog lookup both while processing arrows and
  projecting each new candidate. The installed lookup is now remembered by catalog identity
  and reused for membership and ordered projection. This removes redundant lookup construction;
  no quantitative latency improvement is inferred from that code change.
- Restoration checks registered visible items before scrolling. Surviving selections and
  header controls retain native focus during unrelated catalog changes. Missing targets still
  use the bounded registration wait and title fallback. Candidate movement remains animated
  only in reorder mode; reduced motion snaps it.
- The Add apps list uses Foundation lazy rows with stable app keys and content types.
  Management uses mutually exclusive Browse, Options and Remove presentation states.
  Routes capture profile/folder/app IDs before accepted writes; existing ViewModel ownership,
  retained failure and transaction serialization are preserved.

## Acceptance and evidence

`FolderManagementTest` covers header focus during catalog changes, first-row Up, held-release
suppression, nested name entry/Back, explicit transfer, Add apps return and empty-picker Close.
Keep `FolderSearchTest`, `TvInteractionStudyTest`, `AppContextMenuTest` and favorite-reorder
coverage for surrounding organization, persistence, confirmation and modal contracts.

Device results and any optimized rendering measurements are recorded in TEST_PLAN.md and
PERFORMANCE.md. Raw APKs, datastore archives, captures and traces stay outside tracked source.
Compilation and compiler reports do not establish smoothness, physical input latency, process
recreation coverage or broader hardware/accessibility acceptance.

Native Android Dialogs also replace Android composition locals. Folder dialogs explicitly
retain the caller's density, resources and layout direction inside the new window. This
keeps Oree's custom text scale and fixture localization effective. Appearance checks assert
actual English/French labels inside the dialog, rather than inferring locale from its parent.
Removal confirmation temporarily replaces the member grid and disables header actions, so
D-pad traversal stays on Cancel/Remove. Cancel is disabled once a deletion is accepted.

On the actual French catalog, a worded app count consumed enough width to ellipsize even a
short folder name. Cards therefore use a compact numeric count badge; the open panel uses
localized singular/plural app counts. The badge leaves name width independent of translation
length while the full name remains available to accessibility.

The Add apps navigation probe identified very slow live glass redraws on the physical TV.
Folder transfer/name forms now use the existing RetainedMenuSurface for the settled optical
background, with live foreground rows, fields and focus. Source keys include the wallpaper
frame and softening state. The existing size cap, invalidation keys and live/opaque fallback
remain in that design-system component. This does not alter rendering quality or the shared
artwork cache. Retained-surface performance evidence is scoped separately in PERFORMANCE.md.

Nested native-window return can automatically focus a member before the explicit header
restoration runs. Folder focus ownership is frozen while its window is unfocused and is
updated again after its remembered target has been restored. Header actions also capture
their own return target at activation. An invalid foreground performance sample revealed
this race; its zero-frame result was discarded rather than treated as fast rendering.

The folder header places a centered, single-line editable title above a separate row of
single-line capsule actions. The empty state fills the remaining panel and keeps those
actions in the same location. Home app and folder artwork now uses the existing retargetable
focus spring over fixed input bounds, respecting reduced motion. Catalog entries carry the
application icon resource separately from the TV banner; previews use that icon when available.
This visual follow-up received source inspection and formatting only, at the user's request.
Earlier device/frame measurements do not validate these layout, icon or focus-motion changes.
