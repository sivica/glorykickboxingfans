# Disabled remote JSON update mechanism

This follow-on targets `master` after reviewed foundation PR2 and Dark Arena PR3
merged at `d562c4581116614ba96d73214f3cdc89971d03ae`. The rebase preserves the
entire tested app/test tree. It prepares an app-side mechanism;
**no endpoint is configured, no automatic request/scheduler/button is installed,
and the manifest still has no INTERNET permission**. The app continues to open its
bundled fictional archive in airplane mode. No hosting account, deployment,
credential, signing-key change, factual dataset or Play action is included.

## Actual behavior

`ArchiveRepository.get(context).load()` reads the bundled baseline and prefers a
fully valid saved archive only when its dataset ID matches and revision is newer.
Missing, malformed, incompatible or stale saved data falls back to the bundle.
The fixed cache path is inside private no-backup app storage; payload fields never
become filesystem paths. Source labels say bundled/saved based on the snapshot
actually loaded. There is no invented connected/updating/success indicator.

The explicit worker-thread integration hook `checkForUpdates()` currently returns
`DISABLED` before touching transport or store. Its default `Config.disabled()` is
in `ArchiveRepository`; the endpoint factory's name is not an approval mechanism.
Enabling requires a separate reviewed configuration and Internet-permission change,
plus a deliberate calling/scheduling/UI policy. No background updater is active.

For an enabled, approved endpoint, one check reads a complete candidate with:

- HTTPS on443, no URL credentials/query/fragment, no redirects, no authentication
  or cookie store installed, normal platform TLS trust/hostname checks.
- HTTP200 and `application/json`, UTF-8, identity encoding; oversized declared
  lengths and compressed responses are refused. Reads also enforce1MiB without
  trusting the Content-Length header.
-3-second connect/5-second read timeouts, a10-second cooperative transfer budget,
  stream/connection cleanup and thread-interruption checks. DNS/OS blocking is not
  guaranteed to obey that total budget; no hard end-to-end timeout is claimed.
- The same strict UTF-8 reader, complete single object, unique decoded keys,
  maximum32-container nesting before Android parsing, schema1/synthetic-only and semantic model used by
  the app: global IDs, references, nullable corrections, coverage and outcomes.
  Validation completes before persistence. A different archive ID, incompatible
  schema, invalid payload, equal/stale/downgrade revision never activates.

