package com.smartremote.presentation.ui.devices

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.smartremote.data.local.dao.CommandHistoryDao
import com.smartremote.data.repository.DeviceRepository
import com.smartremote.domain.model.*
import com.smartremote.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class DeviceDetailUiState(
    val device: Device? = null,
    val recentCommands: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val showDeleteDialog: Boolean = false
)

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val commandHistoryDao: CommandHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceDetailUiState())
    val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

    fun loadDevice(deviceId: String) {
        viewModelScope.launch {
            val device = deviceRepo.getDeviceById(deviceId)
            _uiState.update { it.copy(device = device) }
            commandHistoryDao.getMostUsedCommands(deviceId).let { cmds ->
                _uiState.update { it.copy(recentCommands = cmds.map { c -> c.command }) }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val d = _uiState.value.device ?: return@launch
            deviceRepo.toggleFavorite(d.id, !d.isFavorite)
            _uiState.update { it.copy(device = d.copy(isFavorite = !d.isFavorite)) }
        }
    }

    fun deleteDevice(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value.device?.let { deviceRepo.deleteDevice(it.id) }
            onDone()
        }
    }

    fun showDeleteDialog(show: Boolean) = _uiState.update { it.copy(showDeleteDialog = show) }
}

// ─── Device Detail Screen ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: String,
    onBack: () -> Unit,
    onOpenRemote: (String) -> Unit,
    viewModel: DeviceDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(deviceId) { viewModel.loadDevice(deviceId) }

    val device = uiState.device
    val deviceColor = DeviceColors[device?.type?.name] ?: Brand500

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            title = { Text("Delete Device") },
            text = { Text("Are you sure you want to remove ${device?.name}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteDevice(onBack) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.showDeleteDialog(false) }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device?.name ?: "Device", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(if (device?.isFavorite == true) Icons.Default.Star else Icons.Default.StarBorder,
                            "Favorite", tint = if (device?.isFavorite == true) Color(0xFFFBBF24) else LocalContentColor.current)
                    }
                    IconButton(onClick = { viewModel.showDeleteDialog(true) }) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = deviceColor.copy(alpha = 0.12f))
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(60.dp).clip(RoundedCornerShape(16.dp))
                            .background(deviceColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Devices, null, Modifier.size(32.dp), tint = deviceColor)
                        }
                        Column {
                            Text(device?.name ?: "", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(device?.type?.displayName ?: "", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(isOnline = device?.status == DeviceStatus.ONLINE)
                        device?.brand?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                        device?.connectionType?.let {
                            AssistChip(onClick = {}, label = { Text(it.displayName) },
                                leadingIcon = { Icon(connectionIcon(it), null, Modifier.size(16.dp)) })
                        }
                    }
                }
            }

            // Open Remote button
            Button(
                onClick = { device?.id?.let { onOpenRemote(it) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.SettingsRemote, null)
                Spacer(Modifier.width(8.dp))
                Text("Open Remote Control", fontWeight = FontWeight.Bold)
            }

            // Info section
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Device Info", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()
                    InfoRow("Type", device?.type?.displayName ?: "-")
                    InfoRow("Connection", device?.connectionType?.displayName ?: "-")
                    device?.ipAddress?.let { InfoRow("IP Address", it) }
                    device?.mqttTopic?.let { InfoRow("MQTT Topic", it) }
                    device?.roomName?.let { InfoRow("Room", it) }
                    InfoRow("Last Seen", if (device?.lastSeen != null && device.lastSeen > 0)
                        java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(device.lastSeen))
                    else "Never")
                }
            }

            // Capabilities
            if (device?.capabilities?.isNotEmpty() == true) {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Capabilities", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            device.capabilities.forEach { cap ->
                                SuggestionChip(onClick = {}, label = { Text(cap.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                }
            }

            // Recent commands
            if (uiState.recentCommands.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Most Used Commands", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        uiState.recentCommands.forEach { cmd ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.History, null, Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(cmd.replace("_", " "), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StatusChip(isOnline: Boolean) {
    SuggestionChip(
        onClick = {},
        label = { Text(if (isOnline) "Online" else "Offline", style = MaterialTheme.typography.labelSmall) },
        icon = {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (isOnline) GreenOk else RedAlert))
        }
    )
}
