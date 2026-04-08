package com.aicall.companion.telecom

data class TelecomTestLabState(
    val latestCallSummary: String = "테스트 전용 통화 이벤트 없음.",
    val latestScreeningSummary: String = "테스트 전용 스크리닝 이벤트 없음.",
)

enum class TelecomTestLabAction {
    IncomingRinging,
    ConnectedActive,
    Ended,
    ScreeningAllow,
    ScreeningSilence,
    Reset,
}

internal fun TelecomTestLabState.apply(action: TelecomTestLabAction): TelecomTestLabState = when (action) {
    TelecomTestLabAction.IncomingRinging -> copy(
        latestCallSummary = "테스트 전용 가짜 수신 상태(RINGING). 실제 Telecom call은 생성되지 않았습니다.",
    )

    TelecomTestLabAction.ConnectedActive -> copy(
        latestCallSummary = "테스트 전용 가짜 연결 상태(ACTIVE). 실제 carrier call 제어와는 분리되어 있습니다.",
    )

    TelecomTestLabAction.Ended -> copy(
        latestCallSummary = "테스트 전용 가짜 종료 상태(DISCONNECTED). 실제 통화 종료는 발생하지 않았습니다.",
    )

    TelecomTestLabAction.ScreeningAllow -> copy(
        latestScreeningSummary = "테스트 전용 스크리닝 판정: Allow. 실제 번호 차단/허용은 변경되지 않았습니다.",
    )

    TelecomTestLabAction.ScreeningSilence -> copy(
        latestScreeningSummary = "테스트 전용 스크리닝 판정: Silence. 실제 수신 무음 처리는 발생하지 않았습니다.",
    )

    TelecomTestLabAction.Reset -> TelecomTestLabState()
}

internal fun TelecomTestLabAction.toHistoryMessage(): String = when (this) {
    TelecomTestLabAction.IncomingRinging ->
        "[TEST ONLY] 가짜 수신 상태를 기록했습니다. 실제 Telecom call은 생성되지 않았습니다."

    TelecomTestLabAction.ConnectedActive ->
        "[TEST ONLY] 가짜 연결 상태를 기록했습니다. 실제 carrier call 제어와는 분리되어 있습니다."

    TelecomTestLabAction.Ended ->
        "[TEST ONLY] 가짜 종료 상태를 기록했습니다. 실제 통화 종료는 발생하지 않았습니다."

    TelecomTestLabAction.ScreeningAllow ->
        "[TEST ONLY] 가짜 스크리닝 Allow 판정을 기록했습니다. 실제 번호 허용 상태는 바뀌지 않았습니다."

    TelecomTestLabAction.ScreeningSilence ->
        "[TEST ONLY] 가짜 스크리닝 Silence 판정을 기록했습니다. 실제 무음 수신 처리는 일어나지 않았습니다."

    TelecomTestLabAction.Reset ->
        "[TEST ONLY] 테스트 랩 Telecom 상태를 초기화했습니다."
}
