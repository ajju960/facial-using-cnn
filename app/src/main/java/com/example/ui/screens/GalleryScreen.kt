package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AnalysisUiState
import com.example.ui.viewmodel.EmotionViewModel
import java.io.InputStream

data class PresetFace(
    val id: String,
    val name: String,
    val emotion: String,
    val description: String,
    val avatarColor: Color,
    val drawableRes: Int? = null // we'll use custom canvas illustration for extreme stability
)

@Composable
fun GalleryScreen(
    viewModel: EmotionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val galleryAnalysisState by viewModel.galleryAnalysisState.collectAsState()
    val selectedBitmap by viewModel.selectedGalleryBitmap.collectAsState()

    var selectedPresetId by remember { mutableStateOf<String?>(null) }
    var engineUseLocal by remember { mutableStateOf(false) }

    val presetFaces = listOf(
        PresetFace("happy_p", "Joyful Mia", "Happy", "Expressive warm smile, squinted eyes", Color(0xFF4CAF50)),
        PresetFace("sad_p", "Gloomy Liam", "Sad", "Downward lip curve, watery eye contours", Color(0xFF2196F3)),
        PresetFace("angry_p", "Furious Max", "Angry", "Tense scowl, furrowed brow creases", Color(0xFFF44336)),
        PresetFace("surprise_p", "Amazed Zoey", "Surprise", "O-shaped mouth, wide rounded orbits", Color(0xFFFFEB3B)),
        PresetFace("disgust_p", "Displeased Kai", "Disgust", "Crinkled nose, elevated upper lip", Color(0xFFFF9800)),
        PresetFace("neutral_p", "Serene Nova", "Neutral", "Balanced contours, relaxed cheeks", Color(0xFF9E9E9E))
    )

    // Image Picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        viewModel.setSelectedGalleryBitmap(bitmap)
                        selectedPresetId = null // clear preset focus since we loaded a custom image
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFDF8F6)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            Column {
                Text(
                    text = "IMAGE PORTRAIT STUDIO",
                    color = Color(0xFF6750A4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Analyze Expressions",
                    color = Color(0xFF1C1B1F),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Load custom portraits or use our high-fidelity expressive profiles to evaluate emotional features.",
                    color = Color(0xFF49454F),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Image Viewer Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(BorderStroke(1.dp, Color(0x1F79747E)), RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedBitmap != null) {
                    Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = "Selected Portrait",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )

                    // Overlay glowing reticle if analyzing
                    if (galleryAnalysisState is AnalysisUiState.Loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x33000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF6750A4))
                        }
                    }

                    // Clear selection button
                    IconButton(
                        onClick = { viewModel.clearGalleryState() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color(0xCCF3EDF7), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Selection",
                            tint = Color(0xFF1C1B1F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    // Placeholder Empty State
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Portrait Placeholder",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO PORTRAIT LOADED",
                            color = Color(0xFF49454F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("gallery_upload_button")
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = "Upload Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Image", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Engine Config Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x1F79747E), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (engineUseLocal) Icons.Default.SettingsCell else Icons.Default.Cloud,
                        contentDescription = "Engine Type",
                        tint = if (engineUseLocal) Color(0xFFFFB300) else Color(0xFF6750A4),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (engineUseLocal) "Offline Analyzer" else "Gemini Cognitive AI",
                            color = Color(0xFF1C1B1F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (engineUseLocal) "Fast on-device landmarks simulation" else "Cloud multimodal emotional mapping",
                            color = Color(0xFF49454F),
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = !engineUseLocal,
                    onCheckedChange = { engineUseLocal = !it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6750A4),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFF79747E)
                    )
                )
            }

            // Preset Profiles List
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPRESSIVE PRESET PROFILES",
                        color = Color(0xFF49454F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (selectedBitmap == null) {
                        Text(
                            text = "Select to load",
                            color = Color(0xFF6750A4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetFaces.size) { index ->
                        val preset = presetFaces[index]
                        val isSelected = selectedPresetId == preset.id

                        Card(
                            onClick = {
                                selectedPresetId = preset.id
                                // Generate a high-fidelity synthetic avatar representation as a Bitmap
                                val generatedBitmap = createIllustrativeBitmap(context, preset.emotion, preset.avatarColor)
                                viewModel.setSelectedGalleryBitmap(generatedBitmap)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) preset.avatarColor.copy(alpha = 0.12f) else Color.White
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) preset.avatarColor else Color(0x1F79747E)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .width(120.dp)
                                .height(140.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
							) {
                                // Mini avatar circle
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(preset.avatarColor.copy(alpha = 0.15f), CircleShape)
                                        .border(1.dp, preset.avatarColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getEmotionIcon(preset.emotion),
                                        contentDescription = preset.name,
                                        tint = preset.avatarColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = preset.name,
                                        color = Color(0xFF1C1B1F),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = preset.emotion.uppercase(),
                                        color = preset.avatarColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scan trigger button (when image is loaded)
            if (selectedBitmap != null && galleryAnalysisState is AnalysisUiState.Idle) {
                Button(
                    onClick = {
                        val activePreset = presetFaces.find { it.id == selectedPresetId }
                        viewModel.analyzeGalleryImage(
                            selectedBitmap!!,
                            activePreset?.emotion,
                            engineUseLocal
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (engineUseLocal) Color(0xFFFFB300) else Color(0xFF6750A4),
                        contentColor = if (engineUseLocal) Color(0xFF21005D) else Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("gallery_analyze_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Run Scan",
                        tint = if (engineUseLocal) Color(0xFF21005D) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (engineUseLocal) "LOCAL ANALYST SCAN" else "COGNITIVE GEMINI AI SCAN",
                        color = if (engineUseLocal) Color(0xFF21005D) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Results Section
            AnimatedVisibility(
                visible = selectedBitmap != null && galleryAnalysisState !is AnalysisUiState.Idle,
                enter = fadeIn() + expandVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0x1F79747E), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    when (galleryAnalysisState) {
                        is AnalysisUiState.Loading -> {
                            AnalyzingLoadingView()
                        }
                        is AnalysisUiState.Success -> {
                            val result = (galleryAnalysisState as AnalysisUiState.Success).result
                            ExpressionDetailsView(
                                result = result,
                                onRecapture = { viewModel.clearGalleryState() }
                            )
                        }
                        is AnalysisUiState.Error -> {
                            val errorMsg = (galleryAnalysisState as AnalysisUiState.Error).message
                            AnalysisErrorView(
                                message = errorMsg,
                                onRetry = {
                                    val activePreset = presetFaces.find { it.id == selectedPresetId }
                                    viewModel.analyzeGalleryImage(
                                        selectedBitmap!!,
                                        activePreset?.emotion,
                                        engineUseLocal
                                    )
                                },
                                onCancel = { viewModel.clearGalleryState() }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

fun getEmotionIcon(emotion: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (emotion) {
        "Happy" -> Icons.Default.SentimentVerySatisfied
        "Sad" -> Icons.Default.SentimentVeryDissatisfied
        "Angry" -> Icons.Default.SentimentDissatisfied
        "Surprise" -> Icons.Default.SentimentSatisfiedAlt
        "Disgust" -> Icons.Default.SentimentNeutral
        "Fear" -> Icons.Default.Warning
        else -> Icons.Default.SentimentNeutral
    }
}

/**
 * Robust helper to draw high-fidelity illustrative vector avatars onto a Canvas-backed Bitmap.
 * This guarantees pristine visual assets on all platforms, including emulators!
 */
fun createIllustrativeBitmap(context: android.content.Context, emotion: String, color: Color): Bitmap {
    val size = 512
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Base background brush
    val bgPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        shader = android.graphics.LinearGradient(
            0f, 0f, 0f, size.toFloat(),
            android.graphics.Color.parseColor("#1A1D2D"),
            android.graphics.Color.parseColor("#0F111A"),
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    // Glowing outline circle for the face
    val borderPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 10f
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
    }
    canvas.drawCircle(size / 2f, size / 2f, size * 0.4f, borderPaint)

    // Inner filled color (faded face overlay)
    val facePaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.FILL
        this.color = android.graphics.Color.argb(40, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
    }
    canvas.drawCircle(size / 2f, size / 2f, size * 0.38f, facePaint)

    // Draw Face features depending on emotion
    val featurePaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = android.graphics.Paint.Cap.ROUND
        this.color = android.graphics.Color.WHITE
    }

    val eyeY = size * 0.42f
    val eyeRadius = 15f
    val leftEyeX = size * 0.38f
    val rightEyeX = size * 0.62f

    when (emotion) {
        "Happy" -> {
            // Curved squinted eyes
            val leftEyePath = android.graphics.Path().apply {
                moveTo(leftEyeX - 25f, eyeY + 10f)
                quadTo(leftEyeX, eyeY - 15f, leftEyeX + 25f, eyeY + 10f)
            }
            val rightEyePath = android.graphics.Path().apply {
                moveTo(rightEyeX - 25f, eyeY + 10f)
                quadTo(rightEyeX, eyeY - 15f, rightEyeX + 25f, eyeY + 10f)
            }
            canvas.drawPath(leftEyePath, featurePaint)
            canvas.drawPath(rightEyePath, featurePaint)

            // Wide smiling mouth
            val mouthPath = android.graphics.Path().apply {
                moveTo(size * 0.34f, size * 0.58f)
                quadTo(size / 2f, size * 0.82f, size * 0.66f, size * 0.58f)
                quadTo(size / 2f, size * 0.62f, size * 0.34f, size * 0.58f)
            }
            featurePaint.style = android.graphics.Paint.Style.FILL_AND_STROKE
            featurePaint.color = android.graphics.Color.argb(230, 255, 255, 255)
            canvas.drawPath(mouthPath, featurePaint)
        }
        "Sad" -> {
            // Slanted downward sad eyebrows and simple eyes
            canvas.drawCircle(leftEyeX, eyeY, eyeRadius, featurePaint.apply { style = android.graphics.Paint.Style.FILL })
            canvas.drawCircle(rightEyeX, eyeY, eyeRadius, featurePaint)

            featurePaint.style = android.graphics.Paint.Style.STROKE
            canvas.drawLine(leftEyeX - 30f, eyeY - 30f, leftEyeX + 15f, eyeY - 45f, featurePaint)
            canvas.drawLine(rightEyeX - 15f, eyeY - 45f, rightEyeX + 30f, eyeY - 30f, featurePaint)

            // Downward curved mouth
            val mouthPath = android.graphics.Path().apply {
                moveTo(size * 0.36f, size * 0.7f)
                quadTo(size / 2f, size * 0.58f, size * 0.64f, size * 0.7f)
            }
            canvas.drawPath(mouthPath, featurePaint)

            // Tear drops
            val tearPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                this.color = android.graphics.Color.parseColor("#42A5F5")
            }
            canvas.drawCircle(leftEyeX + 15f, eyeY + 35f, 10f, tearPaint)
        }
        "Angry" -> {
            // Sharp inward pointing eyebrows
            canvas.drawCircle(leftEyeX, eyeY, eyeRadius, featurePaint.apply { style = android.graphics.Paint.Style.FILL })
            canvas.drawCircle(rightEyeX, eyeY, eyeRadius, featurePaint)

            featurePaint.style = android.graphics.Paint.Style.STROKE
            canvas.drawLine(leftEyeX - 30f, eyeY - 45f, leftEyeX + 25f, eyeY - 15f, featurePaint)
            canvas.drawLine(rightEyeX - 25f, eyeY - 15f, rightEyeX + 30f, eyeY - 45f, featurePaint)

            // Flat/Grim mouth
            canvas.drawLine(size * 0.38f, size * 0.65f, size * 0.62f, size * 0.65f, featurePaint)
        }
        "Surprise" -> {
            // High arched eyebrows and circular open eyes
            featurePaint.style = android.graphics.Paint.Style.STROKE
            canvas.drawCircle(leftEyeX, eyeY, 18f, featurePaint)
            canvas.drawCircle(rightEyeX, eyeY, 18f, featurePaint)

            canvas.drawLine(leftEyeX - 25f, eyeY - 40f, leftEyeX + 25f, eyeY - 55f, featurePaint)
            canvas.drawLine(rightEyeX - 25f, eyeY - 55f, rightEyeX + 25f, eyeY - 40f, featurePaint)

            // Round O-shaped mouth
            canvas.drawCircle(size / 2f, size * 0.68f, 35f, featurePaint)
        }
        "Disgust" -> {
            // Unbalanced eyes and wavy mouth
            featurePaint.style = android.graphics.Paint.Style.FILL
            canvas.drawCircle(leftEyeX, eyeY - 5f, 12f, featurePaint)
            canvas.drawCircle(rightEyeX, eyeY + 5f, 12f, featurePaint)

            // Nose wrinkling lines
            featurePaint.style = android.graphics.Paint.Style.STROKE
            canvas.drawLine(size / 2f - 15f, size * 0.52f, size / 2f + 15f, size * 0.52f, featurePaint)
            canvas.drawLine(size / 2f - 10f, size * 0.55f, size / 2f + 10f, size * 0.55f, featurePaint)

            // Slanted wavy mouth
            val mouthPath = android.graphics.Path().apply {
                moveTo(size * 0.36f, size * 0.68f)
                quadTo(size * 0.45f, size * 0.62f, size * 0.52f, size * 0.68f)
                quadTo(size * 0.58f, size * 0.74f, size * 0.64f, size * 0.68f)
            }
            canvas.drawPath(mouthPath, featurePaint)
        }
        else -> {
            // Neutral - flat lines for eyes and mouth
            canvas.drawCircle(leftEyeX, eyeY, eyeRadius, featurePaint.apply { style = android.graphics.Paint.Style.FILL })
            canvas.drawCircle(rightEyeX, eyeY, eyeRadius, featurePaint)

            // Straight horizontal mouth
            featurePaint.style = android.graphics.Paint.Style.STROKE
            canvas.drawLine(size * 0.36f, size * 0.64f, size * 0.64f, size * 0.64f, featurePaint)
        }
    }

    return bitmap
}
