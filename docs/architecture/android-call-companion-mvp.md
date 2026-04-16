# Android Call Companion MVP 아키텍처

## 맥락

이 프로젝트는 의도적으로 **carrier-call companion** 범위에 맞춰져 있습니다. Android가 공식적으로 지원하는 Telecom 통합은 그대로 사용하고, speech/AI 경로는 별도 assistant 흐름으로 분리하여 직접적인 carrier-call media 접근을 구현했다고 주장하지 않습니다.

## 주요 구성 요소

### MainActivity

하나의 Compose 화면에서 다음 역할을 맡습니다.

- Telecom 역할 및 최신 call 상태 표시
- assistant 흐름 구동
- Gemma 4 다운로드와 로컬 engine 설정 제공

### TelecomEventStore

최신 call 요약, screening 요약, 현재 active call 참조, recent Telecom history를 관리합니다.

### TelecomHistoryRepository

recent Telecom history를 shared preferences에 저장해 앱을 다시 열어도 최근 통화/screening 이벤트를 확인할 수 있게 합니다.

### CompanionInCallService

Android Telecom에 등록되어 call 추가/갱신/제거 시 `TelecomEventStore`를 갱신합니다.

### CompanionCallScreeningService

단순 suffix 기반 screening 규칙을 적용하고, allow 또는 silence 결정을 내립니다. 규칙 자체는 pure Kotlin으로 분리되어 테스트 가능합니다.

### SpeechRecognizerManager

`SpeechRecognizer` lifecycle을 관리하고 transcript / error 상태를 UI에 제공합니다.

### TextToSpeechManager

Android `TextToSpeech`를 관리하며, 생성된 최신 reply를 읽을 수 있게 합니다.

### AssistantCoordinator

assistant 생성 요청을 앱 다운로드 Gemma 경로, host Ollama fallback, demo path 사이에서 라우팅합니다. 현재 저장소는 engine selection 상태와 local-engine runtime 분리를 통해 Telecom 코드와 AI 경로를 분리합니다.

### AssistantSessionRepository

recent caller/reply exchange를 저장해 앱 재시작 후에도 assistant history를 확인할 수 있게 합니다.

### NativeLocalLlmBridge

미래의 llama.cpp 경로를 위한 첫 Android-native 진입점입니다. 현재 구현은 JNI/CMake scaffold 수준이며, 실제 GGUF 추론 전체를 구현했다고 주장하지 않습니다.

## 데이터 흐름

1. `MainActivity`가 `MainUiState`를 렌더링합니다.
2. `MainViewModel`이 settings, telecom state, speech state, assistant history를 결합합니다.
3. Telecom services가 `TelecomEventStore`를 갱신합니다.
4. `TelecomEventStore`는 recent event를 `TelecomHistoryRepository`로 저장합니다.
5. assistant 흐름이 caller text를 `AssistantCoordinator`에 전달합니다.
6. `AssistantCoordinator`가 local/demo 경로로 요청을 분기합니다.
7. 생성된 exchange는 `AssistantSessionRepository`를 통해 저장됩니다.
8. reply는 UI에 표시되고, 필요하면 수동 또는 자동으로 TTS 재생됩니다.

## 보안 관점

- 앱은 장기 보관용 provider secret를 저장하지 않습니다.
- 앱은 현재 로컬 Gemma 경로를 우선 사용합니다.
- local fallback reply를 통해 미구성 상태를 숨기지 않습니다.
- assistant history와 Telecom history는 사용자 편의를 위해 로컬 on-device에 저장됩니다.

## 로컬 엔진 관점

- 앱은 Gemma 4 LiteRT-LM 모델을 앱 내부에 다운로드/저장할 수 있는 경로를 가집니다.
- emulator/개발 환경에서는 `10.0.2.2:11434`를 통해 host Ollama의 `gemma3:4b` fallback을 사용해 실제 응답 생성까지 검증합니다.
- GGUF 선택 UI와 `NativeLocalLlmBridge`는 향후 on-device llama.cpp 경로를 위한 scaffold입니다.

## 알려진 아키텍처 한계

- active call 참조는 MVP 단순성을 위해 메모리 기반입니다.
- Telecom 동작은 여전히 physical-device validation이 더 필요합니다.
- local llama.cpp 경로는 아직 native scaffold + model-selection surface 수준이며, 완전한 on-device inference 엔진은 아닙니다.
