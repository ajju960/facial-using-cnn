package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.example.data.remote.EmotionAnalysisResult
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.viewmodel.AnalysisUiState
import com.example.ui.viewmodel.EmotionViewModel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    viewModel: EmotionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraAnalysisState by viewModel.cameraAnalysisState.collectAsState()
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // CameraX setups
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFFDF8F6)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasCameraPermission) {
                if (capturedBitmap == null) {
                    // CAMERA PREVIEW MODE
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_FRONT_CAMERA,
                                            preview,
                                            imageCapture
                                        )
                                    } catch (exc: Exception) {
                                        Log.e("CameraScreen", "Use case binding failed", exc)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Glowing high-tech face scan overlay
                        FaceScanGridOverlay()

                        // Camera Controller Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(Color(0xE6FFFFFF), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0x1F79747E), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Face Detector Active",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ALIGN YOUR FACE IN THE RETICLE",
                                color = Color(0xFF1C1B1F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Bottom Actions Panel
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFFFDF8F6))
                                    )
                                )
                                .padding(bottom = 32.dp, top = 64.dp)
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Local Simulation Switch
                                var useLocalAnalysis by remember { mutableStateOf(false) }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { useLocalAnalysis = !useLocalAnalysis }
                                ) {
                                    Icon(
                                        imageVector = if (useLocalAnalysis) Icons.Default.SettingsCell else Icons.Default.CloudQueue,
                                        contentDescription = "Analysis Engine",
                                        tint = if (useLocalAnalysis) Color(0xFFFFB300) else Color(0xFF6750A4),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = if (useLocalAnalysis) "Offline" else "Gemini AI",
                                        color = if (useLocalAnalysis) Color(0xFFFFB300) else Color(0xFF6750A4),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                // Capture Button
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .border(4.dp, Color(0xFF6750A4), CircleShape)
                                        .padding(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .clickable {
                                            capturePhoto(context, imageCapture, cameraExecutor) { bitmap ->
                                                viewModel.analyzeCameraFrame(bitmap, useLocalAnalysis)
                                            }
                                        }
                                        .testTag("camera_capture_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Capture Frame",
                                        tint = Color(0xFF21005D),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // Quick tips guide trigger
                                var showTipsDialog by remember { mutableStateOf(false) }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { showTipsDialog = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = "Tips Guide",
                                        tint = Color(0xFF6750A4),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Tips",
                                        color = Color(0xFF6750A4),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                if (showTipsDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showTipsDialog = false },
                                        title = { Text("Optimal Scanning Guide") },
                                        text = {
                                            Text("1. Ensure clear frontal lighting (avoid strong shadows).\n2. Remove sunglasses or obstructions.\n3. Position your face inside the glowing scan bounding box.\n4. Hold a clear expression (e.g., Happy, Surprised, neutral) for accuracy.")
                                        },
                                        confirmButton = {
                                            Button(onClick = { showTipsDialog = false }) {
                                                Text("Got it")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // CAPTURED & ANALYZING MODE
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Display Captured Image
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Captured Frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.clearCameraState() },
                                modifier = Modifier
                                    .background(Color(0xAA000000), CircleShape)
                                    .size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Go Back",
                                    tint = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(Color(0xE6FFFFFF), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0x1F79747E), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "SCAN ANALYSIS",
                                    color = Color(0xFF6750A4),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // Bottom results details
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.6f)
                                    .background(
                                        Color(0xFFFFFFFF),
                                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                    )
                                    .border(
                                        1.dp,
                                        Color(0x1F79747E),
                                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                    )
                                    .padding(24.dp)
                            ) {
                                when (cameraAnalysisState) {
                                    is AnalysisUiState.Loading -> {
                                        AnalyzingLoadingView()
                                    }
                                    is AnalysisUiState.Success -> {
                                        val result = (cameraAnalysisState as AnalysisUiState.Success).result
                                        ExpressionDetailsView(
                                            result = result,
                                            onRecapture = { viewModel.clearCameraState() }
                                        )
                                    }
                                    is AnalysisUiState.Error -> {
                                        val errorMsg = (cameraAnalysisState as AnalysisUiState.Error).message
                                        AnalysisErrorView(
                                            message = errorMsg,
                                            onRetry = {
                                                viewModel.analyzeCameraFrame(capturedBitmap!!, false)
                                            },
                                            onCancel = { viewModel.clearCameraState() }
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            } else {
                // NO CAMERA PERMISSION PROMPT
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Permission Required",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Needed",
                        color = Color(0xFF1C1B1F),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This app requires the front camera to scan and classify facial expressions in real-time.",
                        color = Color(0xFF49454F),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FaceScanGridOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Define a crop rectangle for the face in the center
        val rectWidth = width * 0.7f
        val rectHeight = rectWidth * 1.2f
        val left = (width - rectWidth) / 2
        val top = (height - rectHeight) / 3

        // Dim background outside the box
        drawRect(
            color = Color(0x77000000),
            size = size
        )

        // Clear/Clip the face region
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(rectWidth, rectHeight),
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        )

        // Draw corner brackets
        val bracketLength = 40.dp.toPx()
        val thickness = 4.dp.toPx()
        val glowColor = Color(0xFF6750A4)

        // Top Left Bracket
        drawRect(glowColor, Offset(left - thickness, top - thickness), Size(bracketLength, thickness))
        drawRect(glowColor, Offset(left - thickness, top - thickness), Size(thickness, bracketLength))

        // Top Right Bracket
        drawRect(glowColor, Offset(left + rectWidth - bracketLength + thickness, top - thickness), Size(bracketLength, thickness))
        drawRect(glowColor, Offset(left + rectWidth, top - thickness), Size(thickness, bracketLength))

        // Bottom Left Bracket
        drawRect(glowColor, Offset(left - thickness, top + rectHeight), Size(bracketLength, thickness))
        drawRect(glowColor, Offset(left - thickness, top + rectHeight - bracketLength + thickness), Size(thickness, bracketLength))

        // Bottom Right Bracket
        drawRect(glowColor, Offset(left + rectWidth - bracketLength + thickness, top + rectHeight), Size(bracketLength, thickness))
        drawRect(glowColor, Offset(left + rectWidth, top + rectHeight - bracketLength + thickness), Size(thickness, bracketLength))

        // Laser Scan Line
        val laserY = top + (rectHeight * laserOffsetY)
        drawLine(
            color = Color(0xFF6750A4),
            start = Offset(left, laserY),
            end = Offset(left + rectWidth, laserY),
            strokeWidth = 3.dp.toPx()
        )
    }
}

@Composable
fun AnalyzingLoadingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_dots")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(80.dp)
                    .border(1.dp, Color(0x1F79747E), CircleShape),
                color = Color(0xFF6750A4),
                strokeWidth = 4.dp
            )
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Analyzing",
                tint = Color(0xFF6750A4),
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "AI DEEP SCAN ACTIVE",
            color = Color(0xFF1C1B1F),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Staggered status subtitles
        var tipText by remember { mutableStateOf("Isolating face coordinates...") }
        LaunchedEffect(Unit) {
            val tips = listOf(
                "Isolating facial boundaries...",
                "Running structural landmark match...",
                "Calculating facial muscle contractions...",
                "Mapping micro-expressions to standard categories...",
                "Querying Gemini cognitive engine..."
            )
            var index = 0
            while (true) {
                kotlinx.coroutines.delay(1800)
                index = (index + 1) % tips.size
                tipText = tips[index]
            }
        }

        Text(
            text = tipText,
            color = Color(0xFF49454F),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ExpressionDetailsView(
    result: EmotionAnalysisResult,
    onRecapture: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PRIMARY EMOTION",
                        color = Color(0xFF49454F),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = result.primaryEmotion ?: "Neutral",
                        color = getEmotionColor(result.primaryEmotion ?: "Neutral"),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = getEmotionColor(result.primaryEmotion ?: "Neutral").copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = getEmotionColor(result.primaryEmotion ?: "Neutral").copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Confidence: ${String.format("%.0f", (result.confidence ?: 0.0) * 100)}%",
                        color = getEmotionColor(result.primaryEmotion ?: "Neutral"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Micro-expression Tag
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Cognitive landmark",
                        tint = Color(0xFF21005D),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MICRO-EXPRESSION MARK",
                            color = Color(0xFF49454F),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = result.microExpressionTag ?: "Unknown Contraction",
                            color = Color(0xFF1C1B1F),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Emotion Breakdown
        item {
            Text(
                text = "EMOTION WEIGHT MATRIX",
                color = Color(0xFF49454F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val breakdown = result.emotionBreakdown ?: emptyMap()
        if (breakdown.isNotEmpty()) {
            val sortedBreakdown = breakdown.entries.sortedByDescending { it.value }
            items(sortedBreakdown.size) { index ->
                val entry = sortedBreakdown[index]
                val emotion = entry.key
                val percentage = entry.value

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = emotion,
                            color = Color(0xFF1C1B1F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$percentage%",
                            color = getEmotionColor(emotion),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFFF3EDF7), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (percentage / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(getEmotionColor(emotion), CircleShape)
                        )
                    }
                }
            }
        } else {
            item {
                Text("No weights calculated", color = Color.Gray, fontSize = 12.sp)
            }
        }

        // AI Insight
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3EDF7), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x1F79747E), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "AI Insight",
                        tint = Color(0xFF21005D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI COGNITIVE INSIGHT",
                        color = Color(0xFF21005D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = result.aiInsight ?: "Neutral profile detected without landmarks.",
                    color = Color(0xFF1C1B1F),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // Mood booster recommendation
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDF8F6), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x1F79747E), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Recommendation",
                        tint = Color(0xFF21005D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECOMMENDED ACTION",
                        color = Color(0xFF21005D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = result.moodBoosterTip ?: "Take a moment to align your focus and posture.",
                    color = Color(0xFF1C1B1F),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRecapture,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6750A4)),
                    border = BorderStroke(1.dp, Color(0xFF6750A4)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Scan Again", tint = Color(0xFF6750A4))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SCAN AGAIN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AnalysisErrorView(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Analysis Error",
            tint = Color(0xFFFF5252),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SCAN ENGINE RETRIAL",
            color = Color(0xFF1C1B1F),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color(0xFF49454F),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3EDF7)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = Color(0xFF1C1B1F))
            }
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun getEmotionColor(emotion: String): Color {
    return when (emotion.trim()) {
        "Happy" -> Color(0xFF4CAF50) // Green
        "Sad" -> Color(0xFF2196F3) // Blue
        "Angry" -> Color(0xFFF44336) // Red
        "Surprise" -> Color(0xFFFFEB3B) // Yellow
        "Fear" -> Color(0xFF9C27B0) // Purple
        "Disgust" -> Color(0xFFFF9800) // Orange
        "Neutral" -> Color(0xFF9E9E9E) // Gray
        else -> Color(0xFF00E5FF) // Cyan accent
    }
}

// Camera Capture handler
fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onCaptured: (Bitmap) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        "captured_face_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri ?: android.net.Uri.fromFile(photoFile)
                try {
                    val rawBitmap = BitmapFactory.decodeStream(
                        context.contentResolver.openInputStream(savedUri)
                    )

                    // Front camera images are often mirrored and rotated. Let's correct rotation.
                    val correctedBitmap = rotateBitmapIfRequired(rawBitmap, photoFile.absolutePath)
                    onCaptured(correctedBitmap)
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Failed to decode captured photo stream", e)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Image capture failure", exception)
            }
        }
    )
}

fun rotateBitmapIfRequired(img: Bitmap, selectedImagePath: String): Bitmap {
    val ei = android.media.ExifInterface(selectedImagePath)
    val orientation = ei.getAttributeInt(
        android.media.ExifInterface.TAG_ORIENTATION,
        android.media.ExifInterface.ORIENTATION_NORMAL
    )

    return when (orientation) {
        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
        else -> {
            // Front camera on many Android devices returns rotated by default depending on sensor orientation.
            // Let's ensure front camera rotates 270 or 90 to be vertical. Since we use front camera, standard is portrait rotate 270 / mirrored.
            // Let's check aspect ratio. If width > height, rotate 270 for front-camera portrait layout.
            if (img.width > img.height) {
                rotateImage(img, 270f)
            } else {
                img
            }
        }
    }
}

fun rotateImage(img: Bitmap, degree: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degree)
    // Mirror horizontal because it's front camera
    matrix.postScale(-1f, 1f, img.width / 2f, img.height / 2f)
    val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    img.recycle()
    return rotatedImg
}
