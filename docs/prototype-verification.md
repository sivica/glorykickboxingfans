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

Final actual results, screenshot list and remaining device/accessibility/release
constraints will be recorded after the instrumented session. No screenshot
comparison/golden pass or protected Play-installed acceptance is implied.
