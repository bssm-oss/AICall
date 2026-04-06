# Android call companion MVP architecture

## Context

This project is intentionally scoped as a carrier-call companion. Telecom integration is real where Android supports it; speech and AI are demonstrated through a separate assistant flow that does not claim direct carrier-call media access.

## Main components

### MainActivity

Hosts one Compose screen with three responsibilities:

- show telecom role and latest call state
- drive the assistant demo flow
- edit backend/token settings

### TelecomEventStore

Stores the latest call summary, screening summary, current active call reference, and the recent Telecom event timeline used by the UI.

### TelecomHistoryRepository

Persists the recent Telecom event timeline in shared preferences so the user can reopen the app and still inspect what happened during the most recent call and screening interactions.

### CompanionInCallService

Registers with Android Telecom and updates `TelecomEventStore` whenever a call is added, updated, or removed.

### CompanionCallScreeningService

Evaluates a simple suffix-based screening rule and either allows the call or silences it. The policy is pure Kotlin so it can be unit-tested.

### SpeechRecognizerManager

Owns `SpeechRecognizer` lifecycle and exposes transcript/error state for the UI.

### TextToSpeechManager

Owns Android `TextToSpeech` and speaks the latest generated reply when the user asks for it.

### AssistantCoordinator

Calls a backend when a base URL and short-lived session token are configured. Otherwise, it returns a local fallback reply so the demo flow still works.

## Data flow

1. `MainActivity` renders `MainUiState` from `MainViewModel`.
2. `MainViewModel` combines settings, telecom state, speech state, and assistant outputs.
3. Telecom services update `TelecomEventStore`.
4. `TelecomEventStore` persists recent events through `TelecomHistoryRepository`.
5. The assistant flow sends caller text to `AssistantCoordinator`.
6. The reply is surfaced in the UI and optionally spoken locally.

## Security posture

- The app stores only a backend session token, not a long-lived provider secret.
- Codex/OpenAI authentication belongs on the backend.
- The app’s local fallback reply makes the unsupported/unconfigured state obvious.

## Known architectural limits

- Active call references are held in memory for MVP simplicity.
- Telecom behavior still requires physical-device validation.
