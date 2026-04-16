package com.aicall.companion.assistant

class AssistantCoordinator(
    private val settingsRepository: AssistantSettingsRepository? = null,
    private val localLlmEngine: LocalLlmEngine = PlaceholderLocalLlmEngine(),
    private val settingsProvider: (() -> AssistantSettings)? = null,
) {
    fun getLocalEngineStatus(settings: AssistantSettings): String {
        return localLlmEngine.getEngineStatus(settings)
    }

    suspend fun generateReply(callerText: String): AssistantResponse {
        val normalizedText = callerText.trim()
        if (normalizedText.isBlank()) {
            return AssistantResponse(
                reply = "응답을 생성하기 전에 먼저 말하거나 텍스트를 입력해 주세요.",
                source = ResponseSource.Demo,
                statusMessage = "입력이 비어 있어 로컬 모델 요청을 시작하지 않았습니다.",
            )
        }

        val settings = settingsProvider?.invoke() ?: requireNotNull(settingsRepository).observe().value
        if (settings.selectedEngine == AssistantEngine.Demo) {
            return AssistantResponse(
                reply = buildDemoReply(normalizedText),
                source = ResponseSource.Demo,
                statusMessage = "데모 엔진이 선택되어 테스트용 응답을 사용했습니다.",
            )
        }
        return localLlmEngine.generateReply(normalizedText, settings)
    }

    companion object {
        fun buildDemoReply(callerText: String): String {
            return "안녕하세요, AI 전화 도우미입니다. 현재 들은 내용은 '$callerText' 입니다. 데모 엔진 응답을 사용합니다."
        }
    }
}
