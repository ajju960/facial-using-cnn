package com.example

import org.junit.Assert.*
import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Request

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun listAvailableModels() {
    val apiKey = BuildConfig.GEMINI_API_KEY
    println("--- GEMINI API KEY LENGTH: ${apiKey.length} ---")
    if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
        println("Warning: API Key is not configured correctly.")
        return
    }
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
        .build()
    
    try {
        client.newCall(request).execute().use { response ->
            println("--- GEMINI RESPONSE CODE: ${response.code} ---")
            println("--- GEMINI RESPONSE BODY: ---")
            println(response.body?.string())
            println("-----------------------------")
        }
    } catch (e: Exception) {
        println("Failed to list models: ${e.message}")
        e.printStackTrace()
    }
  }
}
