package com.example.data.repository

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.local.EmotionDao
import com.example.data.local.EmotionEntity
import com.example.data.remote.Content
import com.example.data.remote.EmotionAnalysisResult
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.InlineData
import com.example.data.remote.Part
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class EmotionRepository(private val emotionDao: EmotionDao) {

    val allLogs: Flow<List<EmotionEntity>> = emotionDao.getAllLogs()

    suspend fun insertLog(log: EmotionEntity) = withContext(Dispatchers.IO) {
        emotionDao.insertLog(log)
    }

    suspend fun deleteLogById(id: Int) = withContext(Dispatchers.IO) {
        emotionDao.deleteLogById(id)
    }

    suspend fun deleteAllLogs() = withContext(Dispatchers.IO) {
        emotionDao.deleteAllLogs()
    }

    suspend fun analyzeFaceWithGemini(bitmap: Bitmap): Result<EmotionAnalysisResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not configured. Please add your key in the Secrets panel in AI Studio."))
        }

        try {
            // Compress and convert bitmap to base64
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val prompt = """
                Analyze the face in this image and classify the person's emotional state.
                You MUST return a JSON object with EXACTLY the following structure (no markdown wrappers, no backticks, just raw JSON text):
                {
                  "faceDetected": true,
                  "primaryEmotion": "Happy",
                  "confidence": 0.95,
                  "emotionBreakdown": {
                    "Happy": 95,
                    "Sad": 2,
                    "Angry": 0,
                    "Neutral": 3,
                    "Surprise": 0,
                    "Fear": 0,
                    "Disgust": 0
                  },
                  "microExpressionTag": "Duchenne Smile",
                  "aiInsight": "You are radiating high positive energy! The eyes show a genuine crinkling associated with real happiness.",
                  "moodBoosterTip": "Keep this positive energy going! Share a warm gesture or smile with someone around you today."
                }
                
                Ensure the primaryEmotion is exactly one of: Happy, Sad, Angry, Neutral, Surprise, Fear, Disgust.
                The percentages in the emotionBreakdown map must sum to approximately 100.
                If NO face is detected in the image, return:
                {
                  "faceDetected": false,
                  "primaryEmotion": "Neutral",
                  "confidence": 0.0,
                  "emotionBreakdown": {
                    "Happy": 0,
                    "Sad": 0,
                    "Angry": 0,
                    "Neutral": 100,
                    "Surprise": 0,
                    "Fear": 0,
                    "Disgust": 0
                  },
                  "microExpressionTag": "No Face Found",
                  "aiInsight": "We couldn't clearly detect a face in this image. Please adjust lighting and framing, then try again.",
                  "moodBoosterTip": "Try aligning your face in the center of the camera or upload a clear portrait."
                }
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data))
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.2f
                )
            )

            val response = RetrofitClient.service.analyzeFace(apiKey, request)
            val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response received from Gemini.")

            // Parse responseText using Moshi
            val parsedResult = RetrofitClient.responseAdapter.fromJson(responseText)
                ?: throw Exception("Failed to parse Gemini response JSON.")

            Result.success(parsedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
