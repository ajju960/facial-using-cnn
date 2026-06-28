package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val CleanMinimalismColorScheme =
  lightColorScheme(
    primary = Color(0xFF6750A4),         // Deep Purple active
    onPrimary = Color(0xFFFFFFFF),       // White on primary
    primaryContainer = Color(0xFFEADDFF), // Soft lavender active highlights
    onPrimaryContainer = Color(0xFF21005D), // Deep dark violet on primary container
    secondary = Color(0xFF381E72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3EDF7), // Soft greyish lavender for metrics grids
    onSecondaryContainer = Color(0xFF1C1B1F),
    tertiary = Color(0xFFFFB300),
    background = Color(0xFFFDF8F6),      // Soft warm minimalist cream background
    onBackground = Color(0xFF1C1B1F),    // Dark primary text
    surface = Color(0xFFFFFFFF),         // Clean pure white for primary cards
    onSurface = Color(0xFF1C1B1F),
    outline = Color(0x1F79747E),         // Very subtle borders matching Tailwind border-79747E/10
    surfaceVariant = Color(0xFFF3EDF7),
    onSurfaceVariant = Color(0xFF49454F)  // Slate/grey muted labels
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors to strictly enforce Clean Minimalism brand palette
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = CleanMinimalismColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
