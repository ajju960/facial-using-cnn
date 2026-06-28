package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionDao {
    @Query("SELECT * FROM emotion_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<EmotionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EmotionEntity)

    @Query("DELETE FROM emotion_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM emotion_logs")
    suspend fun deleteAllLogs()
}
