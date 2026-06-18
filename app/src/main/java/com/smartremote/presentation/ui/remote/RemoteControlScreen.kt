package com.smartremote.presentation.ui.remote

import androidx.compose.animation.*
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartremote.domain.model.*
import com.smartremote.presentation.ui.theme.*

// ─── Remote Control Screen ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(
    deviceId: String,
    onBack: () -> Unit,
    viewModel: RemoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(deviceId) { viewModel.loadDevice(deviceId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.device?.name ?: "Remote", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(uiState.device?.type?.displayName ?: "", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    StatusBadge(isOnline = uiState.device?.status == DeviceStatus.ONLINE)
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (uiState.device?.isFavorite == true) Icons.Default.Star else Icons.Default.StarBorder,
                            "Favorite", tint = if (uiState.device?.isFavorite == true) Color(0xFFFBBF24) else LocalContentColor.current
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (uiState.device?.type) {
                DeviceType.TV, DeviceType.SET_TOP_BOX, DeviceType.PROJECTOR ->
                    TvRemote(uiState.tvState, onCommand = { cmd, v -> viewModel.sendCommand(cmd, v) })

                DeviceType.AIR_CONDITIONER ->
                    AcRemote(uiState.acState, onCommand = { cmd, v -> viewModel.sendCommand(cmd, v) })

                DeviceType.FAN ->
                    FanRemote(uiState.fanState, onCommand = { cmd, v -> viewModel.sendCommand(cmd, v) })

                DeviceType.SMART_LIGHT ->
                    LightRemote(uiState.lightState, onCommand = { cmd, v -> viewModel.sendCommand(cmd, v) })

                else ->
                    GenericRemote(device = uiState.device, onCommand = { cmd, v -> viewModel.sendCommand(cmd, v) })
            }
        }
    }
}

// ─── Status Badge ─────────────────────────────────────────────────────────────

@Composable
fun StatusBadge(isOnline: Boolean) {
    Row(
        Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (isOnline) GreenOk else RedAlert))
        Text(
            if (isOnline) "Online" else "Offline",
            style = MaterialTheme.typography.labelSmall,
            color = if (isOnline) GreenOk else RedAlert
        )
    }
}

// ─── TV Remote ────────────────────────────────────────────────────────────────

@Composable
fun TvRemote(state: TvState?, onCommand: (String, String?) -> Unit) {
    val haptic = LocalHapticFeedback.current
    fun cmd(c: String, v: String? = null) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCommand(c, v) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Power row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteButton(icon = Icons.Default.PowerSettingsNew, label = "Power",
                tint = if (state?.isPowered == true) GreenOk else RedAlert,
                onClick = { cmd("POWER_TOGGLE") })
            Text(
                text = if (state?.isPowered == true) "ON" else "OFF",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (state?.isPowered == true) GreenOk else MaterialTheme.colorScheme.onSurfaceVariant
            )
            RemoteButton(icon = Icons.Default.VolumeOff, label = "Mute",
                tint = if (state?.isMuted == true) MaterialTheme.colorScheme.error else LocalContentColor.current,
                onClick = { cmd("MUTE_TOGGLE") })
        }

        // Volume control
        RemoteSection("Volume") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RemoteButton(Icons.Default.VolumeDown, "Vol -", onClick = { cmd("VOLUME_DOWN") })
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "${state?.volume ?: 20}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                RemoteButton(Icons.Default.VolumeUp, "Vol +", onClick = { cmd("VOLUME_UP") })
            }
        }

        // Channel control
        RemoteSection("Channel") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RemoteButton(Icons.Default.KeyboardArrowDown, "CH -", onClick = { cmd("CHANNEL_DOWN") })
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "${state?.channel ?: 1}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                RemoteButton(Icons.Default.KeyboardArrowUp, "CH +", onClick = { cmd("CHANNEL_UP") })
            }
        }

        // D-Pad Navigation
        RemoteSection("Navigation") {
            DPad(
                onUp    = { cmd("NAV_UP") },
                onDown  = { cmd("NAV_DOWN") },
                onLeft  = { cmd("NAV_LEFT") },
                onRight = { cmd("NAV_RIGHT") },
                onOk    = { cmd("NAV_OK") }
            )
        }

        // Quick Buttons
        RemoteSection("Quick Actions") {
            LazyRemoteRow(
                buttons = listOf(
                    Triple(Icons.Default.Home, "Home", "HOME"),
                    Triple(Icons.Default.ArrowBack, "Back", "BACK"),
                    Triple(Icons.Default.Menu, "Menu", "MENU"),
                    Triple(Icons.Default.Info, "Info", "INFO"),
                    Triple(Icons.Default.Tune, "Source", "SOURCE"),
                    Triple(Icons.Default.AspectRatio, "Aspect", "ASPECT")
                ),
                onCommand = { cmd(it) }
            )
        }

        // Number Pad
        RemoteSection("Number Pad") {
            NumberPad(onNumber = { cmd("NUMBER", it.toString()) })
        }
    }
}

