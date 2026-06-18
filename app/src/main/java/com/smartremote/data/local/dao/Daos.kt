package com.smartremote.data.local.dao

import androidx.room.*
import com.smartremote.data.local.entities.*
import kotlinx.coroutines.flow.Flow

// ─── Room DAO ─────────────────────────────────────────────────────────────────

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY createdAt ASC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :roomId")
    suspend fun getRoomById(roomId: String): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Delete
    suspend fun deleteRoom(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :roomId")
    suspend fun deleteRoomById(roomId: String)
}

// ─── Device DAO ───────────────────────────────────────────────────────────────

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY createdAt ASC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE roomId = :roomId")
    fun getDevicesByRoom(roomId: String): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE isFavorite = 1")
    fun getFavoriteDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :deviceId")
    suspend fun getDeviceById(deviceId: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%'")
    fun searchDevices(query: String): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteDeviceById(deviceId: String)

    @Query("UPDATE devices SET isFavorite = :isFavorite WHERE id = :deviceId")
    suspend fun updateFavorite(deviceId: String, isFavorite: Boolean)

    @Query("UPDATE devices SET status = :status, lastSeen = :lastSeen WHERE id = :deviceId")
    suspend fun updateDeviceStatus(deviceId: String, status: String, lastSeen: Long)

    @Query("SELECT COUNT(*) FROM devices WHERE roomId = :roomId")
    suspend fun getDeviceCountForRoom(roomId: String): Int
}

// ─── Scene DAO ────────────────────────────────────────────────────────────────

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes ORDER BY createdAt ASC")
    fun getAllScenes(): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE id = :sceneId")
    suspend fun getSceneById(sceneId: String): SceneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: SceneEntity)

    @Update
    suspend fun updateScene(scene: SceneEntity)

    @Delete
    suspend fun deleteScene(scene: SceneEntity)

    @Query("UPDATE scenes SET isActive = :isActive WHERE id = :sceneId")
    suspend fun updateSceneActive(sceneId: String, isActive: Boolean)
}

// ─── Schedule DAO ─────────────────────────────────────────────────────────────

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY nextRunTime ASC")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE deviceId = :deviceId")
    fun getSchedulesForDevice(deviceId: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE isEnabled = 1 AND nextRunTime <= :now")
    suspend fun getDueSchedules(now: Long): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("UPDATE schedules SET isEnabled = :enabled WHERE id = :scheduleId")
    suspend fun setScheduleEnabled(scheduleId: String, enabled: Boolean)
}

// ─── Energy DAO ───────────────────────────────────────────────────────────────

@Dao
interface EnergyDao {
    @Query("SELECT * FROM energy_usage WHERE deviceId = :deviceId ORDER BY date DESC LIMIT :limit")
    fun getDeviceEnergyHistory(deviceId: String, limit: Int = 30): Flow<List<EnergyUsageEntity>>

    @Query("SELECT * FROM energy_usage WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getEnergyUsageForPeriod(startDate: Long, endDate: Long): Flow<List<EnergyUsageEntity>>

    @Query("SELECT SUM(kWhConsumed) FROM energy_usage WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalKWhForPeriod(startDate: Long, endDate: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnergyUsage(usage: EnergyUsageEntity)

    @Query("DELETE FROM energy_usage WHERE date < :beforeDate")
    suspend fun deleteOldRecords(beforeDate: Long)
}

// ─── Command History DAO ──────────────────────────────────────────────────────

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT :limit")
    fun getCommandHistory(deviceId: String, limit: Int = 50): Flow<List<CommandHistoryEntity>>

    @Query("SELECT command, COUNT(*) as count FROM command_history WHERE deviceId = :deviceId GROUP BY command ORDER BY count DESC LIMIT 5")
    suspend fun getMostUsedCommands(deviceId: String): List<CommandFrequency>

    @Insert
    suspend fun insertCommand(command: CommandHistoryEntity)

    @Query("DELETE FROM command_history WHERE timestamp < :before")
    suspend fun deleteOldHistory(before: Long)
}

data class CommandFrequency(val command: String, val count: Int)

// ─── AI Message DAO ───────────────────────────────────────────────────────────

@Dao
interface AiMessageDao {
    @Query("SELECT * FROM ai_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<AiMessageEntity>>

    @Insert
    suspend fun insertMessage(message: AiMessageEntity)

    @Query("DELETE FROM ai_messages WHERE id NOT IN (SELECT id FROM ai_messages ORDER BY timestamp DESC LIMIT 100)")
    suspend fun pruneOldMessages()

    @Query("DELETE FROM ai_messages")
    suspend fun clearAllMessages()
}
