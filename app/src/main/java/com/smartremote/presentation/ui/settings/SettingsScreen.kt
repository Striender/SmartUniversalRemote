package com.smartremote.presentation.ui.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.smartremote.data.repository.RoomRepository
import com.smartremote.domain.model.Room
import com.smartremote.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val userName: String = "",
    val email: String = "",
    val darkTheme: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val energyAlerts: Boolean = true,
    val autoDiscovery: Boolean = true,
    val rooms: List<Room> = emptyList(),
    val showAddRoom: Boolean = false,
    val appVersion: String = "1.0.0"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val roomRepo: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val user = auth.currentUser
        _uiState.update {
            it.copy(
                userName = user?.displayName ?: "Guest User",
                email = user?.email ?: "guest@smartremote.app"
            )
        }
        viewModelScope.launch {
            roomRepo.getAllRooms().collect { rooms -> _uiState.update { it.copy(rooms = rooms) } }
        }
    }

    fun toggleDarkTheme() = _uiState.update { it.copy(darkTheme = !it.darkTheme) }
    fun toggleNotifications() = _uiState.update { it.copy(notificationsEnabled = !it.notificationsEnabled) }
    fun toggleBiometric() = _uiState.update { it.copy(biometricEnabled = !it.biometricEnabled) }
    fun toggleEnergyAlerts() = _uiState.update { it.copy(energyAlerts = !it.energyAlerts) }
    fun toggleAutoDiscovery() = _uiState.update { it.copy(autoDiscovery = !it.autoDiscovery) }
    fun showAddRoom(show: Boolean) = _uiState.update { it.copy(showAddRoom = show) }

    fun addRoom(name: String) {
        viewModelScope.launch {
            roomRepo.addRoom(Room(id = UUID.randomUUID().toString(), name = name))
            _uiState.update { it.copy(showAddRoom = false) }
        }
    }

    fun deleteRoom(room: Room) {
        viewModelScope.launch { roomRepo.deleteRoom(room.id) }
    }

    fun signOut(onDone: () -> Unit) {
        auth.signOut()
        onDone()
    }
}

// ─── Settings Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = { viewModel.signOut(onBack) }) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") } }
        )
    }

    if (uiState.showAddRoom) {
        AlertDialog(
            onDismissRequest = { viewModel.showAddRoom(false) },
            title = { Text("Add Room") },
            text = {
                OutlinedTextField(value = newRoomName, onValueChange = { newRoomName = it },
                    label = { Text("Room Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp))
            },
            confirmButton = {
                Button(onClick = { viewModel.addRoom(newRoomName); newRoomName = "" }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { viewModel.showAddRoom(false) }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile card
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center) {
                        Text(uiState.userName.firstOrNull()?.uppercase() ?: "G",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(uiState.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(uiState.email, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }

            // Appearance
            SettingsSection("Appearance") {
                SettingsToggleRow("Dark Theme", "Switch between dark and light mode",
                    Icons.Default.DarkMode, uiState.darkTheme, viewModel::toggleDarkTheme)
            }

            // Security
            SettingsSection("Security") {
                SettingsToggleRow("Biometric Lock", "Use fingerprint or face unlock",
                    Icons.Default.Fingerprint, uiState.biometricEnabled, viewModel::toggleBiometric)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsNavRow("Change PIN", "Update your app PIN", Icons.Default.Pin) { }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsNavRow("Child Lock", "Restrict access to settings", Icons.Default.ChildCare) { }
            }

            // Notifications
            SettingsSection("Notifications") {
                SettingsToggleRow("Push Notifications", "Device alerts and reminders",
                    Icons.Default.Notifications, uiState.notificationsEnabled, viewModel::toggleNotifications)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsToggleRow("Energy Alerts", "Get notified of high usage",
                    Icons.Default.Bolt, uiState.energyAlerts, viewModel::toggleEnergyAlerts)
            }

            // Devices
            SettingsSection("Devices & Network") {
                SettingsToggleRow("Auto-Discovery", "Automatically find new devices",
                    Icons.Default.Search, uiState.autoDiscovery, viewModel::toggleAutoDiscovery)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsNavRow("MQTT Settings", "Configure MQTT broker", Icons.Default.Cloud) { }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsNavRow("IR Library", "Browse IR codes database", Icons.Default.SettingsRemote) { }
            }

            // Rooms
            SettingsSection("Rooms") {
                uiState.rooms.forEach { room ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HomeWork, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(room.name, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { viewModel.deleteRoom(room) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                TextButton(onClick = { viewModel.showAddRoom(true) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Room")
                }
            }

            // Cloud
            SettingsSection("Cloud & Backup") {
                SettingsNavRow("Backup Settings", "Save to Firebase cloud", Icons.Default.CloudUpload) { }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsNavRow("Restore Settings", "Restore from backup", Icons.Default.CloudDownload) { }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsNavRow("Sync Devices", "Sync across multiple phones", Icons.Default.Sync) { }
            }

            // About
            SettingsSection("About") {
                SettingsInfoRow("App Version", uiState.appVersion, Icons.Default.Info)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsNavRow("Privacy Policy", "", Icons.Default.Policy) { }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingsNavRow("Terms of Service", "", Icons.Default.Gavel) { }
            }

            // Sign out
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
                TextButton(onClick = { showSignOutDialog = true }, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Settings Components ──────────────────────────────────────────────────────

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column { content() }
        }
    }
}

@Composable
fun SettingsToggleRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
fun SettingsNavRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsInfoRow(title: String, value: String, icon: ImageVector) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
