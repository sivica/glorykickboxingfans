# Ringside — offline fan archive prototype

**Working title; not a cleared brand. Unofficial and not affiliated with GLORY.**
This Java/XML prototype contains **fictional demonstration events, fighters and
results only**. It ships no factual GLORY-site dataset, official logo/photo/video,
streaming, account, payment, backend, tracking or new permission. Main and the
published v1 remain unchanged while these draft changes are reviewed.

Three screens: event archive with literal search and year filter; ordered event
cards/results; fighter appearances restricted to this included dataset. Aliases,
partial coverage, cancellations, draws/no-contests, unknown results, tournament
rounds and versioned name corrections remain explicit. A missing entry is never
converted into a win/loss or complete career record. All demo data is bundled and
available on a fresh airplane-mode launch; there is no network-dependent first use.

See [data contract](docs/archive-data-contract.md), [schema](docs/archive-schema-v1.json)
and [design/runtime notes](docs/prototype-verification.md). Data sources and rights
for any future factual archive remain separate, unresolved adoption work.

## Existing stack and checks

- Package `com.ivicastojanoski.glorykickboxingfans`, min API24, compile/target36.
- AGP8.10.1 / Gradle8.14.6 / JDK21, Java8 source. Existing Support AppCompat23.1.1 retained; unused Design/FAB removed; AndroidX is test-only. No Compose or production dependency added.
- These draft APKs retain version1/1.0 for local verification. A future update
  needs a fresh monotonic Play version code, independent review and release gates.

```sh
python3 scripts/validate_archive.py
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tests -v
./gradlew --no-daemon --no-build-cache --no-configuration-cache --console=plain \
  -Pandroid.builder.sdkDownload=false --init-script ci/unsigned-debug.init.gradle \
  :app:assembleDebug :app:bundleRelease :app:testDebugUnitTest :app:lintDebug \
  :app:compileDebugAndroidTestJavaWithJavac
```

Use a task-owned writable Gradle/Android home and compatible existing JDK/SDK.
The init script disables debug signing, so this command creates/uses no signing
credential and produces an un-installable unsigned APK. Local runtime evidence
uses the separately approved existing Glory-only test identity, never the upload
key or another app's key. Keep all credentials/artifacts outside source and logs.

CI validates the synthetic dataset, runs negative Python contracts and unsigned
build/JVM/lint checks, and compiles device tests. Pinned actions/read-only token,
no signing, caches, artifact uploads or SDK license acceptance remain. Compilation
is not device execution; screenshot capture is not a golden comparison. No release
workflow is added. Existing toolchain/dependency warnings remain visible.

Original wrapper files retain their Apache-2.0 notices. No new repository-wide
license is inferred for the pre-existing app. New demo data/UI in this change is
original; no third-party sports content is redistributed.
