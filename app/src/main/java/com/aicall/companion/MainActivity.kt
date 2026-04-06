package com.aicall.companion

import android.Manifest
import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aicall.companion.ui.theme.AICallTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AICallTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val microphonePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) {
                    viewModel.refreshCapabilities()
                }
                val dialerRoleLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                ) {
                    viewModel.refreshCapabilities()
                }

                LaunchedEffect(Unit) {
                    viewModel.refreshCapabilities()
                }

                MainScreen(
                    state = uiState,
                    onRequestDialerRole = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val roleManager = getSystemService(RoleManager::class.java)
                            val intent = roleManager?.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                            if (intent != null) {
                                dialerRoleLauncher.launch(intent)
                            }
                        }
                    },
                    onRequestMicrophone = {
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onStartListening = viewModel::startSpeechRecognition,
                    onStopListening = viewModel::stopSpeechRecognition,
                    onGenerateReply = viewModel::generateReply,
                    onSpeakReply = viewModel::speakLatestReply,
                    onClearAssistantHistory = viewModel::clearAssistantHistory,
                    onDraftChange = viewModel::updateDraftCallerText,
                    onBaseUrlChange = viewModel::updateBackendBaseUrl,
                    onBackendTokenChange = viewModel::updateBackendToken,
                    onSystemPromptChange = viewModel::updateSystemPrompt,
                    onSilenceSuffixChange = viewModel::updateSilenceSuffix,
                    onAutoSpeakRepliesChange = viewModel::updateAutoSpeakReplies,
                    onAnswerCall = viewModel::answerCall,
                    onRejectCall = viewModel::rejectCall,
                    onEndCall = viewModel::endCall,
                    onClearTelecomHistory = viewModel::clearTelecomHistory,
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    state: MainUiState,
    onRequestDialerRole: () -> Unit,
    onRequestMicrophone: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onGenerateReply: () -> Unit,
    onSpeakReply: () -> Unit,
    onClearAssistantHistory: () -> Unit,
    onDraftChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onBackendTokenChange: (String) -> Unit,
    onSystemPromptChange: (String) -> Unit,
    onSilenceSuffixChange: (String) -> Unit,
    onAutoSpeakRepliesChange: (Boolean) -> Unit,
    onAnswerCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit,
    onClearTelecomHistory: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("AI Call Companion", style = MaterialTheme.typography.headlineMedium)
            Text(
                "This MVP is an honest carrier-call companion: Telecom role handling, screening, and a separate STT/TTS assistant flow.",
                style = MaterialTheme.typography.bodyMedium,
            )

            StatusCard(
                title = "Telecom role and call state",
                body = "Dialer role held: ${state.hasDialerRole}\n" +
                    "Latest call: ${state.telecomSnapshot.latestCallSummary}\n" +
                    "Screening: ${state.telecomSnapshot.screeningSummary}",
            ) {
                if (!state.hasDialerRole && state.canRequestDialerRole) {
                    Button(onClick = onRequestDialerRole) {
                        Text("Request default dialer role")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAnswerCall, enabled = state.canAnswerCalls) {
                        Text("Answer")
                    }
                    Button(onClick = onRejectCall, enabled = state.canAnswerCalls) {
                        Text("Reject")
                    }
                    Button(onClick = onEndCall, enabled = state.canAnswerCalls) {
                        Text("Hang up")
                    }
                }
                TextButton(onClick = onClearTelecomHistory, enabled = state.telecomSnapshot.recentEvents.isNotEmpty()) {
                    Text("Clear history")
                }
                if (state.telecomSnapshot.recentEvents.isEmpty()) {
                    Text("No recent Telecom history yet.")
                } else {
                    state.telecomSnapshot.recentEvents.forEach { entry ->
                        Text("${entry.timestampLabel} • ${entry.message}")
                    }
                }
            }

            StatusCard(
                title = "Speech assistant demo",
                body = "Recognition available: ${state.speechState.isRecognitionAvailable}\n" +
                    "Microphone permission: ${state.speechState.hasRecordAudioPermission}\n" +
                    "Recognizer error: ${state.speechState.lastError.ifBlank { "None" }}",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!state.speechState.hasRecordAudioPermission) {
                        Button(onClick = onRequestMicrophone) {
                            Text("Grant microphone")
                        }
                    }
                    Button(onClick = onStartListening, enabled = !state.speechState.isListening) {
                        Text("Start STT")
                    }
                    TextButton(onClick = onStopListening, enabled = state.speechState.isListening) {
                        Text("Stop")
                    }
                }
                OutlinedTextField(
                    value = state.draftCallerText,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Caller text / STT transcript") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onGenerateReply) {
                        Text("Generate reply")
                    }
                    Button(onClick = onSpeakReply, enabled = state.latestReply.isNotBlank()) {
                        Text("Speak reply")
                    }
                }
                Text(state.latestReplySource, style = MaterialTheme.typography.labelLarge)
                Text(state.latestReply.ifBlank { "No reply yet." })
                TextButton(onClick = onClearAssistantHistory, enabled = state.assistantHistory.isNotEmpty()) {
                    Text("Clear assistant history")
                }
                if (state.assistantHistory.isEmpty()) {
                    Text("No assistant exchanges yet.")
                } else {
                    state.assistantHistory.forEach { exchange ->
                        Text("${exchange.timestampLabel} • ${exchange.source} • Heard: ${exchange.callerText} • Reply: ${exchange.replyText}")
                    }
                }
            }

            StatusCard(
                title = "Codex-compatible backend settings",
                body = state.backendStatus,
            ) {
                OutlinedTextField(
                    value = state.settings.backendBaseUrl,
                    onValueChange = onBaseUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Backend base URL") },
                    placeholder = { Text("https://your-backend.example.com") },
                )
                OutlinedTextField(
                    value = state.settings.backendSessionToken,
                    onValueChange = onBackendTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Backend session token") },
                    placeholder = { Text("Short-lived token from your Codex backend") },
                )
                OutlinedTextField(
                    value = state.settings.systemPrompt,
                    onValueChange = onSystemPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("System prompt") },
                )
                OutlinedTextField(
                    value = state.settings.silenceNumberSuffix,
                    onValueChange = onSilenceSuffixChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Silence screening suffix") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = state.settings.autoSpeakReplies,
                        onCheckedChange = onAutoSpeakRepliesChange,
                    )
                    Text("Automatically speak newly generated replies")
                }
                Text(
                    "The Android app only stores a backend session token. Your backend is responsible for Codex/OpenAI OAuth or secret management.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider()
            content()
        }
    }
}
