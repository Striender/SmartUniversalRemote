package com.smartremote.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartremote.domain.model.ConnectionType
import com.smartremote.domain.model.DeviceCapability
import com.smartremote.domain.model.DeviceStatus
import com.smartremote.domain.model.DeviceType

// ─── Type Converters ──────────────────────────────────────────────────────────

class Converters {
    private val gson = Gson()

    @TypeConverter fun fromStringMap(value: Map<String, String>?): String =
        gson.toJson(value ?: emptyMap<String, String>())

    @TypeConverter fun toStringMap(value: String): Map<String, String> =
        gson.fromJson(value, object : TypeToken<Map<String, String>>() {}.type) ?: emptyMap()

    @TypeConverter fun fromCapabilities(value: List<DeviceCapability>?): String =
        gson.toJson(value ?: emptyList<DeviceCapability>())

    @TypeConverter fun toCapabilities(value: String): List<DeviceCapability> =
        gson.fromJson(value, object : TypeToken<List<DeviceCapability>>() {}.type) ?: emptyList()

    @TypeConverter fun fromStringList(value: List<String>?): String =
        gson.toJson(value ?: emptyList<String>())

    @TypeConverter fun toStringList(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type) ?: emptyList()
}

// ─── Room Entity ──────────────────────────────────────────────────────────────

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String = "home",
    val color: Int = 0xFF6750A4.toInt(),
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Device Entity ────────────────────────────────────────────────────────────

@Entity(
    tableName = "devices",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("roomId")]
)
@TypeConverters(Converters::class)
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: DeviceType,
    val connectionType: ConnectionType,
    val roomId: String? = null,
    val ipAddress: String? = null,
    val macAddress: String? = null,
    val irCode: String? = null,
    val mqttTopic: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val status: DeviceStatus = DeviceStatus.UNKNOWN,
    val isFavorite: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val customProperties: Map<String, String> = emptyMap(),
    val capabilities: List<DeviceCapability> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Scene Entity ─────────────────────────────────────────────────────────────

@Entity(tableName = "scenes")
@TypeConverters(Converters::class)
data class SceneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: Int,
    val actionsJson: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Schedule Entity ──────────────────────────────────────────────────────────

@Entity(
    tableName = "schedules",
    indices = [Index("deviceId")]
)
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val deviceId: String,
    val command: String,
    val value: String? = null,
    val cronExpression: String,
    val isEnabled: Boolean = true,
    val nextRunTime: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Energy Usage Entity ──────────────────────────────────────────────────────

@Entity(
    tableName = "energy_usage",
    indices = [Index("deviceId"), Index("date")]
)
data class EnergyUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val wattage: Double,
    val hoursUsed: Double,
    val kWhConsumed: Double,
    val estimatedCost: Double,
    val date: Long
)

// ─── Command History Entity ───────────────────────────────────────────────────

@Entity(
    tableName = "command_history",
    indices = [Index("deviceId"), Index("timestamp")]
)
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val command: String,
    val value: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)

// ─── AI Message Entity ────────────────────────────────────────────────────────

@Entity(tableName = "ai_messages")
@TypeConverters(Converters::class)
data class AiMessageEntity(
    @PrimaryKey val id: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedActions: List<String> = emptyList()
)
