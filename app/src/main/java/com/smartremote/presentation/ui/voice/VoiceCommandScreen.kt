package com.smartremote.presentation.ui.voice

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.smartremote.data.repository.DeviceRepository
import com.smartremote.domain.model.*
import com.smartremote.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class VoiceUiState(
    val isListening: Boolean = false,
    val transcribedText: String = "",
    val resultText: String = "",
    val isProcessing: Boolean = false,
    val recentCommands: List<String> = emptyList(),
    val devices: List<Device> = emptyList()
)

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            deviceRepo.getAllDevices().collect { devices -> _uiState.update { it.copy(devices = devices) } }
        }
        _uiState.update {
            it.copy(recentCommands = listOf(
                "Turn on the TV", "Set AC to 24 degrees",
                "Turn off all lights", "Activate Movie Mode", "What's the temperature?"
            ))
        }
    }

    fun startListening() = _uiState.update { it.copy(isListening = true, transcribedText = "", resultText = "") }
    fun stopListening() = _uiState.update { it.copy(isListening = false) }

    fun processCommand(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isListening = false, transcribedText = text, isProcessing = true) }
            delay(800)
            val result = parseVoiceCommand(text)
            _uiState.update { it.copy(isProcessing = false, resultText = result) }
        }
    }

    private suspend fun parseVoiceCommand(text: String): String {
        val lower = text.lowercase()
        val devices = _uiState.value.devices

        return when {
            lower.contains("turn on") || lower.contains("switch on") -> {
                val target = findDeviceInText(lower, devices)
                if (target != null) {
                    deviceRepo.sendCommand(target, "POWER_ON")
                    "✅ Turning on ${target.name}"
                } else "🔍 No matching device found. Try saying the device name clearly."
            }
            lower.contains("turn off") || lower.contains("switch off") -> {
                val target = findDeviceInText(lower, devices)
                if (target != null) {
                    deviceRepo.sendCommand(target, "POWER_OFF")
                    "✅ Turning off ${target.name}"
                } else if (lower.contains("all")) {
                    devices.forEach { deviceRepo.sendCommand(it, "POWER_OFF") }
                    "✅ Turning off all ${devices.size} devices"
                } else "🔍 No matching device found."
            }
            lower.contains("temperature") || lower.contains("degrees") || lower.contains("ac") -> {
                val tempRegex = Regex("(\\d+)")
                val temp = tempRegex.find(lower)?.value
                val ac = devices.firstOrNull { it.type == DeviceType.AIR_CONDITIONER }
                if (ac != null && temp != null) {
                    deviceRepo.sendCommand(ac, "SET_TEMPERATURE", temp)
                    "✅ Setting AC temperature to ${temp}°C"
                } else if (ac != null) "✅ AC found — please specify the temperature"
                else "❌ No AC device found. Add one in Device Manager."
            }
            lower.contains("volume") -> {
                val tv = devices.firstOrNull { it.type == DeviceType.TV }
                val action = if (lower.contains("up") || lower.contains("increase")) "VOLUME_UP" else "VOLUME_DOWN"
                if (tv != null) { deviceRepo.sendCommand(tv, action); "✅ ${if (action == "VOLUME_UP") "Increasing" else "Decreasing"} TV volume" }
                else "❌ No TV found."
            }
            lower.contains("movie mode") -> "🎬 Activating Movie Mode — dimming lights and turning on TV"
            lower.contains("sleep mode") -> "💤 Sleep Mode activated — AC at 26°C, lights off"
            lower.contains("good morning") -> "🌅 Good morning! Turning on lights and AC"
            else -> "🤔 I didn't understand that. Try commands like 'Turn on the TV' or 'Set AC to 24 degrees'"
        }
    }

    private fun findDeviceInText(text: String, devices: List<Device>): Device? =
        devices.firstOrNull { device ->
            text.contains(device.name.lowercase()) ||
            text.contains(device.type.displayName.lowercase()) ||
            (device.type == DeviceType.TV && text.contains("tv")) ||
            (device.type == DeviceType.AIR_CONDITIONER && (text.contains("ac") || text.contains("air")))
        }
}

// ─── Voice Command Screen ─────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandScreen(
    onBack: () -> Unit,
    viewModel: VoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Control", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // Mic button with pulse animation
            VoiceMicButton(
                isListening = uiState.isListening,
                isProcessing = uiState.isProcessing,
                onClick = {
                    if (!micPermission.status.isGranted) {
                        micPermission.launchPermissionRequest()
                    } else if (uiState.isListening) {
                        viewModel.processCommand("Turn on the TV") // Demo
                    } else {
                        viewModel.startListening()
                    }
                }
            )

            // Status text
            AnimatedContent(targetState = when {
                uiState.isProcessing  -> "Processing..."
                uiState.isListening   -> "Listening..."
                !micPermission.status.isGranted -> "Tap to grant microphone permission"
                else -> "Tap to speak"
            }, label = "status") { status ->
                Text(status, style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.isListening) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium)
            }

            // Transcribed text
            AnimatedVisibility(uiState.transcribedText.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.primary)
                        Text("\"${uiState.transcribedText}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Result
            AnimatedVisibility(uiState.resultText.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(uiState.resultText, Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge)
                }
            }

            Divider()

            // Example commands
            Text("Try saying...", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start))

            uiState.recentCommands.forEach { cmd ->
                OutlinedCard(onClick = { viewModel.processCommand(cmd) },
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp, 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Text("\"$cmd\"", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// ─── Mic Button with Pulse ────────────────────────────────────────────────────

@Composable
fun VoiceMicButton(isListening: Boolean, isProcessing: Boolean, onClick: () -> Unit) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scale"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        if (isListening) {
            Box(Modifier.size(160.dp).scale(pulseScale).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)))
            Box(Modifier.size(130.dp).scale(pulseScale * 0.9f).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)))
        }
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(100.dp),
            containerColor = if (isListening) MaterialTheme.colorScheme.error
                             else MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            if (isProcessing) {
                CircularProgressIndicator(Modifier.size(36.dp), color = Color.White, strokeWidth = 3.dp)
            } else {
                Icon(if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    "Mic", Modifier.size(40.dp), tint = Color.White)
            }
        }
    }
}

fun Modifier.scale(scale: Float) = this.graphicsLayer(scaleX = scale, scaleY = scale)
