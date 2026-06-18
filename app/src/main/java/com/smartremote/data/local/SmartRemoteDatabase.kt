package com.smartremote.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartremote.data.local.dao.*
import com.smartremote.data.local.entities.*

@Database(
    entities = [
        RoomEntity::class,
        DeviceEntity::class,
        SceneEntity::class,
        ScheduleEntity::class,
        EnergyUsageEntity::class,
        CommandHistoryEntity::class,
        AiMessageEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SmartRemoteDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
    abstract fun deviceDao(): DeviceDao
    abstract fun sceneDao(): SceneDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun energyDao(): EnergyDao
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun aiMessageDao(): AiMessageDao

    companion object {
        const val DATABASE_NAME = "smart_remote.db"
    }
}
