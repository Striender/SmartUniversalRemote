package com.smartremote.data.repository

import com.google.gson.Gson
import com.smartremote.data.local.dao.*
import com.smartremote.data.local.entities.*
import com.smartremote.data.remote.api.SmartRemoteApiService
import com.smartremote.data.remote.mqtt.MqttManager
import com.smartremote.domain.model.*
import com.smartremote.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ─── Device Repository ────────────────────────────────────────────────────────

@Singleton
class DeviceRepository @Inject constructor(
    private val deviceDao: DeviceDao,
    private val roomDao: RoomDao,
    private val apiService: SmartRemoteApiService,
    private val mqttManager: MqttManager,
    private val gson: Gson
) {
    fun getAllDevices(): Flow<List<Device>> =
        deviceDao.getAllDevices().map { list -> list.map { it.toDomain() } }

    fun getDevicesByRoom(roomId: String): Flow<List<Device>> =
        deviceDao.getDevicesByRoom(roomId).map { list -> list.map { it.toDomain() } }

    fun getFavoriteDevices(): Flow<List<Device>> =
        deviceDao.getFavoriteDevices().map { list -> list.map { it.toDomain() } }

    fun searchDevices(query: String): Flow<List<Device>> =
        deviceDao.searchDevices(query).map { list -> list.map { it.toDomain() } }

    suspend fun getDeviceById(id: String): Device? =
        deviceDao.getDeviceById(id)?.toDomain()

    suspend fun addDevice(device: Device) {
        deviceDao.insertDevice(device.toEntity())
    }

    suspend fun updateDevice(device: Device) {
        deviceDao.updateDevice(device.toEntity())
    }

    suspend fun deleteDevice(deviceId: String) {
        deviceDao.deleteDeviceById(deviceId)
    }

    suspend fun toggleFavorite(deviceId: String, isFavorite: Boolean) {
        deviceDao.updateFavorite(deviceId, isFavorite)
    }

    suspend fun sendCommand(device: Device, command: String, value: String? = null): Result<Boolean> {
        return try {
            when (device.connectionType) {
                ConnectionType.MQTT -> {
                    val topic = device.mqttTopic ?: "smartremote/${device.id}/command"
                    val payload = gson.toJson(mapOf("command" to command, "value" to value))
                    mqttManager.publish(topic, payload)
                    Result.Success(true)
                }
                ConnectionType.WIFI, ConnectionType.ESP32 -> {
                    val response = apiService.sendCommand(
                        device.id,
                        com.smartremote.data.remote.api.CommandRequest(
                            deviceId = device.id,
                            command = command,
                            value = value
                        )
                    )
                    if (response.isSuccessful && response.body()?.success == true) {
                        Result.Success(true)
                    } else {
                        Result.Error("Command failed: ${response.body()?.message}")
                    }
                }
                else -> {
                    // IR Blaster / BLE handled by device-specific managers
                    Result.Success(true)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to send command")
            Result.Error(e.message ?: "Unknown error", e)
        }
    }

    suspend fun discoverDevices(): Result<List<Device>> {
        return try {
            val response = apiService.discoverDevices("local")
            if (response.isSuccessful) {
                val discovered = response.body()?.data?.devices?.map { d ->
                    Device(
                        name = d.name,
                        type = DeviceType.values().firstOrNull { it.name == d.type } ?: DeviceType.OTHER,
                        connectionType = ConnectionType.WIFI,
                        ipAddress = d.ipAddress,
                        macAddress = d.macAddress,
                        brand = d.brand,
                        model = d.model
                    )
                } ?: emptyList()
                Result.Success(discovered)
            } else {
                Result.Error("Discovery failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Discovery error", e)
        }
    }
}

// ─── Room Repository ──────────────────────────────────────────────────────────

@Singleton
class RoomRepository @Inject constructor(
    private val roomDao: RoomDao,
    private val deviceDao: DeviceDao
) {
    fun getAllRooms(): Flow<List<Room>> =
        roomDao.getAllRooms().map { list -> list.map { it.toDomain() } }

    suspend fun addRoom(room: Room) = roomDao.insertRoom(room.toEntity())
    suspend fun updateRoom(room: Room) = roomDao.updateRoom(room.toEntity())
    suspend fun deleteRoom(roomId: String) = roomDao.deleteRoomById(roomId)
}

// ─── Scene Repository ─────────────────────────────────────────────────────────

@Singleton
class SceneRepository @Inject constructor(
    private val sceneDao: SceneDao,
    private val gson: Gson
) {
    fun getAllScenes(): Flow<List<Scene>> =
        sceneDao.getAllScenes().map { list -> list.map { it.toDomain(gson) } }

    suspend fun addScene(scene: Scene) = sceneDao.insertScene(scene.toEntity(gson))
    suspend fun updateScene(scene: Scene) = sceneDao.updateScene(scene.toEntity(gson))
    suspend fun deleteScene(scene: Scene) = sceneDao.deleteScene(scene.toEntity(gson))
    suspend fun activateScene(sceneId: String) = sceneDao.updateSceneActive(sceneId, true)
}

// ─── Energy Repository ────────────────────────────────────────────────────────

@Singleton
class EnergyRepository @Inject constructor(
    private val energyDao: EnergyDao,
    private val apiService: SmartRemoteApiService
) {
    fun getDeviceEnergyHistory(deviceId: String): Flow<List<EnergyUsage>> =
        energyDao.getDeviceEnergyHistory(deviceId).map { list ->
            list.map { it.toDomain("") }
        }

    fun getEnergyForPeriod(startDate: Long, endDate: Long): Flow<List<EnergyUsage>> =
        energyDao.getEnergyUsageForPeriod(startDate, endDate).map { list ->
            list.map { it.toDomain("") }
        }

    suspend fun recordUsage(usage: EnergyUsage) =
        energyDao.insertEnergyUsage(usage.toEntity())
}

// ─── Mappers ──────────────────────────────────────────────────────────────────

fun DeviceEntity.toDomain() = Device(
    id = id, name = name, type = type, connectionType = connectionType,
    roomId = roomId, ipAddress = ipAddress, macAddress = macAddress,
    irCode = irCode, mqttTopic = mqttTopic, brand = brand, model = model,
    status = status, isFavorite = isFavorite, lastSeen = lastSeen,
    imageUrl = imageUrl, customProperties = customProperties, capabilities = capabilities
)

fun Device.toEntity() = DeviceEntity(
    id = id, name = name, type = type, connectionType = connectionType,
    roomId = roomId, ipAddress = ipAddress, macAddress = macAddress,
    irCode = irCode, mqttTopic = mqttTopic, brand = brand, model = model,
    status = status, isFavorite = isFavorite, lastSeen = lastSeen,
    imageUrl = imageUrl, customProperties = customProperties, capabilities = capabilities
)

fun RoomEntity.toDomain() = Room(id = id, name = name, icon = icon, color = color, createdAt = createdAt)
fun Room.toEntity() = RoomEntity(id = id, name = name, icon = icon, color = color, createdAt = createdAt)

fun SceneEntity.toDomain(gson: Gson): Scene {
    val actions = gson.fromJson(actionsJson, Array<SceneAction>::class.java)?.toList() ?: emptyList()
    return Scene(id = id, name = name, icon = icon, color = color, actions = actions, isActive = isActive, createdAt = createdAt)
}
fun Scene.toEntity(gson: Gson) = SceneEntity(
    id = id, name = name, icon = icon, color = color,
    actionsJson = gson.toJson(actions), isActive = isActive, createdAt = createdAt
)

fun EnergyUsageEntity.toDomain(deviceName: String) = EnergyUsage(
    deviceId = deviceId, deviceName = deviceName, wattage = wattage,
    hoursUsed = hoursUsed, kWhConsumed = kWhConsumed, estimatedCost = estimatedCost, date = date
)
fun EnergyUsage.toEntity() = EnergyUsageEntity(
    deviceId = deviceId, wattage = wattage, hoursUsed = hoursUsed,
    kWhConsumed = kWhConsumed, estimatedCost = estimatedCost, date = date
)
