package com.smartremote.presentation.ui.automation

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.smartremote.data.repository.DeviceRepository
import com.smartremote.data.repository.SceneRepository
import com.smartremote.domain.model.*
import com.smartremote.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class AutomationUiState(
    val scenes: List<Scene> = emptyList(),
    val devices: List<Device> = emptyList(),
    val selectedTab: Int = 0,
    val showAddScene: Boolean = false
)

@HiltViewModel
class AutomationViewModel @Inject constructor(
    private val sceneRepo: SceneRepository,
    private val deviceRepo: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AutomationUiState())
    val uiState: StateFlow<AutomationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(sceneRepo.getAllScenes(), deviceRepo.getAllDevices()) { scenes, devices ->
                scenes to devices
            }.collect { (scenes, devices) ->
                _uiState.update { it.copy(scenes = scenes, devices = devices) }
            }
        }
    }

    fun selectTab(tab: Int) = _uiState.update { it.copy(selectedTab = tab) }
    fun showAddScene(show: Boolean) = _uiState.update { it.copy(showAddScene = show) }

    fun activateScene(scene: Scene) {
        viewModelScope.launch {
            scene.actions.forEach { action ->
                val device = deviceRepo.getDeviceById(action.deviceId) ?: return@forEach
                kotlinx.coroutines.delay(action.delayMs)
                deviceRepo.sendCommand(device, action.command, action.value)
            }
            sceneRepo.activateScene(scene.id)
        }
    }

    fun deleteScene(scene: Scene) {
        viewModelScope.launch { sceneRepo.deleteScene(scene) }
    }

    fun addQuickScene(name: String, icon: String, color: Long, actions: List<SceneAction>) {
        viewModelScope.launch {
            val scene = Scene(name = name, icon = icon, color = color.toInt(), actions = actions)
            sceneRepo.addScene(scene)
            _uiState.update { it.copy(showAddScene = false) }
        }
    }
}

// ─── Automation Screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    onBack: () -> Unit,
    viewModel: AutomationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showAddScene) {
        AddSceneSheet(
            devices = uiState.devices,
            onDismiss = { viewModel.showAddScene(false) },
            onSave = { name, icon, color, actions -> viewModel.addQuickScene(name, icon, color, actions) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automation", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddScene(true) }) {
                Icon(Icons.Default.Add, "Add Scene")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = uiState.selectedTab) {
                listOf("Scenes", "Schedules", "Routines").forEachIndexed { i, title ->
                    Tab(selected = uiState.selectedTab == i, onClick = { viewModel.selectTab(i) },
                        text = { Text(title) })
                }
            }

            AnimatedContent(targetState = uiState.selectedTab, label = "tab") { tab ->
                when (tab) {
                    0 -> ScenesTab(uiState.scenes, viewModel::activateScene, viewModel::deleteScene)
                    1 -> SchedulesTab()
                    else -> RoutinesTab(onActivate = { viewModel.activateScene(it) })
                }
            }
        }
    }
}

// ─── Scenes Tab ───────────────────────────────────────────────────────────────

@Composable
fun ScenesTab(scenes: List<Scene>, onActivate: (Scene) -> Unit, onDelete: (Scene) -> Unit) {
    if (scenes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                Text("No scenes yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Tap + to create your first scene", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(scenes, key = { it.id }) { scene ->
            SceneCard(scene = scene, onActivate = { onActivate(scene) }, onDelete = { onDelete(scene) })
        }
    }
}

@Composable
fun SceneCard(scene: Scene, onActivate: () -> Unit, onDelete: () -> Unit) {
    val color = Color(scene.color)
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = if (scene.isActive) BorderStroke(1.5.dp, color) else null) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(24.dp), tint = color)
                }
                Column {
                    Text(scene.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${scene.actions.size} action${if (scene.actions.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row {
                FilledTonalButton(onClick = onActivate, contentPadding = PaddingValues(horizontal = 14.dp)) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Run")
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { showMenu = false; onDelete() })
                    }
                }
            }
        }
    }
}

// ─── Schedules Tab ────────────────────────────────────────────────────────────

@Composable
fun SchedulesTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Schedule, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(0.4f))
            Text("Schedule Automations", style = MaterialTheme.typography.titleMedium)
            Text("Set timers and recurring actions\nfor your devices", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            FilledTonalButton(onClick = {}) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add Schedule") }
        }
    }
}

// ─── Routines Tab ─────────────────────────────────────────────────────────────

@Composable
fun RoutinesTab(onActivate: (Scene) -> Unit) {
    val routines = listOf(
        Triple("Movie Mode", 0xFF7C3AED, listOf("Dim lights", "Turn on TV", "Set AC to 23°C")),
        Triple("Sleep Mode", 0xFF1D4ED8, listOf("Turn off all lights", "Set AC to 26°C", "Activate fan")),
        Triple("Away Mode", 0xFFB45309, listOf("Turn off all devices", "Lock doors", "Arm security")),
        Triple("Morning", 0xFFD97706, listOf("Turn on lights", "Set AC to 25°C", "Turn on TV")),
        Triple("Energy Saver", 0xFF059669, listOf("Reduce AC temp", "Dim lights to 50%", "Turn off idle devices"))
    )

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(routines) { (name, colorLong, actions) ->
            val color = Color(colorLong)
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(0.2f)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(24.dp), tint = color)
                        }
                        Column {
                            Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            actions.take(2).forEach {
                                Text("• $it", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    FilledTonalButton(onClick = {}) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("Run")
                    }
                }
            }
        }
    }
}

// ─── Add Scene Bottom Sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSceneSheet(
    devices: List<Device>,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, List<SceneAction>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("New Scene", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Scene Name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, "auto_awesome", 0xFF6366F1, emptyList()) },
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)
            ) { Text("Create Scene", fontWeight = FontWeight.Bold) }
        }
    }
}
