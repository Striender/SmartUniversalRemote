package com.smartremote.domain.model

import java.util.UUID

// ─── Device Types ────────────────────────────────────────────────────────────

enum class DeviceType(val displayName: String, val icon: String) {
    TV("Television", "tv"),
    AIR_CONDITIONER("Air Conditioner", "ac_unit"),
    FAN("Fan", "toys"),
    SET_TOP_BOX("Set-Top Box", "settings_input_hdmi"),
    PROJECTOR("Projector", "video_projector"),
    SPEAKER("Speaker", "speaker"),
    SMART_LIGHT("Smart Light", "light_mode"),
    STREAMING_DEVICE("Streaming Device", "stream"),
    DVD_PLAYER("DVD Player", "album"),
    SOUNDBAR("Soundbar", "speaker_group"),
    SMART_PLUG("Smart Plug", "electrical_services"),
    THERMOSTAT("Thermostat", "thermostat"),
    CAMERA("Camera", "videocam"),
    DOOR_LOCK("Door Lock", "lock"),
    CURTAIN("Curtain", "blinds"),
    OTHER("Other", "devices")
}

enum class ConnectionType(val displayName: String) {
    IR_BLASTER("Infrared (IR)"),
    WIFI("Wi-Fi"),
    BLUETOOTH("Bluetooth"),
    BLE("Bluetooth LE"),
    ESP32("ESP32/ESP8266"),
    MQTT("MQTT"),
    ZIGBEE("Zigbee"),
    Z_WAVE("Z-Wave")
}

enum class DeviceStatus {
    ONLINE, OFFLINE, CONNECTING, UNKNOWN
}

// ─── Core Device Model ────────────────────────────────────────────────────────

data class Device(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: DeviceType,
    val connectionType: ConnectionType,
    val roomId: String? = null,
    val roomName: String? = null,
    val ipAddress: String? = null,
    val macAddress: String? = null,
    val irCode: String? = null,
    val mqttTopic: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val status: DeviceStatus = DeviceStatus.UNKNOWN,
    val isFavorite: Boolean = false,
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val customProperties: Map<String, String> = emptyMap(),
    val capabilities: List<DeviceCapability> = emptyList()
)

enum class DeviceCapability {
    POWER, VOLUME, CHANNEL, TEMPERATURE, FAN_SPEED,
    BRIGHTNESS, COLOR, TIMER, SCHEDULE, ENERGY_MONITOR
}

// ─── Room Model ───────────────────────────────────────────────────────────────

data class Room(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "home",
    val color: Int = 0xFF6750A4.toInt(),
    val deviceCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Remote Control State ─────────────────────────────────────────────────────

data class TvState(
    val deviceId: String,
    val isPowered: Boolean = false,
    val volume: Int = 20,
    val isMuted: Boolean = false,
    val channel: Int = 1,
    val inputSource: String = "HDMI 1"
)

data class AcState(
    val deviceId: String,
    val isPowered: Boolean = false,
    val temperature: Int = 24,
    val mode: AcMode = AcMode.COOL,
    val fanSpeed: FanSpeed = FanSpeed.AUTO,
    val swingEnabled: Boolean = false,
    val timerMinutes: Int = 0
)

enum class AcMode(val displayName: String, val icon: String) {
    COOL("Cool", "ac_unit"),
    HEAT("Heat", "local_fire_department"),
    FAN("Fan", "toys"),
    DRY("Dry", "water_drop"),
    AUTO("Auto", "auto_mode")
}

data class FanState(
    val deviceId: String,
    val isPowered: Boolean = false,
    val speed: Int = 2,
    val oscillating: Boolean = false,
    val timerMinutes: Int = 0
)

data class LightState(
    val deviceId: String,
    val isPowered: Boolean = false,
    val brightness: Int = 100,
    val colorTemperature: Int = 4000,
    val hexColor: String = "#FFFFFF"
)

enum class FanSpeed(val displayName: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    AUTO("Auto", 0)
}

// ─── Scene / Automation Models ────────────────────────────────────────────────

data class Scene(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String,
    val color: Int,
    val actions: List<SceneAction> = emptyList(),
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class SceneAction(
    val deviceId: String,
    val deviceName: String,
    val command: String,
    val value: String? = null,
    val delayMs: Long = 0
)

data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val deviceId: String,
    val command: String,
    val value: String? = null,
    val cronExpression: String,
    val isEnabled: Boolean = true,
    val nextRunTime: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Energy Models ────────────────────────────────────────────────────────────

data class EnergyUsage(
    val deviceId: String,
    val deviceName: String,
    val wattage: Double,
    val hoursUsed: Double,
    val kWhConsumed: Double,
    val estimatedCost: Double,
    val date: Long
)

data class EnergyReport(
    val period: ReportPeriod,
    val totalKWh: Double,
    val totalCost: Double,
    val deviceBreakdown: List<EnergyUsage>,
    val savingsTip: String,
    val comparisonPercentage: Double
)

enum class ReportPeriod { DAILY, WEEKLY, MONTHLY }

// ─── Voice / AI Models ───────────────────────────────────────────────────────

data class VoiceCommand(
    val rawText: String,
    val intent: CommandIntent,
    val deviceTarget: String? = null,
    val value: String? = null,
    val confidence: Float = 0f
)

enum class CommandIntent {
    TURN_ON, TURN_OFF, SET_TEMPERATURE, SET_VOLUME,
    CHANGE_CHANNEL, SET_BRIGHTNESS, ACTIVATE_SCENE,
    QUERY_STATUS, UNKNOWN
}

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedActions: List<String> = emptyList()
)

// ─── Result Wrapper ───────────────────────────────────────────────────────────

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
