package com.smartremote.presentation.ui.devices

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import com.smartremote.data.repository.DeviceRepository
import com.smartremote.data.repository.RoomRepository
import com.smartremote.domain.model.*
import com.smartremote.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class AddDeviceUiState(
    val step: Int = 0,
    val selectedType: DeviceType? = null,
    val selectedConnection: ConnectionType? = null,
    val deviceName: String = "",
    val brand: String = "",
    val ipAddress: String = "",
    val mqttTopic: String = "",
    val selectedRoomId: String? = null,
    val rooms: List<Room> = emptyList(),
    val discoveredDevices: List<Device> = emptyList(),
    val isDiscovering: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val roomRepo: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            roomRepo.getAllRooms().collect { rooms ->
                _uiState.update { it.copy(rooms = rooms) }
            }
        }
    }

    fun selectType(type: DeviceType) = _uiState.update { it.copy(selectedType = type, step = 1) }
    fun selectConnection(c: ConnectionType) = _uiState.update { it.copy(selectedConnection = c, step = 2) }
    fun updateName(v: String) = _uiState.update { it.copy(deviceName = v) }
    fun updateBrand(v: String) = _uiState.update { it.copy(brand = v) }
    fun updateIp(v: String) = _uiState.update { it.copy(ipAddress = v) }
    fun updateMqtt(v: String) = _uiState.update { it.copy(mqttTopic = v) }
    fun selectRoom(id: String?) = _uiState.update { it.copy(selectedRoomId = id) }
    fun goBack() = _uiState.update { if (it.step > 0) it.copy(step = it.step - 1) else it }

    fun discoverDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true) }
            when (val result = deviceRepo.discoverDevices()) {
                is Result.Success -> _uiState.update { it.copy(discoveredDevices = result.data, isDiscovering = false) }
                is Result.Error   -> _uiState.update { it.copy(isDiscovering = false, error = result.message) }
                else -> _uiState.update { it.copy(isDiscovering = false) }
            }
        }
    }

    fun saveDevice(onSuccess: () -> Unit) {
        val s = _uiState.value
        if (s.deviceName.isBlank()) { _uiState.update { it.copy(error = "Please enter a device name") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val device = Device(
                name = s.deviceName,
                type = s.selectedType ?: DeviceType.OTHER,
                connectionType = s.selectedConnection ?: ConnectionType.WIFI,
                roomId = s.selectedRoomId,
                brand = s.brand.ifBlank { null },
                ipAddress = s.ipAddress.ifBlank { null },
                mqttTopic = s.mqttTopic.ifBlank { null },
                capabilities = defaultCapabilities(s.selectedType)
            )
            deviceRepo.addDevice(device)
            _uiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }

    private fun defaultCapabilities(type: DeviceType?) = when (type) {
        DeviceType.TV -> listOf(DeviceCapability.POWER, DeviceCapability.VOLUME, DeviceCapability.CHANNEL)
        DeviceType.AIR_CONDITIONER -> listOf(DeviceCapability.POWER, DeviceCapability.TEMPERATURE, DeviceCapability.FAN_SPEED, DeviceCapability.TIMER)
        DeviceType.FAN -> listOf(DeviceCapability.POWER, DeviceCapability.FAN_SPEED, DeviceCapability.TIMER)
        DeviceType.SMART_LIGHT -> listOf(DeviceCapability.POWER, DeviceCapability.BRIGHTNESS, DeviceCapability.COLOR)
        else -> listOf(DeviceCapability.POWER)
    }
}

// ─── Add Device Screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    onBack: () -> Unit,
    viewModel: AddDeviceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Add Device", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            listOf("Choose Type", "Connection", "Details")[uiState.step.coerceAtMost(2)],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (uiState.step == 0) onBack() else viewModel.goBack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Step indicator
            StepIndicator(currentStep = uiState.step, totalSteps = 3, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            AnimatedContent(targetState = uiState.step, label = "step") { step ->
                when (step) {
                    0 -> DeviceTypeStep(onSelect = viewModel::selectType)
                    1 -> ConnectionTypeStep(onSelect = viewModel::selectConnection, onDiscover = viewModel::discoverDevices, uiState = uiState)
                    else -> DeviceDetailsStep(uiState = uiState, viewModel = viewModel, onSave = { viewModel.saveDevice(onBack) })
                }
            }
        }
    }
}

// ─── Step Indicator ───────────────────────────────────────────────────────────

