# Unsigned fictional prototype candidate

Candidate configuration commit: `bf692bcb74b1497971cad6f907d0d80ba09ed880`.
The authorized follow-up changes only versionCode1→2 and versionName1.0→1.1-prototype.
Package `com.ivicastojanoski.glorykickboxingfans`, min24/target36, working title and
current Play listing remain unchanged. No signing, upload or release occurred.

| Field | Verified value |
| --- | --- |
| Candidate version |2 /1.1-prototype |
| Recorded active Play baseline |1 /1.0; no newer code recorded in the repository/release evidence |
| Unsigned AAB SHA-256 |`27da8b9fca3b9a599a58cc47f70319fafee129cf439b3da26f3c51158b6ee67f` |
| Size |898943 bytes |
| Build/unit/lint/test compilation |PASS;25 JVM tests,0 lint errors/3 documented warnings |
| Official cached bundletool/package audit |PASS; nondebuggable, no permissions, only launcher exported, intentional demo asset present, no test/private markers |
| Payload comparison with reviewed version1 bundle |Only manifest versionCode/versionName changed; all other manifest nodes, DEX, assets and entries identical |
| Signing/distribution |Unsigned and not uploaded; no key used |

[Current code/coverage review evidence](review-follow-up.md):22 Python and29 local
API36 device cases passed on the prior version1 build at360dp, including200% text
on every primary screen. Those device cases were **not rerun on unsigned version2**;
code/assets are identical, and this change is version metadata only. No device
pass or protected Play-installed acceptance is invented for this new artifact.
CI compiles device tests; it does not execute them. The final PR head/run must
still be matched before any later integration/release operation.

Version2 removes the known consumed-version blocker; it is not a reservation in
Play. Recheck current version availability immediately before an authorized upload.
Remaining decisions concern the concrete owner-only fictional prototype/testing
label and release action, existing signer approval scope and artifact validation,
then actual track/opt-in and protected Play-installed acceptance. RINGSIDE remains
an uncleared working title. Real-data rights are unresolved and no factual dataset
is included; a real-data release needs its own source/implementation approval.
No main-branch merge, Play change, production rollout or new credential is implied.
