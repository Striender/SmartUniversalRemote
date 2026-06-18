package com.smartremote.presentation.ui.energy

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
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.smartremote.data.repository.DeviceRepository
import com.smartremote.data.repository.EnergyRepository
import com.smartremote.domain.model.*
import com.smartremote.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class EnergyUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.DAILY,
    val totalKWh: Double = 4.2,
    val totalCost: Double = 42.0,
    val savingsPercent: Double = 12.0,
    val devices: List<Device> = emptyList(),
    val deviceUsage: List<Pair<String, Double>> = emptyList(),
    val weeklyData: List<Pair<String, Double>> = emptyList(),
    val tips: List<String> = emptyList()
)

@HiltViewModel
class EnergyViewModel @Inject constructor(
    private val energyRepo: EnergyRepository,
    private val deviceRepo: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EnergyUiState())
    val uiState: StateFlow<EnergyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            deviceRepo.getAllDevices().collect { devices ->
                _uiState.update {
                    it.copy(
                        devices = devices,
                        deviceUsage = devices.map { d -> d.name to (0.2 + Math.random() * 1.5) },
                        weeklyData = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
                            .map { day -> day to (2.0 + Math.random() * 4.0) },
                        tips = listOf(
                            "Your AC accounts for 45% of energy usage. Try raising temp by 1°C to save ₹80/month.",
                            "Set your TV to auto power-off after 2 hours to save ₹35/month.",
                            "Use Sleep Mode routine every night to reduce consumption by 30%."
                        )
                    )
                }
            }
        }
    }

    fun selectPeriod(period: ReportPeriod) = _uiState.update { it.copy(selectedPeriod = period) }
}

// ─── Energy Screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyScreen(
    onBack: () -> Unit,
    viewModel: EnergyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Energy Monitor", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period selector
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportPeriod.values().forEach { period ->
                        FilterChip(
                            selected = uiState.selectedPeriod == period,
                            onClick = { viewModel.selectPeriod(period) },
                            label = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            // Summary cards
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EnergyStatCard("Total Usage", "${uiState.totalKWh} kWh", Icons.Default.Bolt,
                        MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
                    EnergyStatCard("Est. Cost", "₹${uiState.totalCost.roundToInt()}", Icons.Default.CurrencyRupee,
                        MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenOk.copy(alpha = 0.1f))) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingDown, null, Modifier.size(28.dp), tint = GreenOk)
                        Column {
                            Text("Savings vs last period", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${uiState.savingsPercent}% less consumption", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = GreenOk)
                        }
                    }
                }
            }

            // Weekly bar chart
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Weekly Usage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        SimpleBarChart(data = uiState.weeklyData, modifier = Modifier.fillMaxWidth().height(140.dp))
                    }
                }
            }

            // Device breakdown
            item {
                Text("Device Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            items(uiState.deviceUsage) { (name, kwh) ->
                val totalKwh = uiState.deviceUsage.sumOf { it.second }.takeIf { it > 0 } ?: 1.0
                val percent = (kwh / totalKwh * 100).roundToInt()
                DeviceEnergyRow(name = name, kwh = kwh, percent = percent)
            }

            // Tips
            item {
                Text("Energy Saving Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            items(uiState.tips) { tip ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Amber500.copy(alpha = 0.08f))) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Lightbulb, null, Modifier.size(20.dp), tint = Amber500)
                        Text(tip, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─── Energy Stat Card ─────────────────────────────────────────────────────────

@Composable
fun EnergyStatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                   containerColor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(24.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Device Energy Row ────────────────────────────────────────────────────────

@Composable
fun DeviceEnergyRow(name: String, kwh: Double, percent: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${String.format("%.2f", kwh)} kWh · $percent%",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth(),
                color = if (percent > 40) RedAlert else if (percent > 20) Amber500 else GreenOk,
                trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

// ─── Simple Bar Chart ─────────────────────────────────────────────────────────

@Composable
fun SimpleBarChart(data: List<Pair<String, Double>>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant

    if (data.isEmpty()) return
    val maxVal = data.maxOf { it.second }.takeIf { it > 0 } ?: 1.0

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2f)
        val spacing = barWidth
        val textPaint = android.graphics.Paint().apply {
            color = onSurface.toArgb()
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
        }

        data.forEachIndexed { i, (label, value) ->
            val x = i * (barWidth + spacing) + spacing / 2
            val barHeight = (value / maxVal * (size.height - 40f)).toFloat()
            val top = size.height - barHeight - 30f

            drawRoundRect(color = surfaceVariant, topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height - 30f), cornerRadius = CornerRadius(8f, 8f))
            drawRoundRect(color = primaryColor, topLeft = Offset(x, top),
                size = Size(barWidth, barHeight), cornerRadius = CornerRadius(8f, 8f))

            drawContext.canvas.nativeCanvas.drawText(label, x + barWidth / 2, size.height, textPaint)
        }
    }
}
