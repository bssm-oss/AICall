# 2026-04-06 Android call companion MVP

## Background

The repository started effectively empty. The requested product was a Kotlin phone-answering assistant using TTS, STT, and Codex-backed AI.

## Goal

Create the smallest honest Android implementation that can be built and reasoned about without falsely claiming unsupported carrier-call audio automation.

## What changed

- Added a new Android Kotlin project with Jetpack Compose.
- Added `InCallService` and `CallScreeningService` integration points.
- Added a simple Compose UI for telecom state, speech demo, and backend token configuration.
- Added persistent recent Telecom history with an in-app clear action.
- Added local STT and TTS wrappers.
- Added persistent assistant exchange history and an auto-speak reply option.
- Added a backend-compatible assistant client with a local fallback reply.
- Added unit tests, CI, README, and AGENTS documentation.

## Design rationale

Android telecom APIs support dialer-role UI and call screening, but they do not provide a documented third-party hook for autonomous carrier-call audio capture and injection. The implementation therefore separates telecom control from the STT/TTS demo flow and keeps provider credentials off-device.

## Impact

- The repository is now a buildable Android project.
- The app can request dialer-role integration and expose honest telecom capability surfaces.
- The app now preserves recent call and screening events across app restarts.
- The assistant flow now preserves recent caller/reply exchanges and can automatically speak fresh replies.
- Future work can layer on device-validated behavior without rewriting the foundation.

## Verification

- Build verification: `./gradlew assembleDebug` → pass
- Test verification: `./gradlew testDebugUnitTest` → pass
- Lint verification: `./gradlew lintDebug` → pass
- Emulator smoke verification: debug APK installed and `MainActivity` launched successfully on `emulator-5554`
- Telecom role and live incoming-call behavior still require deeper device validation.

## Remaining limitations

- The current manual QA evidence is limited to emulator install and main-screen launch.
- Codex/OpenAI integration depends on an external backend not included here.
- Carrier-call media STT/TTS is intentionally not claimed.

## Follow-up work

- Validate the dialer-role and call-control flow on hardware.
- Add richer UI after real in-call behavior is proven.
- Implement the backend session/token flow required for Codex-backed replies.
