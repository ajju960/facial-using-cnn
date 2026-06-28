package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emotion_logs")
data class EmotionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val imageBase64: String?,
    val primaryEmotion: String,
    val confidence: Double,
    val microExpressionTag: String,
    val aiInsight: String,
    val moodBoosterTip: String,
    val breakdownJson: String // Stores JSON map of emotion to percentage
)
