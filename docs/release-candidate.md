# Unsigned fictional prototype candidate — Dark Arena

This is the UI-base candidate record at802b497. The separate disabled-update
branch has its own [artifact and verification record](archive-updates.md).

Tested app-source commit: `9e6d85c8cf747151eeb906726088640781f910a7`.
Main-source tree: `56a30a18d8450c1a9458593b04ef6e846dca7c55`.
Package `com.ivicastojanoski.glorykickboxingfans`, min24/target36 and
versionCode2/versionName1.1-prototype are preserved. This candidate includes the
Dark Arena UI payload; the previous metadata-only checksum is historical.
Current Play listing and published version1 remain unchanged.

| Field | Verified value |
| --- | --- |
| Candidate version |2 /1.1-prototype |
| Recorded active Play baseline |1 /1.0; fresh Console version availability has not been checked this iteration |
| Unsigned AAB SHA-256 |`065c48339bcf3169856133f9ecaea8832721197dda994777ea90a623f0c0ae5a` |
| Size |901485 bytes |
| Build/unit/lint/test compilation |PASS;22 Python /25 JVM;0 lint errors /3 documented warnings |
| Local device execution |31 PASS on the test-signed debug build from this source, API36 ARM64/360dp;200% all primary screens portrait and landscape |
| Official cached bundletool/package audit |PASS; nondebuggable, no permissions, only launcher exported, demo asset present and test/private markers absent |
| Release signing/distribution |Release bundle unsigned and private, not uploaded; only existing approved local test identity used for debug/device checks |

[Design/runtime evidence and screenshots](dark-arena-verification.md) record the
passing session, previous failing search-height probes, clean-data airplane-mode
launch and actual emulator/server cleanup. CI compiles device tests; local runtime
execution is separate. Read the final PR head/run before any integration decision.

Version2 is not reserved in Play. Recheck available versions immediately before
any later authorized upload. Independent review of this UI iteration is pending.
Testing-label/branding and release decisions, release-signing scope, signed-artifact
revalidation and protected Play-installed acceptance remain gates. RINGSIDE is
uncleared, all events/fighters/results are fictional, and no real-data rights are
assumed. No merge, release signing, upload, production rollout or Play change is
implied by this candidate record. API24/29, physical phone and manual TalkBack
remain unrun as documented in the verification record.
