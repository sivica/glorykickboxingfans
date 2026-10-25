# Prototype implementation and evidence

Design reference: [editable Figma frames](https://www.figma.com/design/Dnm0hwiCkUIi4lNqC9oNKg).
Read-only design-to-code inspection used archive3:43, event3:44, fighter3:45,
no-results3:47, correction3:50, missing-result3:51, tokens6:54 and large-text11:105.
Palette/spacing/typography are XML tokens. Cards/notices/native actions are shared;
Android's existing Support SearchView supplies search/clear semantics. Its Material
search symbol is a native component, not a copied official sports asset. No remote
Figma asset is used at runtime; the inspected screens contain no image fills.

## Deliberate adaptations

- Actual fictional pilot names/counts replace Figma placeholder content. Display
  labels say fictional directly. RINGSIDE is a working title only; package and
  Play naming/settings are unchanged.
- Bundled data works from first offline launch. No network cache/fetch/retry,
  connectivity permission or mock first-use blocker is introduced. Invalid bundle
  or item ID produces an explicit local error; it never substitutes results.
- Three native Activities use the existing stack/back navigation. Query/year and
  native scroll state survive recreation/return; private detail Activities are
  not exported. Back means the actual previous screen.
- The whole page scrolls, including header, so large text and landscape do not
  strand controls below a fixed tall header. Text wraps/grows; controls are at
  least48dp. All outcomes, cancellation and unknown state are stated in words.
- The synthetic correction is a retained name spelling correction, shown in an
  accessible dialog with old/new values, revision, reason and source. It is not
  presented as a real fight result correction. No unapproved factual links appear.
- Event date/venue/location are known in this pilot; nullable method/round and
  explicit unknown/cancelled results are supported. Unknown event dates and a
  general editable archive/correction engine are outside this schema slice.

## Evidence layers

Before UI work, foundation checks passed17 Python cases,19 JVM cases and lint
with0 errors/four existing warnings. The six Android reader cases compiled;
execution is recorded separately below when complete. CI compilation alone is
never counted as a device pass.

Local runtime uses a new task-owned API36 ARM AVD, installed image, airplane mode
before first install/launch, no personal account, physical phone or GPS. Existing
Glory-only local test signing and ADB identity are reused without creating keys.
Emulator and dedicated server must be shut down after the session. No cloud/Play
operation, new term, account/security setting or rollout is part of verification.

## Final results (app code d76d866)

| Layer | Actual result |
| --- | --- |
| Python data validation/negative contracts | PASS,17 tests; full local JSON Schema cross-check also passed with existing tooling |
| JVM unit tests | PASS,19 tests, zero failures/errors/skips (15 archive,3 inset,1 legacy arithmetic) |
| Unsigned APK/AAB, lint, instrumentation compilation | PASS;97-task gate completed; no SDK/dependency download or signing |
| Lint |0 errors,4 warnings: old JUnit/AppCompat versions and two intentional inset-wrapper layout suggestions; no suppression/baseline |
| Real device cases | PASS,18 cases:6 packaged-reader and12 production UI cases on API36 ARM64 |
| Actual captures |7 committed below;9 fresh capture files locally in run-368373a1e096; no golden comparison |
| Release bundle inspection | PASS: cached official bundletool, correct package/min24/target36, nondebuggable, no permissions, only launcher exported, demo asset present, test/private markers absent |
| GitHub CI | Foundation PR #2 passed; UI PR #3 result must be checked against its final head |
| API24/API29/physical phone/TalkBack/200% event+fighter screens | NOT RUN; current large-text device case covers archive portrait/landscape and reachable actions |
| Signed/protected Play-installed prototype | NOT RUN; no upload key use, signing enrollment or release |

The original fresh emulator install/launch was in airplane mode and passed on the
initial UI build. Final18-case verification used the updated app on the same owned
AVD, still in airplane mode; it was not a new installation. The bundle is always
local by construction, without a connectivity or cache prerequisite.

Failure history is retained locally: run1 had17/18 pass with a rotation timing
failure; run2 passed18/18 after waiting for stable rotation frames; run3 exposed
four scroll failures after simplifying the inset wrapper. Restoring the wrapper
produced the final18/18 pass with unchanged scroll assertions. Per-run capture
isolation prevents stale images being counted from those failed runs. Task-owned
emulator/server are stopped and font scale restored to1.0. No physical phone used.

Unsigned AAB SHA-256: `1045f434ef4139a8bb12bc5509f54ac2cec238ce6ec6bf0e26f06b58248b7549`,
898665 bytes. Source code commit `d76d866d4817f204140e5f920bf14da6c630545f`,
main source tree `715b751ceafeea96ddb3feef8f91f1bd13cf6e42`.
See [curated results](prototype-results.json). Private APK/AABs and raw local logs
stay outside the repository. No private paths, keys or accounts are in this record.

This is ready for independent draft review, **not an upload-ready release**.
Current code1 was already used by v1: a future approved update needs a fresh
monotonic version code, reviewed source/tree, signing/artifact revalidation and
Play-installed acceptance. RINGSIDE is uncleared, and factual data rights remain
gated; the demo must keep fictional labels. Main/current Play release unchanged.

## Actual Android screenshots

Captured from the real app in airplane mode, not renders of Figma:

- [Archive](screenshots/archive.png), [event](screenshots/event.png), [fighter](screenshots/fighter.png)
- [Filtered search](screenshots/search-year.png), [no results](screenshots/no-results.png)
- [200% archive portrait](screenshots/large-font-portrait.png), [200% landscape footer](screenshots/large-font-landscape.png)

## Local privacy/data-flow audit

| Input | Handling / destination | Evidence and limit |
| --- | --- | --- |
| Fictional archive JSON | Bundled asset → bounded reader → immutable domain → Views | No network fallback or factual source links |
| Search/year/scroll | Local Activity memory and OS saved-instance state | No analytics or remote transmission; this is not a blanket privacy declaration |
| Provenance/correction view | Local synthetic source/history → native dialog | No browser/API or external report submission |
| Test captures | Instrumentation only → session-specific app external files → owned evidence folder | No production capture code; not an app data collection feature |

No network permission or network SDK was added. Existing production dependency
source was reviewed, and merged release manifest/package inspection is recorded
with the final unsigned bundle evidence. Store-added protection and a later
Play-delivered artifact require their own audit; no privacy declaration was filed.

The inset wrapper is intentional: removing it made four real scrolling checks
fail on API36, including the footer action at 200% landscape. It keeps system/IME
padding outside the ScrollView viewport; the MergeRootFrame/UselessParent lint suggestions are accepted
with this runtime evidence rather than suppressed. Stale screenshot reuse is
prevented by a new captureSession per run; only files in that exact directory
count as evidence. Failed-run screenshots must not be reported as current passes.