// ─── AC Remote ────────────────────────────────────────────────────────────────

@Composable
fun AcRemote(state: AcState?, onCommand: (String, String?) -> Unit) {
    val haptic = LocalHapticFeedback.current
    fun cmd(c: String, v: String? = null) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCommand(c, v) }
    val currentTemp = state?.temperature ?: 24

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Power
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FilledIconButton(
                onClick = { cmd("POWER_TOGGLE") },
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (state?.isPowered == true) GreenOk else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.Default.PowerSettingsNew, "Power", Modifier.size(32.dp))
            }
        }

        // Temperature Big Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Temperature", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    FilledIconButton(
                        onClick = { cmd("TEMP_DOWN") },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    ) { Icon(Icons.Default.Remove, "Decrease", tint = MaterialTheme.colorScheme.primary) }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$currentTemp",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text("°C", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                    }

                    FilledIconButton(
                        onClick = { cmd("TEMP_UP") },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    ) { Icon(Icons.Default.Add, "Increase", tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }

        // Mode Selection
        RemoteSection("Mode") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AcMode.values().forEach { mode ->
                    val isSelected = state?.mode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { cmd("SET_MODE", mode.name) },
                        label = { Text(mode.displayName, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(Icons.Default.AcUnit, null, Modifier.size(14.dp))
                        }
                    )
                }
            }
        }

        // Fan Speed
        RemoteSection("Fan Speed") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FanSpeed.values().forEach { speed ->
                    val isSelected = state?.fanSpeed == speed
                    FilterChip(
                        selected = isSelected,
                        onClick = { cmd("SET_FAN_SPEED", speed.name) },
                        label = { Text(speed.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Swing & Timer
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedCard(
                modifier = Modifier.weight(1f),
                onClick = { cmd("TOGGLE_SWING") }
            ) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SwapVert, null, Modifier.size(20.dp),
                        tint = if (state?.swingEnabled == true) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    Text("Swing ${if (state?.swingEnabled == true) "On" else "Off"}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedCard(
                modifier = Modifier.weight(1f),
                onClick = { }
            ) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, null, Modifier.size(20.dp))
                    Text("Timer", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ─── Fan Remote ───────────────────────────────────────────────────────────────

@Composable
fun FanRemote(state: FanState?, onCommand: (String, String?) -> Unit) {
    val haptic = LocalHapticFeedback.current
    fun cmd(c: String, v: String? = null) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCommand(c, v) }
    val speed = state?.speed ?: 2

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = { cmd("POWER_TOGGLE") },
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (state?.isPowered == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(Icons.Default.PowerSettingsNew, "Power", Modifier.size(36.dp))
        }

        RemoteSection("Speed") {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Slider(
                    value = speed.toFloat(),
                    onValueChange = { cmd("SET_SPEED", it.toInt().toString()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("1", "2", "3", "4", "5").forEach {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    "Speed: $speed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        RemoteButton(
            icon = Icons.Default.Loop,
            label = "Oscillate ${if (state?.oscillating == true) "On" else "Off"}",
            modifier = Modifier.fillMaxWidth(),
            tint = if (state?.oscillating == true) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            onClick = { cmd("TOGGLE_OSCILLATE") }
        )
    }
}

// ─── Light Remote ─────────────────────────────────────────────────────────────

@Composable
fun LightRemote(state: LightState?, onCommand: (String, String?) -> Unit) {
    val haptic = LocalHapticFeedback.current
    fun cmd(c: String, v: String? = null) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCommand(c, v) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledIconButton(
            onClick = { cmd("POWER_TOGGLE") },
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (state?.isPowered == true) Color(0xFFEAB308) else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(Icons.Default.LightMode, "Power", Modifier.size(36.dp))
        }

        RemoteSection("Brightness") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Slider(
                    value = (state?.brightness ?: 100).toFloat(),
                    onValueChange = { cmd("SET_BRIGHTNESS", it.toInt().toString()) },
                    valueRange = 1f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${state?.brightness ?: 100}%",
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }

        RemoteSection("Color Temperature") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Slider(
                    value = (state?.colorTemperature ?: 4000).toFloat(),
                    onValueChange = { cmd("SET_COLOR_TEMP", it.toInt().toString()) },
                    valueRange = 2700f..6500f,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Warm", style = MaterialTheme.typography.labelSmall)
                    Text("${state?.colorTemperature ?: 4000}K", style = MaterialTheme.typography.labelSmall)
                    Text("Cool", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ─── Generic Remote ───────────────────────────────────────────────────────────

@Composable
fun GenericRemote(device: Device?, onCommand: (String, String?) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${device?.type?.displayName ?: "Device"} Control",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        RemoteButton(Icons.Default.PowerSettingsNew, "Power",
            modifier = Modifier.fillMaxWidth(), onClick = { onCommand("POWER_TOGGLE", null) })
    }
}

// ─── Reusable Remote Components ───────────────────────────────────────────────

@Composable
fun RemoteSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
fun RemoteButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            Modifier.padding(12.dp).defaultMinSize(minWidth = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, label, Modifier.size(24.dp), tint = tint)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DPad(onUp: () -> Unit, onDown: () -> Unit, onLeft: () -> Unit, onRight: () -> Unit, onOk: () -> Unit) {
    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        // Up
        FilledTonalIconButton(onClick = onUp, modifier = Modifier.align(Alignment.TopCenter).size(52.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, "Up")
        }
        // Down
        FilledTonalIconButton(onClick = onDown, modifier = Modifier.align(Alignment.BottomCenter).size(52.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, "Down")
        }
        // Left
        FilledTonalIconButton(onClick = onLeft, modifier = Modifier.align(Alignment.CenterStart).size(52.dp)) {
            Icon(Icons.Default.KeyboardArrowLeft, "Left")
        }
        // Right
        FilledTonalIconButton(onClick = onRight, modifier = Modifier.align(Alignment.CenterEnd).size(52.dp)) {
            Icon(Icons.Default.KeyboardArrowRight, "Right")
        }
        // OK
        FilledIconButton(onClick = onOk, modifier = Modifier.size(56.dp)) {
            Text("OK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NumberPad(onNumber: (Int) -> Unit) {
    val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, -1, 0, -2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        numbers.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { n ->
                    if (n >= 0) {
                        FilledTonalButton(
                            onClick = { onNumber(n) },
                            modifier = Modifier.weight(1f)
                        ) { Text("$n", style = MaterialTheme.typography.titleMedium) }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun LazyRemoteRow(
    buttons: List<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, String>>,
    onCommand: (String) -> Unit
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { (icon, label, command) ->
            RemoteButton(icon, label, onClick = { onCommand(command) })
        }
    }
}
