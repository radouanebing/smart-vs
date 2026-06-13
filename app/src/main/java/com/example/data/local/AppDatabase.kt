package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.ChatMessage
import com.example.data.model.DeviceStatus
import com.example.data.model.SystemSettings
import com.example.data.model.AutomationRule
import com.example.data.model.VoiceProfile
import com.example.data.model.WakeWord
import com.example.data.model.VoiceLearningLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartDao {
    // Chat commands & messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()

    // Device telemetry status
    @Query("SELECT * FROM device_status WHERE id = 1")
    fun getDeviceStatus(): Flow<DeviceStatus?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDeviceStatus(status: DeviceStatus)

    // User settings configurations
    @Query("SELECT * FROM system_settings WHERE id = 1")
    fun getSettings(): Flow<SystemSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: SystemSettings)

    // Custom Automation Rules persistence
    @Query("SELECT * FROM automation_rules WHERE isActive = 1")
    fun getActiveAutomationRules(): Flow<List<AutomationRule>>

    @Query("SELECT * FROM automation_rules")
    fun getAllAutomationRules(): Flow<List<AutomationRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomationRule(rule: AutomationRule)

    @Delete
    suspend fun deleteAutomationRule(rule: AutomationRule)

    // Voice Profiles persistence operations
    @Query("SELECT * FROM voice_profiles")
    fun getAllVoiceProfiles(): Flow<List<VoiceProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceProfile(profile: VoiceProfile)

    @Delete
    suspend fun deleteVoiceProfile(profile: VoiceProfile)

    // Customizable wake word tokens
    @Query("SELECT * FROM wake_words")
    fun getAllWakeWords(): Flow<List<WakeWord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWakeWord(wakeWord: WakeWord)

    @Delete
    suspend fun deleteWakeWord(wakeWord: WakeWord)

    // Learning engine logs & phonetic calibrations
    @Query("SELECT * FROM voice_learning_logs ORDER BY timestamp DESC")
    fun getVoiceLearningLogs(): Flow<List<VoiceLearningLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceLearningLog(log: VoiceLearningLog)
}

@Database(entities = [ChatMessage::class, DeviceStatus::class, SystemSettings::class, AutomationRule::class, VoiceProfile::class, WakeWord::class, VoiceLearningLog::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val smartDao: SmartDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_voice_assistant_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
