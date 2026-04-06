package com.aicall.companion.telecom

object ScreeningPolicy {
    fun evaluate(number: String?, silenceSuffix: String): ScreeningDecision {
        val normalizedNumber = number.orEmpty().trim()
        val normalizedSuffix = silenceSuffix.trim()
        if (normalizedNumber.isBlank() || normalizedSuffix.isBlank()) {
            return ScreeningDecision.Allow
        }
        return if (normalizedNumber.endsWith(normalizedSuffix)) {
            ScreeningDecision.Silence
        } else {
            ScreeningDecision.Allow
        }
    }
}