@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalSteps) { i ->
            Box(
                Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

// ─── Step 1: Device Type ──────────────────────────────────────────────────────

@Composable
fun DeviceTypeStep(onSelect: (DeviceType) -> Unit) {
    val types = DeviceType.values().toList()
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(3) }) {
            Text("What type of device are you adding?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp))
        }
        items(types) { type ->
            val color = DeviceColors[type.name] ?: Brand500
            Card(
                onClick = { onSelect(type) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Devices, null, Modifier.size(32.dp), tint = color)
                    Text(type.displayName, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

// ─── Step 2: Connection Type ──────────────────────────────────────────────────

@Composable
fun ConnectionTypeStep(
    onSelect: (ConnectionType) -> Unit,
    onDiscover: () -> Unit,
    uiState: AddDeviceUiState
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("How does it connect?", style = MaterialTheme.typography.titleMedium)

        ConnectionType.values().forEach { conn ->
            Card(
                onClick = { onSelect(conn) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(connectionIcon(conn), null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(conn.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(connectionDesc(conn), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onDiscover, modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isDiscovering) {
            if (uiState.isDiscovering) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.width(8.dp))
            Text("Auto-Discover Devices")
        }

        if (uiState.discoveredDevices.isNotEmpty()) {
            Text("Discovered Devices", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            uiState.discoveredDevices.forEach { device ->
                Card(onClick = { onSelect(device.connectionType) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DevicesOther, null, tint = GreenOk)
                        Column {
                            Text(device.name, style = MaterialTheme.typography.titleSmall)
                            Text(device.ipAddress ?: "", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

fun connectionIcon(c: ConnectionType) = when (c) {
    ConnectionType.IR_BLASTER -> Icons.Default.SettingsRemote
    ConnectionType.WIFI       -> Icons.Default.Wifi
    ConnectionType.BLUETOOTH  -> Icons.Default.Bluetooth
    ConnectionType.BLE        -> Icons.Default.BluetoothSearching
    ConnectionType.MQTT       -> Icons.Default.Cloud
    ConnectionType.ESP32      -> Icons.Default.Memory
    ConnectionType.ZIGBEE     -> Icons.Default.Hub
    ConnectionType.Z_WAVE     -> Icons.Default.Waves
}

fun connectionDesc(c: ConnectionType) = when (c) {
    ConnectionType.IR_BLASTER -> "Control via infrared signal (TV, AC, Fan)"
    ConnectionType.WIFI       -> "Connect over your home Wi-Fi network"
    ConnectionType.BLUETOOTH  -> "Pair via Bluetooth (up to 10m)"
    ConnectionType.BLE        -> "Bluetooth Low Energy for smart devices"
    ConnectionType.MQTT       -> "IoT protocol for smart home hubs"
    ConnectionType.ESP32      -> "DIY ESP32/ESP8266 smart devices"
    ConnectionType.ZIGBEE     -> "Zigbee mesh network protocol"
    ConnectionType.Z_WAVE     -> "Z-Wave mesh network protocol"
}

// ─── Step 3: Device Details ───────────────────────────────────────────────────

@Composable
fun DeviceDetailsStep(
    uiState: AddDeviceUiState,
    viewModel: AddDeviceViewModel,
    onSave: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {

        Text("Device Details", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(value = uiState.deviceName, onValueChange = viewModel::updateName,
            label = { Text("Device Name *") }, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Label, null) }, singleLine = true,
            isError = uiState.error != null && uiState.deviceName.isBlank(),
            shape = RoundedCornerShape(14.dp))

        OutlinedTextField(value = uiState.brand, onValueChange = viewModel::updateBrand,
            label = { Text("Brand (e.g. Samsung, LG)") }, modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Business, null) }, singleLine = true,
            shape = RoundedCornerShape(14.dp))

        if (uiState.selectedConnection in listOf(ConnectionType.WIFI, ConnectionType.ESP32)) {
            OutlinedTextField(value = uiState.ipAddress, onValueChange = viewModel::updateIp,
                label = { Text("IP Address") }, modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Router, null) }, singleLine = true,
                shape = RoundedCornerShape(14.dp))
        }

        if (uiState.selectedConnection == ConnectionType.MQTT) {
            OutlinedTextField(value = uiState.mqttTopic, onValueChange = viewModel::updateMqtt,
                label = { Text("MQTT Topic") }, modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Topic, null) }, singleLine = true,
                shape = RoundedCornerShape(14.dp))
        }

        // Room picker
        Text("Assign to Room", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            item {
                FilterChip(selected = uiState.selectedRoomId == null,
                    onClick = { viewModel.selectRoom(null) }, label = { Text("None") })
            }
            items(uiState.rooms) { room ->
                FilterChip(selected = uiState.selectedRoomId == room.id,
                    onClick = { viewModel.selectRoom(room.id) }, label = { Text(room.name) })
            }
        }

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !uiState.isSaving
        ) {
            if (uiState.isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else { Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text("Add Device", fontWeight = FontWeight.Bold) }
        }
    }
}
