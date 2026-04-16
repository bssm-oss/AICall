package com.aicall.companion

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.FilterChip
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
import com.aicall.companion.assistant.AssistantEngine
import com.aicall.companion.telecom.TelecomTestLabAction
import com.aicall.companion.ui.theme.AICallTheme

private const val TEST_LAB_SAMPLE_CALLER_TEXT = "안녕하세요. 오늘 예약 시간을 다시 확인하고 싶어서 전화드렸어요."

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
                val localModelLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                ) { uri: Uri? ->
                    if (uri != null) {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                        viewModel.updateLocalModelSelection(
                            uri = uri.toString(),
                            label = uri.lastPathSegment.orEmpty(),
                        )
                    }
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
                    onSelectedEngineChange = viewModel::updateSelectedEngine,
                    onSystemPromptChange = viewModel::updateSystemPrompt,
                    onSilenceSuffixChange = viewModel::updateSilenceSuffix,
                    onAutoSpeakRepliesChange = viewModel::updateAutoSpeakReplies,
                    onSelectLocalModel = { localModelLauncher.launch(arrayOf("*/*")) },
                    onInspectLocalEngineStatus = viewModel::inspectLocalEngineStatus,
                    onDownloadGemmaModel = viewModel::downloadGemmaModel,
                    onAnswerCall = viewModel::answerCall,
                    onRejectCall = viewModel::rejectCall,
                    onEndCall = viewModel::endCall,
                    onClearTelecomHistory = viewModel::clearTelecomHistory,
                    onTriggerTestLabAction = viewModel::applyTelecomTestLabAction,
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
    onSelectedEngineChange: (AssistantEngine) -> Unit,
    onSystemPromptChange: (String) -> Unit,
    onSilenceSuffixChange: (String) -> Unit,
    onAutoSpeakRepliesChange: (Boolean) -> Unit,
    onSelectLocalModel: () -> Unit,
    onInspectLocalEngineStatus: () -> Unit,
    onDownloadGemmaModel: () -> Unit,
    onAnswerCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit,
    onClearTelecomHistory: () -> Unit,
    onTriggerTestLabAction: (TelecomTestLabAction) -> Unit,
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
            Text("AI 전화 도우미", style = MaterialTheme.typography.headlineMedium)
            Text(
                "이 앱은 Android가 실제로 지원하는 범위 안에서 Telecom 역할, screening, STT/TTS 보조 흐름을 제공하는 정직한 통화 도우미입니다.",
                style = MaterialTheme.typography.bodyMedium,
            )

            StatusCard(
                title = "테스트 랩",
                body = "실제 carrier call 없이 앱 안에서 assistant, 로컬 모델 선택 상태, 테스트 전용 Telecom 이벤트를 점검합니다. 아래 Telecom 버튼은 [TEST ONLY] 기록만 남기며 실제 통화 처리에는 연결되지 않습니다.",
            ) {
                Text("테스트 전용 Telecom 상태", style = MaterialTheme.typography.titleMedium)
                Text(state.telecomSnapshot.testLabState.latestCallSummary)
                Text(state.telecomSnapshot.testLabState.latestScreeningSummary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onTriggerTestLabAction(TelecomTestLabAction.IncomingRinging) }) {
                        Text("가짜 수신")
                    }
                    Button(onClick = { onTriggerTestLabAction(TelecomTestLabAction.ConnectedActive) }) {
                        Text("가짜 연결")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onTriggerTestLabAction(TelecomTestLabAction.Ended) }) {
                        Text("가짜 종료")
                    }
                    Button(onClick = { onTriggerTestLabAction(TelecomTestLabAction.ScreeningAllow) }) {
                        Text("허용 판정")
                    }
                    Button(onClick = { onTriggerTestLabAction(TelecomTestLabAction.ScreeningSilence) }) {
                        Text("무음 판정")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onTriggerTestLabAction(TelecomTestLabAction.Reset) }) {
                        Text("테스트 초기화")
                    }
                }
                HorizontalDivider()
                Text("엔진 점검", style = MaterialTheme.typography.titleMedium)
                Text("선택 엔진: ${state.settings.selectedEngine.toKoreanLabel()}")
                Text("로컬 모델: ${state.settings.localModelLabel.ifBlank { "선택된 GGUF 모델 없음" }}")
                Text(state.localStatus, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onInspectLocalEngineStatus) {
                    Text("로컬 엔진 상태 점검")
                }
                TextButton(onClick = onDownloadGemmaModel) {
                    Text("Gemma 4 다운로드")
                }
                HorizontalDivider()
                Text("어시스턴트 흐름 검증", style = MaterialTheme.typography.titleMedium)
                Text(
                    "샘플 입력을 채운 뒤 응답 생성과 최근 응답 읽기를 바로 눌러, 실제 통화 없이 assistant 흐름을 검증할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onDraftChange(TEST_LAB_SAMPLE_CALLER_TEXT) }) {
                        Text("샘플 입력 채우기")
                    }
                    Button(onClick = onGenerateReply) {
                        Text("응답 생성")
                    }
                    Button(onClick = onSpeakReply, enabled = state.latestReply.isNotBlank()) {
                        Text("최근 응답 읽기")
                    }
                }
                Text(
                    "현재 테스트 입력: ${state.draftCallerText.ifBlank { "아직 없음" }}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "최근 응답 엔진: ${state.latestReplySource.toKoreanReplySource()}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(state.latestReply.ifBlank { "아직 생성된 응답이 없습니다." })
            }

            StatusCard(
                title = "Telecom 역할과 통화 상태",
                body = "기본 다이얼러 역할 보유: ${state.hasDialerRole}\n" +
                    "최근 통화 상태: ${state.telecomSnapshot.latestCallSummary}\n" +
                    "스크리닝: ${state.telecomSnapshot.screeningSummary}",
            ) {
                if (!state.hasDialerRole && state.canRequestDialerRole) {
                    Button(onClick = onRequestDialerRole) {
                        Text("기본 다이얼러 역할 요청")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAnswerCall, enabled = state.canAnswerCalls) {
                        Text("받기")
                    }
                    Button(onClick = onRejectCall, enabled = state.canAnswerCalls) {
                        Text("거절")
                    }
                    Button(onClick = onEndCall, enabled = state.canAnswerCalls) {
                        Text("끊기")
                    }
                }
                TextButton(onClick = onClearTelecomHistory, enabled = state.telecomSnapshot.recentEvents.isNotEmpty()) {
                    Text("기록 지우기")
                }
                if (state.telecomSnapshot.recentEvents.isEmpty()) {
                    Text("아직 Telecom 기록이 없습니다.")
                } else {
                    state.telecomSnapshot.recentEvents.forEach { entry ->
                        Text("${entry.timestampLabel} • ${entry.message}")
                    }
                }
            }

            StatusCard(
                title = "음성 보조 흐름",
                body = "음성 인식 가능: ${state.speechState.isRecognitionAvailable}\n" +
                    "마이크 권한: ${state.speechState.hasRecordAudioPermission}\n" +
                    "인식 오류: ${state.speechState.lastError.ifBlank { "없음" }}",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!state.speechState.hasRecordAudioPermission) {
                        Button(onClick = onRequestMicrophone) {
                            Text("마이크 권한 허용")
                        }
                    }
                    Button(onClick = onStartListening, enabled = !state.speechState.isListening) {
                        Text("STT 시작")
                    }
                    TextButton(onClick = onStopListening, enabled = state.speechState.isListening) {
                        Text("중지")
                    }
                }
                OutlinedTextField(
                    value = state.draftCallerText,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("상대 입력 / STT 전사문") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onGenerateReply) {
                        Text("응답 생성")
                    }
                    Button(onClick = onSpeakReply, enabled = state.latestReply.isNotBlank()) {
                        Text("응답 읽기")
                    }
                }
                Text(state.latestReplySource, style = MaterialTheme.typography.labelLarge)
                Text(state.latestReply.ifBlank { "아직 생성된 응답이 없습니다." })
                TextButton(onClick = onClearAssistantHistory, enabled = state.assistantHistory.isNotEmpty()) {
                    Text("어시스턴트 기록 지우기")
                }
                if (state.assistantHistory.isEmpty()) {
                    Text("아직 어시스턴트 기록이 없습니다.")
                } else {
                    state.assistantHistory.forEach { exchange ->
                        Text("${exchange.timestampLabel} • ${exchange.source} • 입력: ${exchange.callerText} • 응답: ${exchange.replyText}")
                    }
                }
            }

            StatusCard(
                title = "로컬 엔진 설정",
                body = state.localStatus,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistantEngine.entries.forEach { engine ->
                        FilterChip(
                            selected = state.settings.selectedEngine == engine,
                            onClick = { onSelectedEngineChange(engine) },
                            label = {
                                Text(
                                    when (engine) {
                                        AssistantEngine.Local -> "로컬"
                                        AssistantEngine.Demo -> "데모"
                                    }
                                )
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = state.settings.systemPrompt,
                    onValueChange = onSystemPromptChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("시스템 프롬프트") },
                )
                OutlinedTextField(
                    value = state.settings.silenceNumberSuffix,
                    onValueChange = onSilenceSuffixChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("무음 스크리닝 접미사") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(
                        checked = state.settings.autoSpeakReplies,
                        onCheckedChange = onAutoSpeakRepliesChange,
                    )
                    Text("새 응답을 자동으로 읽기")
                }
                Button(onClick = onSelectLocalModel) {
                    Text("GGUF 모델 선택")
                }
                Text(
                    state.settings.localModelLabel.ifBlank { "아직 선택된 로컬 GGUF 모델이 없습니다." },
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "현재 앱은 로컬 Gemma 모델과 데모 엔진만 사용합니다. Gemma 4 모델은 앱 안에서 직접 다운로드할 수 있습니다.",
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

private fun AssistantEngine.toKoreanLabel(): String = when (this) {
    AssistantEngine.Local -> "로컬"
    AssistantEngine.Demo -> "데모"
}

private fun String.toKoreanReplySource(): String = when {
    isBlank() || this == "아직 생성된 응답이 없습니다." -> "아직 없음"
    contains("Local") || contains("로컬") -> "로컬"
    contains("Demo") || contains("데모") -> "데모"
    else -> this
}
