# Dark Arena implementation handoff

The current UI target is Figma page **02 — Fight Nation-inspired / Dark Arena**,
not the earlier paper/ink/lime page. The approved specification is preserved
verbatim in [dark-arena-handoff.json](dark-arena-handoff.json): 8,138 bytes,
SHA-256 `fe466ca5da71a6486d8641f4143f0e28060f6b0b1c4ff13394e81412b5c8276a`.
It contains the designer's retained successful Figma results and applied edits.

The implementation's direct `get_design_context` read of archive `18:455`
returned the Figma Starter-plan quota limit. No connector retry or quota bypass was attempted. The owner supplied this retained
specification as the implementation handoff. No earlier Dark Arena render was saved.
The owner later explicitly requested ordinary browser inspection; the saved Figma
UI opened normally, and archive/event/fighter renderings were visually inspected
without changing the design, plan, account or permissions. This supplements the
retained values; font/content/native-component differences remain, and no
pixel-perfect parity or automated golden comparison is claimed.

## Translation to the existing Android stack

- Native Java/XML and existing Support AppCompat components remain. No production
  dependency, service, font download or permission is added.
- The app has no bundled Barlow Condensed font. Android's system
  `sans-serif-condensed` bold is an explicit fallback; Roboto roles use system
  `sans-serif`/`sans-serif-medium`. Font metrics differ from the design specimen.
- Semantic colors, 6dp corners, condensed heading roles, 3dp header rule,
  red demonstration banner, bordered secondary controls, event-card body/footer
  structure and warning-colored correction action follow the retained values.
- Fixed specimen widths/heights become match-parent/wrap-content and minimum
  touch heights. Screens scroll at 200% text in portrait and landscape. Body text
  keeps its system font scaling; no maximum lines, truncation or text-size caps
  are introduced. Native search and year/correction/provenance dialogs keep their
  existing behavior rather than imitating a static drawing with inert controls.
- Insets stay on the outer wrapper. Light system-bar icons are used over the dark
  window background on supported APIs. The wrapper is retained because its
  removal previously caused real large-text footer-reachability failures.
- The JSON includes unused design tokens and temporary research-reference paths
  for accurate provenance. Those reference files are not bundled app assets.
  No Fight Nation screenshot, brand asset, athlete image or remote asset is used.

The data model, nullable correction/ID guards, synthetic bundled dataset, first-use
offline availability, stable navigation IDs and search/year state are preserved.
RINGSIDE remains an uncleared working title. Package, version2/1.1-prototype,
minimum24/target36, current Play listing and published release are unchanged.

Current verification and real screenshots: [Dark Arena verification](../docs/dark-arena-verification.md).
