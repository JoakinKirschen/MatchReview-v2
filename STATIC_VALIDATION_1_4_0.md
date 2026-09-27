# Static validation — MatchReview 1.4.0

Validated: 2026-09-27

## Scope

- Passes 36–45
- Version code 6 / version name 1.4.0
- Room database version 4
- Backup format 2, with legacy format-1 restore support

## Checks completed

- Parsed all Android XML resources successfully.
- Checked balanced Kotlin braces, brackets, and parentheses in 72 Kotlin files.
- Confirmed `PASS_CHANGELOG.md` contains exactly one completed section for every pass 1–45.
- Confirmed the next-pass tracker starts at Pass 46.
- Confirmed version metadata is consistent in Gradle and release documentation.
- Confirmed format-2 inspection and restore both call database digest verification.
- Added 10 focused JVM test files for the new domain behavior and privacy-safe formatter.

## Build limitation

This workspace does not provide a Java runtime, Android SDK, Gradle installation, or cached
Gradle distribution. Therefore `testDebugUnitTest` and `assembleDebug` could not be
executed here. Run the included GitHub Actions workflow or:

```bash
./gradlew clean testDebugUnitTest assembleDebug
```

No APK is included because no binary could be compiled and verified in this workspace.
