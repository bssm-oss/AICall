# 2026-04-06 Android Call Companion MVP 변경 기록

## 배경

저장소는 사실상 비어 있는 상태에서 시작했습니다. 목표는 Kotlin 기반의 전화 보조 앱이었고, TTS, STT, Codex 기반 AI 경로를 현실적인 Android 제약 안에서 정직하게 구현하는 것이었습니다.

## 목표

지원되지 않는 carrier-call audio automation을 구현한 것처럼 보이지 않으면서도, 실제로 빌드/검증/유지보수가 가능한 Android 구현을 만드는 것입니다.

## 변경 내용

- Jetpack Compose 기반 Android Kotlin 프로젝트를 추가했습니다.
- `InCallService`, `CallScreeningService` 연결 지점을 추가했습니다.
- Telecom 상태, speech demo, 그리고 이후 로컬 Gemma 중심으로 정리된 설정 UI 기반을 추가했습니다.
- recent Telecom history 저장/clear 기능을 추가했습니다.
- local STT/TTS wrapper를 추가했습니다.
- recent assistant exchange 저장/clear 기능과 auto-speak 옵션을 추가했습니다.
- assistant engine selection, GGUF model selection UI, Android native bridge scaffold를 추가했습니다.
- manual backend URL 요구사항을 제거하고, 이후 로컬 Gemma 중심 흐름으로 정리할 수 있는 UX 기반으로 바꿨습니다.
- 실제 carrier call 없이 앱 안에서 assistant, local 상태, 테스트 전용 fake Telecom 상태 전이를 점검할 수 있는 `테스트 랩` 섹션을 추가했습니다.
- unit tests, CI, README, AGENTS 문서를 추가/정비했습니다.

## 설계 이유

Android Telecom API는 dialer-role UI 및 screening은 지원하지만, 제3자 앱이 autonomous carrier-call audio capture/injection을 하는 공식 경로는 제공하지 않습니다. 따라서 Telecom 제어와 STT/TTS assistant 흐름을 분리하고, provider credential은 앱 밖에 두는 구조를 유지했습니다.

## 영향 범위

- 저장소는 이제 빌드 가능한 Android 프로젝트가 되었습니다.
- 앱은 dialer-role integration과 honest Telecom capability surface를 제공합니다.
- recent call/screening history와 assistant history가 앱 재시작 후에도 유지됩니다.
- local LLM을 위한 Android-native build path가 검증 가능한 수준으로 추가되었습니다.
- 앱 표면은 이제 로컬 Gemma 중심 흐름을 반영하며, Gemma 4 앱내 다운로드 경로를 포함합니다.
- fake Telecom 이벤트는 `[TEST ONLY]` 기록과 별도 test-lab 상태로만 반영되어 실제 call handling과 분리됩니다.
- `테스트 랩`에서 로컬 llama.cpp native bridge 상태와 Gemma 4 다운로드 상태를 직접 점검할 수 있고, `Local` 엔진 선택 시 호스트 Ollama의 `gemma3:4b` 모델을 통해 실제 응답 생성도 검증할 수 있습니다.

## 검증

- `./gradlew assembleDebug` → pass
- `./gradlew testDebugUnitTest` → pass
- `./gradlew lintDebug` → pass
- emulator에서 debug APK 설치 및 `MainActivity` launch → pass
- emulator UI dump에서 한국어 `테스트 랩`, `Gemma 4 다운로드`, 로컬 엔진 상태 점검, `GGUF 모델 선택` UI, 그리고 로컬 Gemma 응답 렌더링을 확인
- 앱 내 `테스트 랩`에서 가짜 수신/연결/종료와 가짜 screening 판정이 `[TEST ONLY]` history로 표시되는지 수동 확인 가능
- 앱 내 `테스트 랩`에서 `모델 미선택 / Native: llama.cpp native bridge scaffold loaded` 상태가 표시되는지 수동 확인 가능
- 앱 내 `테스트 랩`에서 `Local` 엔진 선택 후 실제 로컬 응답 생성 및 `최근 응답 엔진: Local` 렌더링 확인
- emulator GSM incoming-call 시뮬레이션은 재현되었지만, 실제 Telecom routing은 `com.google.android.dialer` 쪽으로 향해 우리 `CompanionInCallService`에는 live in-call UI가 바인딩되지 않음을 logcat으로 확인

## 남아 있는 한계

- 현재 manual QA 증거는 emulator install / launch / rendered UI dump 수준입니다.
- emulator에서 실제 GSM incoming-call 시뮬레이션은 확인했지만, dialer role routing은 여전히 stock dialer가 소유합니다.
- 현재 앱은 Gemma 4 앱내 다운로드 경로를 포함하지만, 검증된 실제 응답 생성은 host Ollama의 `gemma3:4b` fallback입니다.
- carrier-call media STT/TTS는 의도적으로 구현했다고 주장하지 않습니다.
- `테스트 랩`은 실제 dialer-role/platform 제약을 우회하지 않으며, test-only 상태 전이만 제공합니다.
- local llama.cpp native scaffold는 아직 on-device GGUF inference 단계는 아니지만, 로컬 fallback 자체는 Ollama + `gemma3:4b`로 실제 응답을 생성할 수 있습니다.

## 후속 과제

- dialer-role / call-control 흐름을 hardware에서 더 깊게 검증합니다.
- 필요하다면 Gemma 기반 host 경로를 on-device llama.cpp GGUF inference로 확장합니다.
- placeholder local engine을 실제 llama.cpp GGUF inference로 교체합니다.
