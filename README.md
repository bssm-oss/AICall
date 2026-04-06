# AI Call Companion

AI Call Companion is a greenfield Android Kotlin app that implements the smallest honest version of an AI-assisted phone workflow: it can request the default dialer role, expose `InCallService` and `CallScreeningService` entry points, provide simple call-control hooks, and run a separate STT → AI → TTS assistant flow.

This repository does **not** claim unsupported carrier-call media automation. The current MVP is a carrier-call companion, not a full autonomous carrier-call bot.

## Problem it solves

The app gives a user one place to:

- request the Android telecom role needed for real incoming-call UI integration
- apply a simple metadata-based screening rule
- generate AI reply suggestions for live calls
- test speech-to-text and text-to-speech in a controlled assistant demo mode

## Core features

- Default dialer role request flow
- `InCallService` and `CallScreeningService` skeletons
- Call answer / reject / hang-up hooks for the currently active Telecom call
- Persistent recent Telecom history with an in-app clear action
- Local STT using `SpeechRecognizer`
- Local TTS using Android `TextToSpeech`
- Codex-compatible backend configuration using a backend URL and short-lived session token
- Local fallback reply when the backend is not configured

## Tech stack

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Android Telecom APIs (`InCallService`, `CallScreeningService`)
- OkHttp
- kotlinx.serialization
- JUnit 4

## Requirements

- Android Studio or a compatible Gradle/SDK setup
- JDK 17 for Gradle builds
- Android SDK with platform 35 and matching build tools installed locally
- A physical device is recommended for telecom-role validation

## Installation

1. Ensure JDK 17 is available.
2. Ensure `ANDROID_HOME` or `ANDROID_SDK_ROOT` points to a valid Android SDK.
3. Run:

```bash
export JAVA_HOME="/Users/heodongun/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home"
./gradlew assembleDebug
```

## Environment and backend configuration

The Android app intentionally does **not** store a long-lived OpenAI/Codex secret. Instead, configure these values in-app:

- `Backend base URL`
- `Backend session token`
- `System prompt`
- `Silence screening suffix`

The expected backend endpoint is:

- `POST /assistant/respond`
- `Authorization: Bearer <short-lived-session-token>`
- Request body:

```json
{
  "caller_text": "Hello, can you call me back?",
  "system_prompt": "You are an AI call companion..."
}
```

- Response body:

```json
{
  "reply": "Hello, I can help take a message."
}
```

The backend is where Codex/OpenAI OAuth or secret-backed inference should happen.

## Local run and verification

### Build

```bash
export JAVA_HOME="/Users/heodongun/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home"
./gradlew assembleDebug
```

### Unit tests

```bash
export JAVA_HOME="/Users/heodongun/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home"
./gradlew testDebugUnitTest
```

### Lint

```bash
export JAVA_HOME="/Users/heodongun/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home"
./gradlew lintDebug
```

## Main user flows

1. Open the app and request the default dialer role.
2. Review the latest call/screening state on the home screen.
3. Optionally grant microphone permission.
4. Use the speech assistant demo to capture speech, generate a reply, and speak it aloud.
5. Configure the backend URL/session token when a Codex-backed service is available.

## Project structure

```text
app/
  src/main/java/com/aicall/companion/
    assistant/
    speech/
    telecom/
    ui/theme/
  src/test/java/com/aicall/companion/
docs/
  architecture/
  changes/
.github/workflows/
```

## Architecture overview

- `MainActivity` hosts a single Compose screen for role state, call state, speech demo, and backend configuration.
- `TelecomEventStore` keeps the latest call and screening summary in memory for the UI.
- `TelecomEventStore` now also keeps a persistent recent-event history for call and screening actions.
- `CompanionInCallService` and `CompanionCallScreeningService` are the Android Telecom integration points.
- `SpeechRecognizerManager` and `TextToSpeechManager` own the device speech integrations.
- `AssistantCoordinator` calls a backend when configured, otherwise falls back to a local demo reply.

## Development principles

- Do not claim unsupported carrier-call audio automation.
- Keep long-lived provider secrets off-device.
- Prefer evidence-based verification over assumptions.
- Keep UI and UX intentionally simple until device validation proves more complexity is justified.

## Contribution notes

- Use feature branches.
- Keep changes scoped and documented.
- Run build, unit tests, and lint before claiming completion.

## CI overview

GitHub Actions runs unit tests, lint, and a debug assemble on pull requests and pushes.

## Known limitations

- The repository has emulator smoke evidence for app install and main-screen launch, but it does not yet have a validated incoming-call scenario for telecom behavior.
- Recent Telecom history is only as rich as the Telecom events the device actually delivers to the app.
- The app does not transcribe or inject audio directly into carrier-call media.
- The Codex/OpenAI path depends on a backend not included in this repository.

## Roadmap

- Validate the dialer-role flow on a physical Android device.
- Add richer call-state UI once in-call behavior is verified on hardware.
- Replace the local fallback assistant with a real backend session flow.
