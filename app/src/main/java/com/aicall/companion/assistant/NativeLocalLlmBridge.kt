package com.aicall.companion.assistant

class NativeLocalLlmBridge {
    external fun getNativeEngineStatus(): String

    companion object {
        init {
            System.loadLibrary("aicall_local_engine")
        }
    }
}
