# Archive schema v1 and offline foundation

This increment is **synthetic demonstration data only**. All event names, dates,
fighters, venues, pairings and results are fictional, not GLORY facts. No factual
source dataset, official media or website content is bundled. Real-source reuse
requires a separately reviewed permission/provenance decision.

The asset `app/src/main/assets/archive-demo-v1.json` contains 5 events, 8 fighters
and 14 bouts across 3 demonstration years. `schema_version` changes only when
structure changes; `revision` changes when content/corrections change. The strict
loader accepts only schema 1 and `synthetic-demo`, bounded to 1 MiB. Failure is an
explicit local error, never a service fallback or invented replacement result.

- Stable `demo-*` IDs are independent of names/aliases. Event date is an exact ISO
  calendar date. All source, fighter, event, bout and correction IDs are unique.
- Sources identify original fictional authorship only. Each entity/correction has
  source IDs; no official website links or rights assumptions enter this asset.
- Event coverage is `complete-demo-card` or `partial-demo-card`. Dataset and fighter
  coverage text states included events only; appearances are not career totals.
- Bouts have contiguous display `order` starting at 1 (main card order, not temporal
  tournament order), two distinct existing fighter IDs and explicit status/result.
- `completed` results: `red_win`, `blue_win`, `draw`, `no_contest`, `unknown`.
  `cancelled` requires `not_applicable`; unknown status requires unknown result.
  Unresolved/cancelled bouts have null method/round. No winner is guessed.
- Optional tournament ID and stage (`quarterfinal`, `semifinal`, `final`, `reserve`,
  `unknown`) preserve repeated appearances in one card. Tournament IDs cannot
  collide with entities or span events in schema v1.
- Corrections retain ID, from/to revision, target ID/field, old/new values, reason
  and provenance. Chains must connect and their last values match the current
  snapshot. Scalar name/title/method/result/coverage corrections are supported;
  structural changes require a reviewed dataset revision/schema extension rather
  than rewriting stable identity or silently discarding history.

`Archive` is immutable plain Java. It validates semantic invariants and returns
newest-first event search and included appearances. Search is literal substring,
case/accent normalized, across event title/aliases/location/venue and included
fighter names/aliases; year filters intersect search. No full-text database is
needed for this bounded pilot. `ArchiveReader` is the Android JSON adapter.
`docs/archive-schema-v1.json` describes structure; `scripts/validate_archive.py`
checks the actual asset plus references/status/coverage/correction semantics with
Python's standard library. JVM tests exercise production domain queries/invariants;
device tests must exercise the Android reader and actual UI. No database, network,
account, location or background sync is introduced.

Commands:

```sh
python3 scripts/validate_archive.py
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tests -v
```

The existing unsigned Gradle checks validate compilation/JVM/lint without creating
or using a signing key. Runtime captures and signed/protected release acceptance
remain separate evidence. No merge, upload or release is implied by this contract.
