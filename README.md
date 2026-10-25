# Glory Kickboxing Fans

This is still the original Java/XML **Hello World template**: a title, placeholder
fragment, floating action button Snackbar and Settings menu. No kickboxing feed,
backend, authentication, GPS or product data has been added.

## Approved compatibility scope

- Android Gradle Plugin **8.10.1**, pinned Gradle **8.14.6**, existing JDK **21.0.11**.
- Compile/target SDK **36**, Build Tools **35.0.0**, Java source/bytecode **8**.
- Minimum SDK **24** (Android 7), increased with owner approval. Android 10/API29
  remains supported; devices below API24 are no longer supported.
- Package remains `com.ivicastojanoski.glorykickboxingfans`, version 1 / 1.0.
- Existing production Support AppCompat/Design **23.1.1** remain. AndroidX is used
  only by the instrumented test harness; Jetifier is disabled. No UI redesign.

The launcher is explicitly exported. Platform-only inset handling keeps the
existing toolbar/content/FAB clear of system bars and cutouts and handles the
Android 7-10 API fallback. It starts from original padding so redispatch does not
accumulate padding. The FAB now has an accessible description. No custom Back
handler was added without a demonstrated need; a device test covers the system
Back action on this single-screen app.

## Unsigned local checks

Use task-owned `GRADLE_USER_HOME` and `ANDROID_USER_HOME`, the existing SDK/JDK,
then run without using or creating a signing credential:

```sh
./gradlew --no-daemon --no-build-cache --no-configuration-cache --console=plain \
  -Pandroid.builder.sdkDownload=false --init-script ci/unsigned-debug.init.gradle \
  :app:assembleDebug :app:bundleRelease :app:testDebugUnitTest :app:lintDebug \
  :app:compileDebugAndroidTestJavaWithJavac
```

The helper disables debug signing for this invocation. The resulting APK cannot
be installed. Device APK/test-APK signing used an explicitly approved, private Glory-only
local test key stored outside this checkout. Never use that test key for Play or
copy another app's signing key. The separately approved host ADB transport identity
was used read-only for the isolated emulator session.
The generic kit task plan does not automatically add this init script, so use the
explicit unsigned command above rather than its execution mode for this phase.
SDK auto-download remains disabled; no JDK/SDK/image installation occurred.

## Verified status and limits

- **Unsigned APK/bundle, JVM tests, lint and device-test compilation passed on
  Gradle8.14.6**, 97 tasks executed. Packaged identity/version and **min24/target36**
  were inspected. The rebuilt debug APK payload outside META-INF exactly matches
  the locally device-tested app; no additional emulator run was needed for this
  build-tool-only security correction.
- **4 unit tests passed**, 0 failures/errors/skips: the original arithmetic test
  plus three padding regressions (repeated dispatch/reset, landscape side insets,
  preservation of base spacing).
- **6 real production-Views device cases passed on API36**: launch/identity/inset
  bounds, FAB/Snackbar, Settings, recreation, landscape and Back. First-launch icon
  contrast was corrected after actual capture review. A mistaken test expectation
  for a visible Snackbar action was corrected against the unchanged original null
  listener. Captures are actual images, without golden regression comparisons.
- **Lint passed: 0 errors, 4 warnings**, with no suppression or baseline. Warnings
  cover the newer JUnit version, the two old UI dependencies, and the now
  redundant values-v21 resource folder. Java 8/deprecated platform compatibility
  API compiler notes remain visible. Gradle reports deprecated build features that
  need separate work before a future Gradle9 upgrade.
- The production runtime dependency report contains only the original support
  libraries. AndroidX test dependencies do not enter the production classpath.

The original Support23.1.1 UI rendered and passed these cases on one API36 AVD;
this is limited coverage, not proof across every supported device. API24/API29,
physical Android10, large-font and accessibility acceptance remain untested. The
task-owned emulator and dedicated ADB server are shut down after each run. No phone,
release key, GitHub publication or Play change was used to obtain this evidence.

Gradle was advanced within 8.x after the release review found five high-severity
[official Gradle advisories](https://github.com/gradle/gradle/security/advisories)
affecting 8.11.1. The 8.14.6 distribution checksum and generated wrapper JAR match
the official published checksums. AGP and Android SDK levels were kept unchanged.

## Pull request checks

The app-specific `glory-checks.yml` workflow builds unsigned debug and release
artifacts, runs JVM tests/lint and compiles device tests on Ubuntu 24.04/JDK21, and verifies the reviewed
wrapper JAR checksum. It uses pinned official actions, read-only repository
permissions, no persisted checkout credentials, no signing key and no artifact
uploads. SDK36/BuildTools35 must already be present on the hosted runner; the job
fails if they are absent instead of accepting licenses or downloading an SDK.
Device checks are local evidence and are not rerun by this CI job.

This is an app-specific gate, not publication of the local modernization kit or
activation of its release blueprint. `ci/android-modernization.json` describes the
local kit pilot; the kit's scripts are not vendored into this repository.

The unsigned release AAB also passed the cached official bundletool validator and
manifest inspection: original package/version, min24/target36, nondebuggable,
no permissions and no native libraries. It is not a signed Play upload artifact.
The source and packaged DEX audit found no private/test markers in the inspected
paths/content; this bounded audit is not a complete vulnerability certification.

The official Gradle wrapper scripts retain their Apache-2.0 notices; the wrapper
JAR embeds its license. No new repository-wide license is inferred from generated
wrapper files or imposed on the pre-existing application.

## Internal release boundary

The eventual initial internal artifact is a **template**, not a kickboxing product.
Before owner-only internal distribution, verify runtime behavior, build a clean
nondebuggable bundle, inspect identity/signature/test exclusion and confirm the
actual app/owner/tester state. Signing credentials, secret storage, Play signing
and policy/terms declarations, cloud identities/access grants require their exact
approvals. No Vedro credential or account identifier may be copied. No production
or public rollout, billing or new feature/data source is authorized.
