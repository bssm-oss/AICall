package com.aicall.companion.assistant

import android.content.Context
import android.os.Build
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

object GemmaLiteRtEngine {
    private val lock = Any()
    private var engine: Engine? = null
    private var loadedPath: String? = null
    private var lastError: String? = null

    init {
        runCatching { System.loadLibrary("litertlm_jni") }
    }

    fun getLastError(): String? = lastError

    fun isUnsupportedEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val product = Build.PRODUCT.lowercase()
        return fingerprint.contains("generic") ||
            model.contains("emulator") ||
            model.contains("sdk_gphone") ||
            hardware.contains("ranchu") ||
            product.contains("sdk_gphone")
    }

    fun loadModel(context: Context): Boolean {
        synchronized(lock) {
            val modelFile = GemmaModelManager.getModelFile(context)
            if (!modelFile.exists()) {
                lastError = "Gemma 4 모델 파일이 없습니다."
                return false
            }
            if (loadedPath == modelFile.absolutePath && engine != null) {
                return true
            }

            return try {
                Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
                val config = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = context.cacheDir.path,
                    maxNumTokens = 2048,
                )
                engine?.close()
                engine = Engine(config)
                engine?.initialize()
                loadedPath = modelFile.absolutePath
                lastError = null
                engine != null
            } catch (e: Exception) {
                engine?.close()
                engine = null
                loadedPath = null
                lastError = e.message
                false
            }
        }
    }

    fun generate(prompt: String): String {
        synchronized(lock) {
            val current = engine ?: return ""
            return try {
                var output = ""
                val config = ConversationConfig(
                    systemInstruction = Contents.of("너는 짧고 자연스럽게 한국어로 답하는 도우미다. 답장 내용만 출력해라."),
                )
                current.createConversation(config).use { conversation ->
                    runBlocking {
                        conversation.sendMessageAsync(prompt).collect { chunk ->
                            output += chunk.toString()
                        }
                    }
                }
                output
            } catch (_: Exception) {
                ""
            }
        }
    }
}
