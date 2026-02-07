package com.example.depthclock

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.palette.graphics.Palette
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Settings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Принудительная горизонтальная ориентация
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // 2. Предотвращаем блокировку экрана
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // (Код скрытия панелей убираем отсюда, так как он нужен не только при старте)

        setContent {
            AdvancedDepthClockApp()
        }
    }

    // ЭТОТ МЕТОД РЕШАЕТ ПРОБЛЕМУ
    // Он вызывается каждый раз, когда вы возвращаетесь в приложение (из галереи, свернутого режима и т.д.)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        // Используем WindowCompat для совместимости со старыми Android (включая 8.0)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        // Настройка поведения: панели появляются при свайпе и исчезают сами
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Команда скрыть и статус бар (сверху), и навигацию (снизу)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AdvancedDepthClockApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    // --- СОСТОЯНИЕ ---
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var foregroundBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Состояние часов
    var clockColor by remember { mutableStateOf(Color.White) }
    var clockScale by remember { mutableFloatStateOf(1f) }
    var clockOffset by remember { mutableStateOf(Offset.Zero) }

    // Состояние прозрачности (Храним данные здесь)
    var isAlphaEnabled by remember { mutableStateOf(false) }
    var clockAlpha by remember { mutableFloatStateOf(0.5f) }

    // Режимы
    var isEditing by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var showWarning by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, it)
                        ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    }
                }
                originalBitmap = bitmap
                clockOffset = Offset.Zero
                clockScale = 1f
                foregroundBitmap = null
                showWarning = false
                isEditing = false

                isAlphaEnabled = false
                clockAlpha = 0.5f

                val palette = Palette.from(bitmap).generate()
                val dominantColor = palette.vibrantSwatch?.rgb ?: palette.dominantSwatch?.rgb
                clockColor = dominantColor?.let { Color(it) } ?: Color.White

                processImage(bitmap,
                    onSuccess = { fg -> foregroundBitmap = fg },
                    onError = { showWarning = true }
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. ФОН
        originalBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: Text("Нажмите кнопку снизу", modifier = Modifier.align(Alignment.Center))

        // 2. ЧАСЫ
        if (originalBitmap != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                MovableResizableClockContent(
                    color = clockColor,
                    scale = clockScale,
                    offset = clockOffset,
                    alpha = if (isAlphaEnabled) clockAlpha else 1f,
                    isEditing = isEditing,
                    onLongPress = {
                        isEditing = true
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                )
            }
        }

        // 3. ПЕРЕДНИЙ ПЛАН
        foregroundBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Foreground",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 4. СЛОЙ ЖЕСТОВ
        if (isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            clockScale *= zoom
                            clockScale = clockScale.coerceIn(0.5f, 7f)
                            clockOffset += pan
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { isEditing = false })
                    }
            ) {
                Text(
                    text = "Коснитесь экрана для сохранения",
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
                )
            }
        }

        // 5. КНОПКИ
        if (originalBitmap == null || isEditing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing && originalBitmap != null) {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Настройки",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Выбрать фото",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // 6. ДИАЛОГОВОЕ ОКНО НАСТРОЕК (Вынесено в отдельный файл)
        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false },
                isAlphaEnabled = isAlphaEnabled,
                onAlphaEnabledChange = { isAlphaEnabled = it },
                clockAlpha = clockAlpha,
                onClockAlphaChange = { clockAlpha = it }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MovableResizableClockContent(
    color: Color,
    scale: Float,
    offset: Offset,
    alpha: Float, // НОВЫЙ ПАРАМЕТР
    isEditing: Boolean,
    onLongPress: () -> Unit
) {
    var currentTime by remember { mutableStateOf("00:00") }

    LaunchedEffect(Unit) {
        while (true) {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            currentTime = formatter.format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                alpha = alpha // ПРИМЕНЯЕМ ПРОЗРАЧНОСТЬ
            )
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
    ) {
        Text(
            text = currentTime,
            fontSize = 100.sp,
            fontWeight = FontWeight.Bold,
            color = if (isEditing) color.copy(alpha = 0.8f) else color,
            style = LocalTextStyle.current.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    blurRadius = 20f
                )
            )
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MovableResizableClockContent(
    color: Color,
    scale: Float,
    offset: Offset,
    isEditing: Boolean,
    onLongPress: () -> Unit
) {
    var currentTime by remember { mutableStateOf("00:00") }

    LaunchedEffect(Unit) {
        while (true) {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            currentTime = formatter.format(Date())
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .graphicsLayer(scaleX = scale, scaleY = scale)
            // Логика долгого нажатия привязана к самому контейнеру часов
            .combinedClickable(
                onClick = {}, // Пустой клик, чтобы не пропускать нажатия сквозь часы
                onLongClick = onLongPress,
                // Отключаем ripple эффект, если не хотим визуального "всплеска"
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            )
        // Убрали .border и .padding рамки
    ) {
        Text(
            text = currentTime,
            fontSize = 100.sp,
            fontWeight = FontWeight.Bold,
            color = if (isEditing) color.copy(alpha = 0.8f) else color, // Чуть меняем прозрачность при редактировании
            style = LocalTextStyle.current.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    blurRadius = 20f
                )
            )
        )
    }
}

// Логика ML Kit (без изменений, но перенесли в конец для чистоты)
fun processImage(inputBitmap: Bitmap, onSuccess: (Bitmap) -> Unit, onError: () -> Unit) {
    val options = SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
    val segmenter = SubjectSegmentation.getClient(options)
    segmenter.process(InputImage.fromBitmap(inputBitmap, 0))
        .addOnSuccessListener { result ->
            result.foregroundBitmap?.let(onSuccess) ?: onError()
        }
        .addOnFailureListener { onError() }
}