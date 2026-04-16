# AI Call Companion

AI Call Companion은 Android Kotlin 기반의 AI 통화 동반 앱입니다. 현재 저장소는 **지원 가능한 범위 안에서 정직하게 동작하는 통화 보조 앱**을 목표로 하며, 기본 다이얼러 역할 요청, `InCallService` / `CallScreeningService` 연결 지점, 통화 제어 훅, 그리고 별도의 STT → AI → TTS 보조 흐름을 제공합니다.

이 저장소는 **지원되지 않는 carrier-call media automation**을 구현했다고 주장하지 않습니다. 현재 제품은 자율 통화 봇이 아니라, Android에서 현실적으로 검증 가능한 AI 통화 companion입니다.

## 해결하려는 문제

이 앱은 사용자가 한 곳에서 아래 흐름을 다룰 수 있게 합니다.

- Android Telecom 역할 기반의 실제 수신 통화 UI 연결 준비
- 간단한 메타데이터 기반 통화 screening 규칙 적용
- AI 응답 제안 생성
- STT/TTS 보조 흐름 시험
- 이후 로컬 LLM 경로로 확장 가능한 구조 유지

## 핵심 기능

- 기본 다이얼러 역할 요청 흐름
- `InCallService`, `CallScreeningService` 스켈레톤 연결
- 활성 Telecom call에 대한 answer / reject / hang-up 훅
- 앱 내 recent Telecom history 저장 및 clear 기능
- 앱 내 `테스트 랩` 섹션에서 assistant/local 상태와 테스트 전용 Telecom 이벤트 점검
- `SpeechRecognizer` 기반 로컬 STT
- Android `TextToSpeech` 기반 로컬 TTS
- 기본 로컬 Gemma 응답 경로
- assistant exchange history 저장 및 clear 기능
- 새 reply 자동 읽기(auto-speak) 옵션
- assistant engine 선택 구조(`Local`, `Demo`)
- GGUF 모델 선택 UI 및 로컬 llama.cpp native bridge scaffold

## 기술 스택

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Android Telecom APIs (`InCallService`, `CallScreeningService`)
- OkHttp
- kotlinx.serialization
- JUnit 4
- Android NDK / CMake (local engine scaffold용)

## 요구 환경

- Android Studio 또는 호환 가능한 Gradle/SDK 환경
- JDK 17
- Android SDK platform 35 및 build-tools
- Android NDK / CMake (로컬 엔진 경로를 계속 확장할 때 필요)
- 실제 Telecom 동작 검증용 physical device 권장

## 설치

1. JDK 17이 준비되어 있는지 확인합니다.
2. `ANDROID_HOME` 또는 `ANDROID_SDK_ROOT`가 유효한 Android SDK를 가리키는지 확인합니다.
3. 아래 명령으로 빌드합니다.

```bash
export JAVA_HOME="/Users/heodongun/Library/Java/JavaVirtualMachines/corretto-17.0.13/Contents/Home"
./gradlew assembleDebug
```

## 환경 및 로컬 모델 설정

앱은 현재 로컬 Gemma 경로를 기본으로 사용합니다. 현재 앱이 직접 보관하는 값은 아래 수준입니다.

- `System prompt`
- `Silence screening suffix`
- assistant engine 선택값
- local GGUF model 선택 정보

현재 앱은 두 가지 로컬 Gemma 경로를 가집니다.

- **앱 관리 Gemma 4 다운로드 경로**: 앱 안에서 `Gemma 4 다운로드`를 눌러 LiteRT-LM 모델을 직접 내려받습니다.
- **개발/에뮬레이터 fallback 경로**: emulator/개발 환경에서는 `10.0.2.2:11434`를 통해 host Ollama의 Gemma 모델에 접근합니다.

## 로컬 LLM 경로

저장소에는 llama.cpp 기반 local engine 경로를 위한 Android native scaffold가 포함되어 있습니다.

- assistant engine에서 `Local` 선택 가능
- GGUF 파일 선택 UI 제공
- Android NDK / CMake 기반 native bridge scaffold 포함
- 현재는 실제 llama.cpp 추론 전체 연결 전 단계이므로, local engine은 준비 상태 및 파일 선택 상태를 안전하게 표현하는 수준입니다

앱 안에서는 `Gemma 4 다운로드` 버튼으로 **Gemma 4 LiteRT-LM** 모델을 직접 내려받을 수 있습니다. 다만 현재 emulator에서 실제 응답 생성까지 검증한 경로는 host Ollama의 **gemma3:4b** fallback입니다. GGUF 파일 선택 UI와 native bridge는 향후 on-device llama.cpp 경로를 위한 scaffold로 유지됩니다.