The single-process repository serializes reads/writes and repeats validation plus
the revision check inside its write lock, so a newer concurrent commit wins. It
copies caller bytes, writes through Android `AtomicFile`, syncs, calls finish/fail
as appropriate, and reads committed bytes back before reporting `ACTIVATED`.
Rejected/download-failed candidates leave the existing valid cache available.
Interrupted pending writes recover the previously committed file; storage damage
to an already committed cache falls back to bundled data. This is not a promise
against hardware failure or a tamper-resistant revision ledger.
Late cancellation or a readback error can occur after a valid atomic commit; callers
must reload the current revision before retrying. Monotonic activation prevents a
duplicate revision from replacing it. Production uses
one repository singleton; multi-process writers are unsupported. Official API
constraints: [AtomicFile](https://developer.android.com/reference/android/util/AtomicFile),
[HttpURLConnection](https://developer.android.com/reference/java/net/HttpURLConnection).

Screens hold immutable snapshots until recreated. Successfully saving data does
not silently rebuild an open screen or manufacture an active-update status.

## Security/product limits before enabling

TLS delivery and a higher revision do not prove sporting facts, redistribution
rights, correction-history continuity or publisher integrity. There is no signed
payload protocol or pinning. The approved host must be a trusted curator and direct
JSON URL; DNS/private-network destination policy, hosting/operational controls,
rollback/recovery policy and any future signing mechanism need their own review.
Requests would expose ordinary IP/TLS/HTTP metadata to that host; privacy/dataflow
and relevant Play declarations must be reviewed before network enablement.

The1MiB wire bound is not a UI performance guarantee. Current native screens build
their small pilot lists eagerly. A real archive needs a profiled record budget or
pagination/RecyclerView work before approval to enable a larger feed. No full
career coverage, real GLORY data rights or production readiness is assumed.

## Verification

Local unsigned APK/AAB build, lint, device-test compilation and all63 JVM/30 Python
cases pass. Lint remains0 errors/3 existing warnings. The complete **46-case API36
ARM64 device suite passes** at360dp, including all31 baseline reader/UI/accessibility
cases and15 actual repository/AtomicFile cases. All primary screens retain200%
portrait/landscape checks. CI compiles device tests; actual execution is local.

Earlier41-case execution had34 passes/7 exact diagnostic-text assertion failures;
those assertions were aligned with the shared adapter wording while retaining
invalid-data refusal. One later run was interrupted for an emulator identity scope
review. Both records remain preserved. After owner approval for existing SDK-key
use, run `run-5b94389d11e7` passed the earlier42 cases. After the review corrections
below, final run `run-96884fd79255` passes all46 cases. The private identity's metadata
remained unchanged; no key values were displayed/exported. Font returned to1.0,
density override reset, and task emulator/server stopped. Airplane mode preceded
installation and task-only demo-data reset; the package was absent before installation in this final run, followed by the
explicit demo-data reset before first launch. No phone/shared AVD or live endpoint
was used. No credential is included in source.

Unsigned candidate version2/1.1-prototype is907567bytes, SHA-256
`07e03add386f12b5ca225626b49af76b70104123622e12b2c92d63a156597fbb`.
Cached official bundletool/package audit passes with min24/target36, no permissions,
only launcher exported and no test/private markers. The bundle stays private and
unsigned; it is not a release or a reservation of version2 in Play.

JVM tests use fake transport/connection/clock/store and small semantic fixtures;
they do not claim a real TLS, DNS, JSON-loader or server test. Platform cases use
the actual Android JSON reader/AtomicFile and synthetic revisions generated from
the bundled fixture, in isolated test files. They cover persistence/recreation,
stale/downgrade/identity refusal, bad schema/JSON/UTF-8/global IDs, byte limits,
corrupt-cache fallback, interrupted writes and failure injection at public
`AtomicFile.startWrite`/`finishWrite` boundaries. Actual platform file read,
persistence and `failWrite` rollback are retained. An
abandoned descriptor simulates interruption before commit; an actual device power
cut is not tested. No production-cache cleanup or live service call is used.

[Final default offline archive capture](screenshots/updates/archive-default.png)
shows the same fictional bundled screen with no invented active-update status.

Exact counts/results and source/artifact provenance are in
[archive-update-results.json](archive-update-results.json). CI uses the pull request synthetic merge, rather than installing its artifact.
The PR records the exact head/base/checkout commit and whether the merge tree
matches the tested source. CI runs Python/JVM and compiles platform tests only;
local signed-debug instrumentation is separately recorded from the exact app/test
source. A successful CI compilation is never reported as device execution. API24/API29, physical
phone, manual TalkBack, protected Play install and live endpoint integration remain
unrun. Foundation/UI PRs are merged; this updater remains a separate draft awaiting
independent review. Its merge/release and network enablement are separate gates.

## Independent review corrections

- The old failure test obstructed a `.new` file, coupling it to API36 implementation
  details; API24/API29 use a backup scheme. Tests now inject failures through the
  public AtomicFile operations, with actual file persistence, pending-write recovery
  and rollback still exercised. No SDK-version filename branch is needed. API24/29
  runtime execution is still unrun; source/API compatibility is not a device pass.
- A focused pre-fix API36 regression reproduced acceptance/activation of valid JSON
  followed by junk (`run-78fca47a3434`, one expected failure). A small syntax-only
  gate now validates the whole object and unique decoded keys before JSONObject
  interprets it. Comments, single quotes, trailing content, bad escapes/numbers,
  duplicate escaped keys and invalid Unicode fail closed. The model still performs
  all archive semantics; there is no new JSON-library dependency or updater framework.
- The32-container limit is checked during bounded descent before recursive Android
  parsing, including unknown fields. Actual reader/download/cache regressions use
  a4096-level payload below1MiB and verify rejection/fallback, not a caught stack
  overflow. CLI loading enforces the same new UTF-8/byte/depth/unique-key rules.
