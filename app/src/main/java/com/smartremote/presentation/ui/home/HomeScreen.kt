package com.smartremote.presentation.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartremote.domain.model.*
import com.smartremote.presentation.ui.components.*
import com.smartremote.presentation.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ─── Home Screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDevice: (String) -> Unit,
    onNavigateToRemote: (String) -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToEnergy: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { scrollState.firstVisibleItemIndex > 0 } }

    Scaffold(
        topBar = {
            HomeTopBar(
                isScrolled = isScrolled,
                onSettingsClick = onNavigateToSettings,
                onVoiceClick = onNavigateToVoice,
                onAiClick = onNavigateToAi
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddDevice,
                expanded = !isScrolled,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Device") },
                text = { Text("Add Device") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            state = scrollState,
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Welcome & Status Banner
            item {
                WelcomeBanner(
                    userName = uiState.userName,
                    onlineDevices = uiState.onlineDeviceCount,
                    totalDevices = uiState.totalDeviceCount
                )
            }

            // Quick Scene Cards
            item {
                SectionHeader("Quick Scenes", icon = Icons.Default.AutoAwesome) {
                    onNavigateToAutomation()
                }
                Spacer(Modifier.height(12.dp))
                QuickScenesRow(
                    scenes = uiState.quickScenes,
                    onSceneActivate = { viewModel.activateScene(it) }
                )
            }

            // Favorite Devices
            if (uiState.favoriteDevices.isNotEmpty()) {
                item {
                    SectionHeader("Favorites", icon = Icons.Default.Star) { }
                }
                item {
                    FavoriteDevicesGrid(
                        devices = uiState.favoriteDevices,
                        onDeviceClick = { onNavigateToRemote(it.id) },
                        onDevicePowerToggle = { viewModel.toggleDevicePower(it) }
                    )
                }
            }

            // Room-based Device List
            items(uiState.roomsWithDevices) { (room, devices) ->
                RoomSection(
                    room = room,
                    devices = devices,
                    onDeviceClick = { onNavigateToRemote(it.id) },
                    onDevicePowerToggle = { viewModel.toggleDevicePower(it) }
                )
            }

            // Energy Summary
            item {
                EnergySummaryCard(
                    dailyKWh = uiState.dailyKWh,
                    monthlyCost = uiState.monthlyCost,
                    onClick = onNavigateToEnergy
                )
            }

            // AI Suggestions
            if (uiState.aiSuggestions.isNotEmpty()) {
                item {
                    AiSuggestionsCard(
                        suggestions = uiState.aiSuggestions,
                        onSuggestionClick = { viewModel.executeAiSuggestion(it) },
                        onOpenAi = onNavigateToAi
                    )
                }
            }
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    isScrolled: Boolean,
    onSettingsClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onAiClick: () -> Unit
) {
    val today = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Smart Remote",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                AnimatedVisibility(!isScrolled) {
                    Text(
                        text = today,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onVoiceClick) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Command",
                    tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onAiClick) {
                Icon(Icons.Default.SmartToy, contentDescription = "AI Assistant",
                    tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (isScrolled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.background
        )
    )
}

// ─── Welcome Banner ──────────────────────────────────────────────────────────

@Composable
fun WelcomeBanner(userName: String, onlineDevices: Int, totalDevices: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Brand600, Brand400, Color(0xFF818CF8)),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text = "Hello, ${userName.ifEmpty { "there" }} 👋",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$onlineDevices of $totalDevices devices online",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        // Decorative circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterEnd)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                Icons.Default.Hub,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center).size(40.dp),
                tint = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// ─── Quick Scenes ─────────────────────────────────────────────────────────────

@Composable
fun QuickScenesRow(scenes: List<Scene>, onSceneActivate: (Scene) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(scenes) { scene ->
            SceneChip(scene = scene, onClick = { onSceneActivate(scene) })
        }
        item {
            OutlinedCard(
                onClick = { },
                modifier = Modifier.width(100.dp).height(64.dp)
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                    Text("New", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SceneChip(scene: Scene, onClick: () -> Unit) {
    val sceneColor = Color(scene.color)
    Card(
        onClick = onClick,
        modifier = Modifier.width(110.dp).height(64.dp),
        colors = CardDefaults.cardColors(containerColor = sceneColor.copy(alpha = 0.15f)),
        border = if (scene.isActive) BorderStroke(1.5.dp, sceneColor) else null
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp), tint = sceneColor)
            Text(
                scene.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

// ─── Favorite Devices Grid ────────────────────────────────────────────────────

@Composable
fun FavoriteDevicesGrid(
    devices: List<Device>,
    onDeviceClick: (Device) -> Unit,
    onDevicePowerToggle: (Device) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(if (devices.size <= 2) 120.dp else 250.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(devices.take(4)) { device ->
            CompactDeviceCard(
                device = device,
                onClick = { onDeviceClick(device) },
                onPowerToggle = { onDevicePowerToggle(device) }
            )
        }
    }
}

// ─── Compact Device Card ──────────────────────────────────────────────────────

@Composable
fun CompactDeviceCard(device: Device, onClick: () -> Unit, onPowerToggle: () -> Unit) {
    val isOn = device.status == DeviceStatus.ONLINE
    val deviceColor = DeviceColors[device.type.name] ?: Brand500

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOn)
                deviceColor.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = if (isOn) deviceColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Switch(
                    checked = isOn,
                    onCheckedChange = { onPowerToggle() },
                    modifier = Modifier.scale(0.65f).offset(x = 8.dp, y = (-8).dp)
                )
            }
            Column {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    if (isOn) "On" else "Off",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOn) deviceColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Room Section ─────────────────────────────────────────────────────────────

@Composable
fun RoomSection(
    room: Room,
    devices: List<Device>,
    onDeviceClick: (Device) -> Unit,
    onDevicePowerToggle: (Device) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column {
        SectionHeader(
            title = room.name,
            icon = Icons.Default.HomeWork,
            expandable = true,
            isExpanded = isExpanded,
            onExpandToggle = { isExpanded = !isExpanded }
        ) { }

        AnimatedVisibility(visible = isExpanded) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(devices) { device ->
                    DeviceCard(
                        device = device,
                        onClick = { onDeviceClick(device) },
                        onPowerToggle = { onDevicePowerToggle(device) }
                    )
                }
            }
        }
    }
}

// ─── Device Card ──────────────────────────────────────────────────────────────

@Composable
fun DeviceCard(device: Device, onClick: () -> Unit, onPowerToggle: () -> Unit) {
    val isOn = device.status == DeviceStatus.ONLINE
    val deviceColor = DeviceColors[device.type.name] ?: Brand500

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "alpha"
    )

    Card(
        onClick = onClick,
        modifier = Modifier.width(130.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOn)
                deviceColor.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(if (isOn) 4.dp else 0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Status dot + icon
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(
                            if (isOn) deviceColor.copy(alpha = pulseAlpha) else Color.Gray
                        )
                )
                Text(
                    if (isOn) "Online" else "Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.Devices, null,
                modifier = Modifier.size(32.dp),
                tint = if (isOn) deviceColor else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(device.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(device.type.displayName, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)

            FilledIconToggleButton(
                checked = isOn,
                onCheckedChange = { onPowerToggle() },
                modifier = Modifier.fillMaxWidth().height(32.dp),
                colors = IconButtonDefaults.filledIconToggleButtonColors(
                    containerColor = deviceColor.copy(alpha = 0.2f),
                    checkedContainerColor = deviceColor
                )
            ) {
                Icon(Icons.Default.PowerSettingsNew, "Power", Modifier.size(16.dp))
            }
        }
    }
}

// ─── Energy Summary Card ──────────────────────────────────────────────────────

@Composable
fun EnergySummaryCard(dailyKWh: Double, monthlyCost: Double, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, null, Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.secondary)
                Column {
                    Text("Energy Today", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("${String.format("%.1f", dailyKWh)} kWh",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Est. Monthly", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                Text("₹${String.format("%.0f", monthlyCost)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

// ─── AI Suggestions Card ──────────────────────────────────────────────────────

@Composable
fun AiSuggestionsCard(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    onOpenAi: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary)
                    Text("AI Suggestions", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onOpenAi) { Text("Chat") }
            }
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionClick(suggestion) },
                    label = { Text(suggestion, style = MaterialTheme.typography.bodySmall) },
                    icon = { Icon(Icons.Default.Lightbulb, null, Modifier.size(16.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    expandable: Boolean = false,
    isExpanded: Boolean = true,
    onExpandToggle: () -> Unit = {},
    onSeeAll: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (expandable) {
                IconButton(onClick = onExpandToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onSeeAll) {
            Text("See all", style = MaterialTheme.typography.labelLarge)
        }
    }
}

fun Modifier.scale(scale: Float) = this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))
