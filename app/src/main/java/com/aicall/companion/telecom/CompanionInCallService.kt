package com.aicall.companion.telecom

import android.telecom.Call
import android.telecom.InCallService

class CompanionInCallService : InCallService() {
    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            TelecomEventStore.onCallUpdated(call)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callback)
        TelecomEventStore.onCallAdded(call)
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        TelecomEventStore.onCallRemoved()
        super.onCallRemoved(call)
    }
}
