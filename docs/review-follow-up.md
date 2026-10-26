# Review fixes and verification follow-up

Current Dark Arena iteration: [verification and screenshots](dark-arena-verification.md).
Current unsigned version2 candidate: [metadata and checksum](release-candidate.md).
The version1 artifact/runtime details below are preserved as the reviewed baseline;
the consumed-version blocker is removed in the new unsigned candidate.

Reviewed paper-theme app code: `a16dfa8d3dd20bbd1757bded5567525dac058776` (UI branch integrated
foundation fix `522753d725ab226bb900ba65f80455441b884814`). Evidence-only changes
following this point do not alter the tested app source. Both PRs remain drafts;
default branch and Play are unchanged.

| Review finding | Fix and regression evidence |
| --- | --- |
| Partial method/round metadata discarded | Shared event/fighter formatter keeps each known field and labels only its missing counterpart. Unknown result also preserves known metadata without assigning a winner. Android tests cover all four method/round combinations on both screen contexts. |
| Nullable method history refused | Java model, Android reader, Python and JSON Schema preserve literal null→KO and KO→null, including chains. Null names and broken chains remain refused. No fabricated string replaces unknown data. |
| Correction/tournament identity collision accepted only in Python | Collision check runs after corrections add their IDs, matching Java. The previous validator was executed against the adversarial case and accepted it; the corrected one refuses it. Python/JVM/actual Android-loader regressions cover the collision. |
| Grouped accessibility label omitted visible fictional eyebrow | Event/appearance nodes now include the fictional scope; fighter nodes retain outcome/action and explicitly disclose demonstration data. Actual native nodes verify role, clickability and labels; this is not manual TalkBack narration evidence. |

## Test delta and coverage

| Layer | Prior | Current |
| --- | --- | --- |
| Python standard-library data/negative contracts |17 PASS |**22 PASS** |
| JVM production-domain/inset tests |19 PASS |**25 PASS**, zero failures/errors/skips |
| Actual API36 ARM64 device cases |18 PASS at400dp |**29 PASS at360dp**:10 Android-reader,12 original UI,7 new accessibility/navigation/partial-detail cases |
| Unsigned APK/AAB, lint, test compilation |PASS |PASS; no SDK/dependency download or signing in this gate |
| Lint |0 errors/4 warnings |**0 errors/3 warnings**, without suppression or baseline |

The full Draft2020-12 schema validator already available locally also accepts
both nullable-method transitions and refuses null names. The semantic Python gate
handles cross-entity identity/reference rules beyond JSON Schema. JVM tests run
the actual Java semantic model. Android's functional org.json loader runs in the
10 device cases; an equivalent JVM JSON implementation is not cached, so no new
JSON library or heavyweight test stack was installed merely to emulate it.

New cases exercise event and fighter200% text in portrait/landscape, wrapping/no
ellipsis, measured text fit,48dp controls, reachable footer/correction actions,
detail recreation/native back context, missing fighter identity, native query
clear preserving year, and actual accessibility-node semantics. Layout checks
cover the whole measured content; screenshots are viewport captures, not claims
that all long content fits onscreen at once. No image comparison/golden pass.

Installed image inventory in the configured SDK contains API36 only. API24/API29
runtime coverage is **NOT RUN**, with no SDK download/install attempted. Manual
TalkBack narration/focus traversal and physical-phone acceptance remain **NOT RUN**.
Native-node checks do not substitute for them. No accessibility service/access
grant was enabled. Name/data gates and protected Play-installed acceptance remain.

Fresh run `run-dd1fb5ec145f` captured13 requested PNGs. Font scale was restored to1.0,
density override reset, and the task emulator/dedicated ADB server shut down.
The initial prototype fresh install was airplane mode; this current verification
updated the same owned AVD, still in airplane mode. No phone/account/live data used.

## Lint assessment

- Test-only JUnit4.12 had a documented TemporaryFolder information-disclosure issue;
  current tests use neither that API nor secret temporary files. Cached4.13.2 is
  now pinned, removing the known tooling issue without changing the production
  dependency graph. [Official JUnit advisory](https://github.com/junit-team/junit4/security/advisories/GHSA-269g-pwp5-87pp).
- AppCompat23.1.1 remains maintenance/compatibility debt. The warning alone is not
  proof of a specific exploit; no full vulnerability certification is asserted.
  Android recommends AndroidX beyond the final support-library feature release.
  A bounded future migration and missing older-device coverage remain work; no
  blind UI-library/SDK leap was made here. [Official support revisions](https://developer.android.com/topic/libraries/support-library/revisions).
- MergeRootFrame/UselessParent suggestions concern the intentional inset wrapper.
  Its removal previously failed four real scroll-reachability checks. It stays
  without suppressions; those regressions now pass. No root background overdraw
  is introduced to silence or game lint.

## Actual current screenshots

- [Archive](screenshots/archive.png), [event](screenshots/event.png), [fighter](screenshots/fighter.png)
- [Missing result](screenshots/missing-result.png), [correction history](screenshots/correction.png), [search/year](screenshots/search-year.png), [no results](screenshots/no-results.png)
- [200% event portrait](screenshots/large-event-portrait.png), [200% fighter portrait](screenshots/large-fighter-portrait.png)
- [200% event landscape footer](screenshots/large-event-landscape.png), [200% fighter landscape footer](screenshots/large-fighter-landscape.png)
- [200% archive portrait](screenshots/large-font-portrait.png), [200% archive landscape](screenshots/large-font-landscape.png)

Unsigned bundle:898936bytes, SHA-256
`218fcb77af15850f023dbb0725650228f2c51d40eb893911f3ec861930ac8f26`.
Cached official bundletool validation and package inspection pass: existing package,
min24/target36, nondebuggable, no permissions, only launcher exported, intentional
synthetic asset present and test/private markers absent. This is not upload-ready:
versionCode1 is consumed, independent review is pending, and signing/Play-installed
acceptance requires separate current release evidence. No signed artifact or key
is committed; [curated machine-readable results](prototype-results.json).