## 로컬 실행 및 검증

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

## 주요 사용자 흐름

1. 앱을 열고 기본 다이얼러 역할 요청 상태를 확인합니다.
2. 현재 call / screening 상태와 recent Telecom history를 확인합니다.
3. 필요하면 `테스트 랩`에서 가짜 Telecom 상태, 로컬 모델 선택 상태, 로컬 엔진 native bridge 상태를 실제 통화 없이 점검합니다.
4. 필요하면 마이크 권한을 부여합니다.
5. STT를 통해 caller text를 얻거나 직접 입력합니다.
6. assistant engine(`Local`, `Demo`)을 선택합니다.
7. reply를 생성하고 즉시 읽거나(auto-speak), 수동으로 TTS 재생합니다.
8. recent assistant history를 검토합니다.
9. 로컬 모델 / GGUF 참고값 / 시스템 프롬프트 상태를 조정합니다.

## 프로젝트 구조

```text
app/
  src/main/java/com/aicall/companion/
    assistant/
    speech/
    telecom/
    ui/theme/
  src/main/cpp/
  src/test/java/com/aicall/companion/
docs/
  architecture/
  changes/
.github/workflows/
```

## 아키텍처 개요

- `MainActivity`는 Telecom 상태, assistant 흐름, Gemma 4 다운로드/로컬 engine 설정을 하나의 Compose 화면에서 제공합니다.
- `MainActivity`는 별도의 `테스트 랩` 카드에서 테스트 전용 fake Telecom 상태와 assistant 검증 진입점, 로컬 엔진 상태 점검을 함께 제공합니다.
- `MainViewModel`은 settings, telecom state, speech state, assistant history를 결합해 UI 상태를 만듭니다.
- `TelecomEventStore`는 최신 call/screening 상태와 recent Telecom history를 관리합니다.
- `CompanionInCallService`, `CompanionCallScreeningService`는 Android Telecom 연결 지점입니다.
- `SpeechRecognizerManager`, `TextToSpeechManager`는 device speech integration을 담당합니다.
- `AssistantCoordinator`는 앱 다운로드 Gemma 경로, host Ollama fallback, demo 경로를 조정하는 중심 포인트입니다.
- `AssistantSessionRepository`는 recent caller/reply exchange를 보존합니다.
- `NativeLocalLlmBridge`와 `app/src/main/cpp/`는 llama.cpp 연동을 위한 native scaffold입니다.

## 개발 원칙

- 지원되지 않는 carrier-call audio automation을 주장하지 않습니다.
- 장기 보관 provider secret를 앱 안에 넣지 않습니다.
- 검증하지 않은 동작을 문서나 릴리즈에 쓰지 않습니다.
- Telecom, speech, assistant 책임을 분리한 구조를 유지합니다.

## 기여 및 작업 규칙

- feature branch 기반으로 작업합니다.
- 변경은 목적별로 나눠서 커밋합니다.
- build, unit tests, lint를 통과시키고 나서만 완료를 주장합니다.
- README / docs / release note를 실제 동작과 맞춥니다.

## CI 개요

GitHub Actions는 push / pull_request에서 다음을 실행합니다.

- `testDebugUnitTest`
- `lintDebug`
- `assembleDebug`

## 현재 한계

- emulator에서 GSM incoming-call 시뮬레이션 자체는 확인했지만, 이 이미지에서는 `com.google.android.dialer`가 dialer/in-call routing을 계속 유지해 우리 앱의 `CompanionInCallService`까지 실제 통화 UI가 전달되지는 않았습니다.
- assistant history는 생성된 exchange 기록이지, live carrier-call audio transcription이 아닙니다.
- `테스트 랩`의 fake Telecom 이벤트는 앱 내부 검증용 상태 전이일 뿐이며, 실제 carrier call 생성/제어를 의미하지 않습니다.
- `테스트 랩`과 설정 카드에서는 native bridge/모델 선택/다운로드 상태를 보여주고, `Local` 엔진 선택 시 emulator에서는 host Ollama의 `gemma3:4b` 모델로 실제 응답 생성까지 검증합니다.
- 앱은 직접 carrier-call media를 캡처하거나 주입하지 않습니다.
- local llama.cpp native scaffold는 유지되고 있으며, 현재 실제 로컬 fallback 자체는 호스트 Ollama의 `gemma3:4b` 모델로 응답을 생성하고, 앱 내부에는 Gemma 4 다운로드 경로가 추가되어 있습니다.

## 로드맵

- local llama.cpp native bridge를 실제 on-device GGUF inference로 확장
- physical device에서 dialer role / screening / call control 검증 강화
- 한국어 문서와 릴리즈 노트를 계속 실제 동작과 동기화
