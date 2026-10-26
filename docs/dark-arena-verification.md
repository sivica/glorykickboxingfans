# Dark Arena native UI verification

This record covers the reviewed UI base at802b497. Subsequent disabled-update
loader changes have [separate verification and limits](archive-updates.md).

Tested source: `9e6d85c8cf747151eeb906726088640781f910a7`, main-source tree
`56a30a18d8450c1a9458593b04ef6e846dca7c55`. PR3 stays stacked on foundation PR2
`522753d725ab226bb900ba65f80455441b884814`. Subsequent documentation/capture
commits preserve the tested app/test source. Default branch and Play are unchanged.

## Design and implementation

Near-black surfaces, white/secondary text, condensed headings, red demonstration
banner/actions/event footers, 6dp cards and bordered secondary controls follow the
[exact retained handoff](../design/README.md). Its 8,138-byte SHA-256 matches the
supplied value. Direct Figma connector inspection hit the Starter tool quota; the
owner then authorized normal browser viewing. The saved Dark Arena page opened
normally and its archive/event/fighter renders were visually inspected. No further
connector call, hidden endpoint, plan/access change or design edit was used.

Android's system condensed bold replaces unavailable Barlow Condensed; no font or
dependency was downloaded. Native search and dialogs stay interactive and use the
dark palette. Actual fictional names, longer coverage explanations and counts
replace design placeholders. Header/back copy keeps the prototype's established
labels. Whole-page scrolling, wrap-content text and minimum targets adapt the
390dp specimens to the tested 360dp device and 200% fonts. Bundled first use stays
offline; optional connection-dependent mock states are not implemented. No borrowed
media/image asset is present. Pixel-perfect parity and golden tests are not claimed.

## Passed checks

| Layer | Evidence |
| --- | --- |
| Dataset validation | Synthetic schema1/revision2, 5 events / 8 fighters / 14 bouts / 1 correction; production model/data/schema/validator unchanged from foundation522753d |
| Python data/adversarial contracts |22 PASS |
| JVM domain/inset/unit cases |25 PASS |
| Unsigned Android gate |APK/AAB build, unit tests, lint and device-test compilation PASS; cached/offline dependencies, no SDK installation |
| Lint |0 errors /3 warnings: old AppCompat maintenance debt and the two retained inset-wrapper layout suggestions; no suppression/baseline |
| Local device execution |31 PASS on API36 ARM64 at360dp;10 actual Android-reader,12 UI,7 accessibility/navigation and2 dark-theme/native-dialog cases |
| Unsigned release audit |Official cached bundletool PASS; package/min24/target36/version2 verified, nondebuggable, no source permissions, only launcher exported, synthetic asset present, test/private markers absent |

The device run uses the existing approved Glory-only test identity. It creates no
credential and performs no release signing. CI compiles these device tests; device
execution and screenshots are local evidence. Exact final-head CI is reported on
the PR and must be read back before integration.

The strengthened archive 200% check initially exposed clipped native search text:
AppCompat23's bundled `abc_search_view.xml` fixes its input child at36dp. An outer
height adjustment alone still failed (layout112px versus child108px). The final
fix makes that child wrap-content and derives the outer floor from actual scaled
font metrics. The assertion was retained and now checks empty hint and typed query
in portrait, plus the whole archive layout after rotation. Two earlier31-case
runs failed this one case; both reports remain locally preserved. Final run
`run-cd2623859eaa` passes all31 cases without weakening assertions.

All three primary screens have 200% portrait/landscape layout and reachability
checks. Tests cover safe insets, native back/recreation, query/year/clear behavior,
corrections, unknown outcomes, partial metadata and native accessibility nodes.
Dark-theme cases check resolved text/system-bar colors, normal-text contrast and
the native year dialog's readable Close action and selection behavior.

Airplane mode was enabled before installation. The existing emulator app was
updated, then **only the task demo package's app data was cleared** before first
launch: this is clean-data offline acceptance, not a fresh-install claim. Font was
restored to1.0, density override reset, and the task emulator/ADB server stopped.
No physical phone, shared AVD or live service was used.

## Real screenshots

All17 PNGs are original captures from the passing session, not Figma composites.
Viewport captures show different scroll positions; long pages remain scrollable.
Landscape footer captures prove reachability rather than showing all content at once.

| State | Capture |
| --- | --- |
| Primary screens |[Archive](screenshots/dark-arena/archive.png), [event](screenshots/dark-arena/event.png), [fighter](screenshots/dark-arena/fighter.png) |
| Search and state handling |[Search/year](screenshots/dark-arena/search-year.png), [no results](screenshots/dark-arena/no-results.png), [year selector](screenshots/dark-arena/year-selector.png), [unknown result](screenshots/dark-arena/missing-result.png), [correction](screenshots/dark-arena/correction.png) |
|200% portrait |[Archive](screenshots/dark-arena/large-font-portrait.png), [event](screenshots/dark-arena/large-event-portrait.png), [fighter](screenshots/dark-arena/large-fighter-portrait.png) |
|200% landscape top |[Archive](screenshots/dark-arena/large-font-landscape-before-scroll.png), [event](screenshots/dark-arena/large-event-landscape-top.png), [fighter](screenshots/dark-arena/large-fighter-landscape-top.png) |
|200% landscape footer |[Archive](screenshots/dark-arena/large-font-landscape.png), [event](screenshots/dark-arena/large-event-landscape.png), [fighter](screenshots/dark-arena/large-fighter-landscape.png) |

## Not run / remaining gates

API24/API29 images are absent, so those runtimes were not run or installed. Physical
phone, manual TalkBack narration/focus traversal, golden image comparison and a
protected Play-installed prototype remain unrun. Native-node assertions do not
replace manual accessibility acceptance. RINGSIDE remains uncleared, data remains
fictional, and no factual source/reuse right is inferred. Independent review is
required before any integration decision. No merge, release signing, upload or
Play change occurred. See [candidate metadata](release-candidate.md) and
[machine-readable results](dark-arena-results.json).
