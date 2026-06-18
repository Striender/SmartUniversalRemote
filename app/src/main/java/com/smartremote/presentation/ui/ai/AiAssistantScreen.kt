package com.smartremote.presentation.ui.ai

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.smartremote.BuildConfig
import com.smartremote.data.local.dao.AiMessageDao
import com.smartremote.data.local.entities.AiMessageEntity
import com.smartremote.data.remote.api.AnthropicApiService
import com.smartremote.data.remote.api.AnthropicMessage
import com.smartremote.data.remote.api.AnthropicRequest
import com.smartremote.data.repository.DeviceRepository
import com.smartremote.domain.model.AiMessage
import com.smartremote.domain.model.Device
import com.smartremote.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── AI ViewModel ─────────────────────────────────────────────────────────────

data class AiUiState(
    val messages: List<AiMessage> = emptyList(),
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = ""
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val anthropicApi: AnthropicApiService,
    private val aiMessageDao: AiMessageDao,
    private val deviceRepo: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val conversationHistory = mutableListOf<AnthropicMessage>()

    init {
        viewModelScope.launch {
            aiMessageDao.getAllMessages().collect { entities ->
                val messages = entities.map {
                    AiMessage(it.id, it.content, it.isFromUser, it.timestamp, it.suggestedActions)
                }
                _uiState.update { s -> s.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            deviceRepo.getAllDevices().collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
    }

    fun updateInput(text: String) = _uiState.update { it.copy(inputText = text) }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            // Add user message
            val userMsg = AiMessage(content = text, isFromUser = true)
            aiMessageDao.insertMessage(userMsg.toEntity())
            _uiState.update { it.copy(inputText = "", isLoading = true) }

            conversationHistory.add(AnthropicMessage(role = "user", content = text))

            try {
                val deviceContext = _uiState.value.devices.joinToString(", ") {
                    "${it.name} (${it.type.displayName}, ${if (it.isOnline) "Online" else "Offline"})"
                }

                val systemPrompt = """
                    You are a smart home AI assistant for the Smart Universal Remote app.
                    The user has these devices: $deviceContext.
                    Help them control devices, create automations, understand energy usage, and optimize their smart home.
                    When the user asks to control a device, respond with what you'd do and end with a JSON action block:
                    {"action": "command", "device": "device name", "command": "COMMAND", "value": "optional"}
                    Keep responses concise and helpful. Suggest energy-saving tips when relevant.
                """.trimIndent()

                val response = anthropicApi.createMessage(
                    apiKey = BuildConfig.ANTHROPIC_API_KEY,
                    body = AnthropicRequest(
                        system = systemPrompt,
                        messages = conversationHistory
                    )
                )

                if (response.isSuccessful) {
                    val reply = response.body()?.content?.firstOrNull()?.text ?: "I couldn't process that."
                    val aiMsg = AiMessage(content = reply, isFromUser = false)
                    aiMessageDao.insertMessage(aiMsg.toEntity())
                    conversationHistory.add(AnthropicMessage(role = "assistant", content = reply))

                    // Parse and execute action if present
                    parseAndExecuteAction(reply)
                } else {
                    val errMsg = AiMessage(
                        content = "I'm having trouble connecting right now. Please try again.",
                        isFromUser = false
                    )
                    aiMessageDao.insertMessage(errMsg.toEntity())
                }
            } catch (e: Exception) {
                val errMsg = AiMessage(content = "Error: ${e.message}", isFromUser = false)
                aiMessageDao.insertMessage(errMsg.toEntity())
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun parseAndExecuteAction(reply: String) {
        // Simple JSON extraction from reply
        val jsonRegex = Regex("""\{"action":\s*"command".*?\}""", RegexOption.DOT_MATCHES_ALL)
        val match = jsonRegex.find(reply) ?: return
        // Execute the command on the matched device
    }

    fun clearChat() {
        viewModelScope.launch {
            aiMessageDao.clearAllMessages()
            conversationHistory.clear()
        }
    }

    private fun AiMessage.toEntity() = AiMessageEntity(
        id = id, content = content, isFromUser = isFromUser,
        timestamp = timestamp, suggestedActions = suggestedActions
    )
}

// ─── AI Assistant Screen ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onBack: () -> Unit,
    viewModel: AiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape)
                                .background(
                                    Brush.radialGradient(listOf(Brand400, Brand700))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, null, Modifier.size(20.dp), tint = Color.White)
                        }
                        Column {
                            Text("AI Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Powered by Claude", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.DeleteSweep, "Clear", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = {
            AiInputBar(
                text = uiState.inputText,
                isLoading = uiState.isLoading,
                onTextChange = { viewModel.updateInput(it) },
                onSend = { viewModel.sendMessage() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.messages.isEmpty()) {
            AiWelcomeView(Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    AiMessageBubble(message)
                }
                if (uiState.isLoading) {
                    item { TypingIndicator() }
                }
            }
        }
    }
}

// ─── AI Welcome View ──────────────────────────────────────────────────────────

@Composable
fun AiWelcomeView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(80.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Brand400, Brand700))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SmartToy, null, Modifier.size(44.dp), tint = Color.White)
        }
        Spacer(Modifier.height(20.dp))
        Text("Smart Home AI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ask me anything about your smart home. I can control devices, create automations, and help you save energy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        // Quick prompts
        val prompts = listOf("Turn on the TV", "Set AC to 24°C", "Activate Movie Mode", "How much energy did I use today?")
        prompts.forEach { prompt ->
            SuggestionChip(
                onClick = { },
                label = { Text(prompt) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

// ─── Message Bubble ───────────────────────────────────────────────────────────

@Composable
fun AiMessageBubble(message: AiMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            Box(
                Modifier.size(32.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Brand400, Brand700))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, null, Modifier.size(18.dp), tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
        }

        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isFromUser) 20.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 20.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isFromUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Typing Indicator ─────────────────────────────────────────────────────────

@Composable
fun TypingIndicator() {
    val dots = remember { mutableListOf(false, false, false) }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(Modifier.padding(12.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                }
            }
        }
    }
}

// ─── Input Bar ────────────────────────────────────────────────────────────────

@Composable
fun AiInputBar(
    text: String,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp, 8.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about your smart home…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            FilledIconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !isLoading,
                modifier = Modifier.size(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Send, "Send")
                }
            }
        }
    }
}
