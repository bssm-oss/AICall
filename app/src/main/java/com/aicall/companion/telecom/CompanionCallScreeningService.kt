package com.aicall.companion.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import com.aicall.companion.CallCompanionApp

class CompanionCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val app = application as CallCompanionApp
        val silenceSuffix = app.container.settingsRepository.observe().value.silenceNumberSuffix
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        when (ScreeningPolicy.evaluate(number, silenceSuffix)) {
            ScreeningDecision.Allow -> {
                TelecomEventStore.onScreeningDecision(number, ScreeningDecision.Allow)
                respondToCall(callDetails, CallResponse.Builder().build())
            }

            ScreeningDecision.Silence -> {
                TelecomEventStore.onScreeningDecision(number, ScreeningDecision.Silence)
                respondToCall(
                    callDetails,
                    CallResponse.Builder()
                        .setSilenceCall(true)
                        .setSkipCallLog(false)
                        .setSkipNotification(false)
                        .build(),
                )
            }
        }
    }
}
